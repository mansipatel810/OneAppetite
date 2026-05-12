package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.ChatRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private final JdbcTemplate jdbcTemplate;
    private final GeminiClient geminiClient;

    private static final Pattern HARMFUL_SQL_PATTERN = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|replace)\\b"
    );

    /**
     * Sensitive columns the chatbot must NEVER expose regardless of role.
     * Even if the LLM is jailbroken or misinterprets the prompt, any generated
     * SQL referencing these will be blocked before execution.
     */
    private static final Pattern FORBIDDEN_COLUMNS_PATTERN = Pattern.compile(
            "(?i)\\b(password|reset_otp|reset_otp_expiry)\\b"
    );

    /**
     * Cross-role queries that EMPLOYEE users are never allowed to run.
     * Vendors / admins are exempt — they have legitimate reasons to see
     * other people's wallet_balance, role, is_active, etc.
     *
     * Note: we only block AGGREGATIONS on wallet_balance (SUM/AVG/MAX/MIN/COUNT)
     * or grouping by other users. A plain SELECT wallet_balance ... WHERE user_id = self
     * is fine — that's just the user checking their own balance.
     */
    private static final Pattern EMPLOYEE_FORBIDDEN_PATTERN = Pattern.compile(
            "(?i)(" +
            "\\b(SUM|AVG|MAX|MIN|COUNT)\\s*\\(\\s*(DISTINCT\\s+)?wallet_balance" +    // wallet aggregations
            "|\\bGROUP\\s+BY\\s+(vendor_id|employee_id|role)" +                       // cross-user grouping
            "|\\badmin_secret\\b" +                                                    // admin-only field
            "|\\bSUM\\s*\\(\\s*(DISTINCT\\s+)?(total_amount|price|quantity)\\b" +     // sales aggregations
            ")"
    );

    /**
     * Columns we always strip from the data we feed into the humanizer prompt,
     * as a belt-and-braces backstop: even if forbidden SQL slips past the
     * pre-execution filter, the LLM never sees these field values.
     */
    private static final Set<String> ALWAYS_REDACT_COLUMNS = Set.of(
            "password", "reset_otp", "reset_otp_expiry"
    );

    private static final String SCHEMA_CONTEXT = """
Database Schema for OneAppetite (a campus food ordering platform)

1. USERS (table name: users)
   Columns: user_id (INT, PK), name (VARCHAR), email (VARCHAR, UNIQUE),
   role (ENUM: 'EMPLOYEE','VENDOR','ADMIN'), building_id (INT, FK->buildings),
   vendor_name (VARCHAR), vendor_description (VARCHAR), vendor_type (VARCHAR),
   stall_floor (VARCHAR), stall_wing (VARCHAR), is_active (BOOLEAN),
   wallet_balance (DOUBLE), notifications_enabled (BOOLEAN)

   IMPORTANT — exact value reference for filter columns:
   - role: exactly 'EMPLOYEE', 'VENDOR', or 'ADMIN' (uppercase).
   - vendor_type: either 'Veg' or 'NonVeg' (same case rule as menu_items.dietary_type — use UPPER() for safety).
   - is_active: TRUE = active user, FALSE = deactivated. When counting "vendors" without other qualifiers, filter by is_active = TRUE.

2. CITIES (table name: cities)
   Columns: city_id (INT, PK), city_name (VARCHAR)

3. CAMPUSES (table name: campuses)
   Columns: campus_id (INT, PK), city_id (INT, FK->cities), campus_name (VARCHAR), address (VARCHAR)

4. BUILDINGS (table name: buildings)
   Columns: building_id (INT, PK), campus_id (INT, FK->campuses), building_name (VARCHAR)

5. MENU ITEMS (table name: menu_items)
   Columns: item_id (INT, PK), item_name (VARCHAR), category (VARCHAR),
   meal_course (VARCHAR), dietary_type (VARCHAR), price (DOUBLE),
   quantity_available (INT), is_in_stock (BOOLEAN), vendor_id (INT, FK->users), min_prep_time (INT)

   IMPORTANT — exact value reference for filter columns:
   - dietary_type: stored as either 'Veg' or 'NonVeg' (case may vary; ALWAYS compare with UPPER(dietary_type) IN ('VEG','VEGETARIAN') for veg, or UPPER(dietary_type) IN ('NONVEG','NON_VEG','NON-VEG') for non-veg).
   - meal_course: one of 'Breakfast', 'Lunch', 'Dinner'.
   - category: free-form, examples: 'Parathas','Sandwiches','Snacks','Combo','Rice','Indo-Chinese','Starters','Grills','Egg Dishes','Curry','Biryani','Wraps','Soups','Side dish','Main course','Pizza','Pasta','Sides','Wings','Beverages','Bakery','Light Bites','Chaat','Street Food','Cakes','Indian Sweets','Waffles','Salads','Bowls','Platters','South Indian'.
   - is_in_stock: TRUE means available, FALSE means out of stock.
   - For "available" or "in stock" filters: WHERE is_in_stock = TRUE AND quantity_available > 0.

6. ORDERS (table name: orders)
   Columns: order_id (INT, PK), employee_id (INT, FK->users), vendor_id (INT, FK->users),
   token_number (VARCHAR), status (ENUM: 'CART','PLACED','PREPARING','READY','PICKED_UP','COMPLETED','PENDING'),
   total_amount (FLOAT), order_time (DATETIME), ready_time (DATETIME)

7. ORDER ITEMS (table name: order_items)
   Columns: order_item_id (INT, PK), order_id (INT, FK->orders),
   item_id (INT, FK->menu_items), quantity (INT), price (FLOAT)

8. NOTIFICATIONS (table name: notifications)
   Columns: id (INT, PK), user_id (INT, FK->users), message (VARCHAR),
   timestamp (DATETIME), is_read (BOOLEAN)

9. VENDOR EXTRA BUILDINGS (table name: vendor_extra_buildings)
   Columns: user_id (INT, FK->users), building_id (INT, FK->buildings)

Query Generation Rules:
- All table names are lowercase. Use exactly: users, cities, campuses, buildings, menu_items, orders, order_items, notifications, vendor_extra_buildings.
- Always use proper JOINs based on the listed FKs.
- Respect EXACT ENUM values for WHERE clauses.
- users.role values are exactly: 'EMPLOYEE', 'VENDOR', 'ADMIN'.
- Order status values are exactly: 'CART','PLACED','PREPARING','READY','PICKED_UP','COMPLETED','PENDING'.
- employee_id and vendor_id in orders both reference users.user_id.
- vendor_id in menu_items references users.user_id.
""";

    public ChatService(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate, GeminiClient geminiClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.geminiClient = geminiClient;
    }

    public String chat(ChatRequest req) {
        String userPrompt = req.message();
        String role       = req.role()   == null ? "EMPLOYEE" : req.role();
        Integer userId    = req.userId() == null ? 0          : req.userId();
        System.out.println("\n========== [OneBot] User asked: " + userPrompt + " (role=" + role + ", userId=" + userId + ") ==========");

        String sqlPrompt = buildSqlPrompt(userPrompt, role, userId);
        String generatedSql = geminiClient.generateText(sqlPrompt).trim();
        System.out.println("[OneBot] Gemini raw output:\n" + generatedSql);

        if (generatedSql.contains("REJECT:")) {
            System.out.println("[OneBot] → REJECTED (off-topic or modification)");
            return "I am OneBot, an assistant for OneAppetite. I can only answer questions about menus, orders, vendors, and campus information.";
        }

        generatedSql = generatedSql.replace("```sql", "").replace("```", "").trim();
        System.out.println("[OneBot] Cleaned SQL:\n" + generatedSql);

        if (!generatedSql.toUpperCase().startsWith("SELECT")) {
            System.out.println("[OneBot] → Not a SELECT, returning as plain text reply");
            return generatedSql;
        }

        if (HARMFUL_SQL_PATTERN.matcher(generatedSql).find()) {
            System.out.println("[OneBot] → Blocked by harmful-SQL regex");
            return "Security Alert: Query blocked. Only read operations are permitted.";
        }

        // ── HARD ENFORCEMENT 1: forbidden columns (universal) ──
        // No role should ever be able to read these — block at SQL level.
        if (FORBIDDEN_COLUMNS_PATTERN.matcher(generatedSql).find()) {
            System.out.println("[OneBot] → Blocked: SQL references forbidden columns (password/OTP)");
            return "I'm not able to share sensitive account information like passwords or security codes.";
        }

        // ── HARD ENFORCEMENT 2: cross-role guardrails for EMPLOYEE ──
        // Employees can't query wallet balances of others, admin secrets,
        // or aggregate sales/orders grouped by vendor/employee.
        if ("EMPLOYEE".equals(role) && EMPLOYEE_FORBIDDEN_PATTERN.matcher(generatedSql).find()) {
            System.out.println("[OneBot] → Blocked: employee attempted vendor/admin-only query");
            return "Sorry, I can only help you with your own orders, menu browsing, and food choices. " +
                   "Sales analytics and other users' details aren't available to employees.";
        }

        try {
            List<Map<String, Object>> dbResults = jdbcTemplate.queryForList(generatedSql);
            System.out.println("[OneBot] DB returned " + dbResults.size() + " row(s)");

            if (dbResults.isEmpty()) {
                System.out.println("[OneBot] → No rows; sending canned 'no data' response");
                return "I couldn't find any data matching your request.";
            }

            // ── BELT-AND-BRACES: strip sensitive columns from results even
            //    if the SQL filter somehow let them through. Defense in depth.
            List<Map<String, Object>> sanitized = sanitizeResults(dbResults);

            String humanPrompt =
                "User asked: '" + userPrompt + "'.\n" +
                "Database returned " + sanitized.size() + " row(s) of EXACT data:\n" + sanitized + "\n\n" +
                "ABSOLUTE ANTI-HALLUCINATION RULES (most important):\n" +
                "1. You MUST only use values literally present in the rows above. " +
                "Never invent item names, prices, categories, vendor names, or any other field. " +
                "If a row has 'order_id' and 'total_amount' but no 'item_name', " +
                "DO NOT invent item names like 'Main Course' or 'Snack' — just describe what's actually there.\n" +
                "2. If the data is an aggregate (a single row with COUNT, SUM, AVG, or just one numeric value), " +
                "respond with ONE short sentence stating that value. No bullet list. Example: 'You have 6 orders today.' " +
                "or 'Your wallet balance is ₹935.'\n" +
                "3. If the data is a list of multiple rows with descriptive fields (item_name, vendor_name, etc.), " +
                "THEN use the bullet format below.\n" +
                "4. If you don't have enough columns to format a bullet list, omit the list entirely. " +
                "Just give the one-sentence answer.\n\n" +
                "Format when listing multiple rows:\n" +
                "  Line 1: one-sentence summary that mentions the total count.\n" +
                "  Line 2: blank line.\n" +
                "  Lines 3+: up to 10 bullets, each in the format:\n" +
                "    • <exact value from a name column> — ₹<exact price if a price column exists> (<exact category or vendor from a column>)\n" +
                "  If more than 10 rows: end with '…and N more.' using the real number.\n\n" +
                "Style rules:\n" +
                "- Use PLAIN TEXT only. No markdown asterisks, underscores, or hashes.\n" +
                "- Use the • bullet character (U+2022).\n" +
                "- Use real newline characters between lines.\n" +
                "- Never mention SQL, databases, queries, or tables.\n" +
                "- Friendly, concise, professional tone — like a polite cafeteria attendant.\n" +
                "- If unsure about a value, leave it out rather than guess.";

            return geminiClient.generateText(humanPrompt);

        } catch (Exception e) {
            System.err.println("[OneBot] !!! Database Execution Error !!!");
            System.err.println("[OneBot] Failed SQL was:\n" + generatedSql);
            System.err.println("[OneBot] MySQL error: " + e.getMessage());
            e.printStackTrace();
            return "I encountered an error analyzing the data. Please try asking in a different way.";
        }
    }

    /**
     * Strip sensitive column values from every row before they reach the
     * humanizer LLM. Even if a SQL filter is bypassed, the actual data values
     * never leave this server. The LLM only ever sees redacted rows, so it
     * cannot leak them in the natural-language response.
     */
    private List<Map<String, Object>> sanitizeResults(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> {
                    Map<String, Object> clean = new LinkedHashMap<>(row);
                    ALWAYS_REDACT_COLUMNS.forEach(clean::remove);
                    return clean;
                })
                .toList();
    }

    /**
     * Role-specific context that scopes what the chatbot is allowed to answer
     * and how it should interpret first-person words like "my", "I", "our".
     */
    private String roleContext(String role, Integer userId) {
        switch (role) {
            case "VENDOR":
                return "CURRENT USER CONTEXT:\n" +
                       "- The person asking is a VENDOR with user_id = " + userId + ".\n" +
                       "- They are ALWAYS allowed to query THEIR OWN data — wallet_balance, vendor_name, " +
                       "orders, menu items, sales, etc. — as long as the WHERE clause scopes to user_id = " + userId + " (or vendor_id = " + userId + ").\n" +
                       "- When they say 'my orders', 'my items', 'my menu', 'best selling', 'top items', etc., scope strictly:\n" +
                       "    • For menu_items queries: WHERE vendor_id = " + userId + "\n" +
                       "    • For orders queries: WHERE vendor_id = " + userId + "\n" +
                       "    • For order_items: JOIN orders o ON oi.order_id = o.order_id WHERE o.vendor_id = " + userId + "\n" +
                       "- WALLET / EARNINGS / SALES / REVENUE (CRITICAL — must match the UI):\n" +
                       "    • If the user's question is one of these EXACT phrasings or close variants — 'wallet', 'wallet balance', " +
                       "'my earnings', 'total earnings', 'my sales', 'my total sales', 'my revenue', 'how much have I earned', " +
                       "'how much have I made' — the SQL MUST BE EXACTLY:\n" +
                       "        SELECT wallet_balance FROM users WHERE user_id = " + userId + "\n" +
                       "      NEVER write SUM(orders.total_amount) for these. NEVER. The wallet_balance column IS the answer.\n" +
                       "    • The wallet_balance column reflects the live credited earnings (same value shown on the vendor's Settings page as 'Total Earnings'). " +
                       "Using SUM(total_amount) would drift from this and confuse the vendor.\n" +
                       "    • ONLY use SUM(orders.total_amount) when the user explicitly attaches a time window like " +
                       "'sales TODAY', 'sales THIS WEEK', 'sales LAST MONTH', 'orders YESTERDAY'. " +
                       "Plain 'my sales' WITHOUT a time qualifier = wallet_balance, not SUM.\n" +
                       "- For 'best selling' or 'top items', use SUM(oi.quantity) per item_id, ORDER BY DESC, scoped to this vendor.\n" +
                       "- For 'today', use WHERE DATE(order_time) = CURDATE().\n" +
                       "- Do NOT expose data from other vendors. Refuse cross-vendor comparisons with REJECT: cross-vendor.";

            case "ADMIN":
                return "CURRENT USER CONTEXT:\n" +
                       "- The person asking is an ADMIN (user_id = " + userId + ").\n" +
                       "- Admins can ask about ANY user, vendor, employee, order, menu item, or platform-wide statistic.\n" +
                       "- When they say 'employees', filter users WHERE role = 'EMPLOYEE'.\n" +
                       "- When they say 'vendors', filter users WHERE role = 'VENDOR'.\n" +
                       "- 'active' / 'inactive' maps to is_active = TRUE / FALSE.\n" +
                       "- 'top vendor by revenue' = SUM(orders.total_amount) GROUP BY vendor_id ORDER BY total DESC.\n" +
                       "- 'top customer by spend' = SUM(orders.total_amount) GROUP BY employee_id ORDER BY total DESC.\n" +
                       "- They can see cross-vendor and cross-employee data. No scoping restrictions.";

            case "EMPLOYEE":
            default:
                return "CURRENT USER CONTEXT:\n" +
                       "- The person asking is an EMPLOYEE / customer (user_id = " + userId + ").\n" +
                       "- They are ALWAYS allowed to query THEIR OWN data — wallet_balance, name, email, " +
                       "their own orders, their own cart — as long as the WHERE clause scopes to user_id = " + userId + " (or employee_id = " + userId + ").\n" +
                       "- When they say 'my orders' or 'my cart', scope to WHERE employee_id = " + userId + ".\n" +
                       "- For 'my wallet', 'wallet balance', 'how much money do I have': " +
                       "SELECT wallet_balance FROM users WHERE user_id = " + userId + ".\n" +
                       "- For 'available items', filter menu_items.is_in_stock = TRUE AND quantity_available > 0.\n" +
                       "- They CANNOT see other employees' wallets/orders, vendor sales aggregations, or admin data. " +
                       "If asked, REJECT: cross-role.";
        }
    }

    private String buildSqlPrompt(String userPrompt, String role, Integer userId) {
        return "You are OneBot, an AI assistant restricted strictly to the OneAppetite food ordering platform database.\n\n" +
                roleContext(role, userId) + "\n\n" +
                "Rules:\n" +
                "0. SECURITY RULES (highest priority — these override every other rule):\n" +
                "   • NEVER select 'password', 'reset_otp', or 'reset_otp_expiry'. If asked, reply EXACTLY 'REJECT: sensitive'.\n" +
                "   • If the role context above says the user is EMPLOYEE, NEVER write queries that read other employees' wallet_balance, other vendors' sales/orders, or admin-only data. Reply EXACTLY 'REJECT: cross-role'.\n" +
                "   • If the role context above says the user is VENDOR, NEVER write queries that touch other vendors' data (other vendor_id values). Reply EXACTLY 'REJECT: cross-vendor'.\n" +
                "   • Honor the role scoping in the CURRENT USER CONTEXT block — those WHERE clauses are mandatory, not suggestions.\n" +
                "1. If the user asks about ANY topic outside of food ordering, menus, vendors, orders, campuses, or buildings, reply EXACTLY with 'REJECT: off-topic'.\n" +
                "2. If the user tries to modify data (add, delete, update), reply EXACTLY with 'REJECT: modification'.\n" +
                "3. Otherwise, write a highly optimized MySQL SELECT query to answer the question.\n" +
                "4. Use NOW() for current datetime references.\n" +
                "5. Output ONLY the raw SQL query. NO markdown, NO explanations.\n" +
                "6. If the user greets you or asks who you are, respond with a friendly message explaining that you are OneBot, the AI assistant for OneAppetite — a campus food ordering platform.\n" +
                "7. NEVER write SELECT COUNT(*). Instead, select the actual rows with relevant descriptive columns " +
                "(e.g. for menu items: item_name, price, category, dietary_type; for vendors: vendor_name, building_id, vendor_type). " +
                "The summarizer will count them and list examples. ALWAYS append LIMIT 50 to listing queries for safety.\n" +
                "8. When the question implies showing items, ALWAYS JOIN to users to get vendor_name when listing menu items, " +
                "and to buildings to get building_name when listing vendors. This makes the answer more informative.\n" +
                "9. For ANY search by name (item_name, vendor_name, category, building_name, etc.), NEVER use exact equality. " +
                "Always use case-insensitive partial matching: LOWER(column) LIKE LOWER('%keyword%'). " +
                "Example: user asks 'price of aloo paratha' → WHERE LOWER(mi.item_name) LIKE LOWER('%aloo paratha%'). " +
                "Example: user asks 'items at quick bites' → JOIN users u WHERE LOWER(u.vendor_name) LIKE LOWER('%quick bites%'). " +
                "This handles real item names like 'Aloo Paratha + Curd' matching shorter user queries.\n" +
                "10. If the user uses casual short words like 'maggi', 'biryani', 'pizza', 'dosa' — treat them as keywords " +
                "and search both item_name AND category with LIKE. Be generous with matching.\n\n" +
                SCHEMA_CONTEXT + "\n\n" +
                "User Prompt: " + userPrompt;
    }
}
