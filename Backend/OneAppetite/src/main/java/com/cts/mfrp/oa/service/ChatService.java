package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.ChatRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Campus food guide — anonymous, stateless menu navigation + calorie estimates.
 *
 * Per campus policy:
 *   - Admins cannot access the chatbot at all (UI hides it; this service hard-blocks too).
 *   - No user-specific data is ever read. Tables like orders, order_items, notifications
 *     are forbidden; columns like wallet_balance, email, phone are forbidden.
 *   - Personal questions ("what did I eat yesterday?") trigger an exact rebuff line.
 *   - Every menu listing is restricted to items in stock right now.
 *   - The current wall-clock time biases suggestions toward the appropriate meal course.
 */
@Service
public class ChatService {

    private final JdbcTemplate jdbcTemplate;
    private final GeminiClient geminiClient;

    /** Exact wording mandated by the campus privacy policy. Do not edit. */
    private static final String PRIVACY_REBUFF =
            "I do not have access to personal accounts or history. " +
            "I can, however, help you with calorie estimates or today's campus menu.";

    /** Generic "I'm only a food guide" reply for off-topic / out-of-scope asks. */
    private static final String SCOPE_REBUFF =
            "I'm the campus food guide. I can suggest dishes that are in stock right now, " +
            "share calorie estimates, and tell you where to find them on campus.";

    /** Reply when an admin somehow reaches the endpoint (UI hides it, but defense in depth). */
    private static final String ADMIN_BLOCK_REPLY =
            "The campus food guide isn't available on the admin dashboard.";

    /** Friendly in-persona reply when Gemini errors out (rate limit, safety block, etc.). */
    private static final String SERVICE_HICCUP_REPLY =
            "I'm having a brief moment — please try again in a few seconds.";

    /** Block any write/DDL SQL coming out of the LLM. */
    private static final Pattern HARMFUL_SQL_PATTERN = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|replace)\\b"
    );

    /**
     * Tables the food guide must never touch — all of them carry user-specific
     * data. Hitting these triggers the privacy rebuff regardless of role.
     */
    private static final Pattern FORBIDDEN_TABLES_PATTERN = Pattern.compile(
            "(?i)\\b(orders|order_items|notifications)\\b"
    );

    /**
     * Sensitive user columns. None of these are needed for menu navigation,
     * so even a mention in the generated SQL is treated as a privacy breach.
     *
     * Note: {@code \bname\b} matches the standalone {@code name} column (e.g.
     * {@code u.name}, the user's real name) but does NOT match {@code vendor_name},
     * {@code item_name}, {@code building_name}, etc. — those are single tokens
     * because underscores are word characters in regex.
     */
    private static final Pattern FORBIDDEN_COLUMNS_PATTERN = Pattern.compile(
            "(?i)\\b(password|reset_otp|reset_otp_expiry|wallet_balance|email|phone|" +
            "admin_secret|notifications_enabled|is_active|name)\\b"
    );

    /**
     * {@code SELECT *} is always dangerous against the users table — even when
     * we redact known sensitive columns post-query, new columns added later
     * would leak silently. Force the LLM to select explicit, audited columns.
     */
    private static final Pattern SELECT_STAR_PATTERN = Pattern.compile(
            "(?i)select\\s+\\*"
    );

    /**
     * Any query that touches the {@code users} table must restrict to
     * {@code role = 'VENDOR'}. This stops jailbroken SQL like {@code SELECT *
     * FROM users WHERE role = 'EMPLOYEE'} from leaking other employees' rows.
     */
    private static final Pattern USERS_TABLE_PATTERN = Pattern.compile(
            "(?i)\\busers\\b"
    );
    private static final Pattern VENDOR_ROLE_FILTER_PATTERN = Pattern.compile(
            "(?i)role\\s*=\\s*'VENDOR'"
    );

    /**
     * First-line privacy filter — catch personal questions before they reach the LLM.
     * Saves a Gemini call AND guarantees the rebuff text is verbatim.
     */
    private static final Pattern PERSONAL_QUESTION_PATTERN = Pattern.compile(
            "(?i)\\b(" +
            // "my X" where X is a personal noun
            "my\\s+(orders?|cart|wallet|balance|history|purchases?|spending|account|" +
                  "dietary|profile|name|email|phone|id|preferences?|allergies|meals?|food)|" +
            // "what did I X" / "when did I X" / etc.
            "(what|when|where|how|why|did)\\s+(did\\s+)?i\\s+(eat|order|buy|spend|have|get|pick|consume|pay)|" +
            // "I have/had ordered/eaten/etc." — note: 'have' and 'had' are optional so we also
            // catch plain "I ate", "I ordered". 'i'?ve' separately catches the contraction "I've".
            "\\bi\\s+(have\\s+|had\\s+)?(ordered|bought|eaten|ate|spent|paid|consumed)\\b|" +
            "\\bi'?ve\\s+(ordered|bought|eaten|spent|had|consumed|paid)\\b|" +
            // "when/what was my last"
            "(when|what)\\s+was\\s+my\\s+last|" +
            // "who am I" / "am I allergic / vegetarian / vegan"
            "who\\s+am\\s+i|" +
            "am\\s+i\\s+(allergic|vegetarian|vegan)" +
            ")\\b"
    );

    /**
     * Stripped from every row before it's fed to the humanizer LLM — even if
     * the upstream filters miss something, these values never leave the server.
     */
    private static final Set<String> ALWAYS_REDACT_COLUMNS = Set.of(
            "password", "reset_otp", "reset_otp_expiry",
            "wallet_balance", "email", "phone",
            "user_id", "employee_id", "admin_secret",
            "notifications_enabled", "is_active"
    );

    private static final String SCHEMA_CONTEXT = """
Allowed schema for the OneAppetite General Campus Food Guide.
Everything else is OFF-LIMITS — never reference user-specific tables/columns.

1. MENU_ITEMS  (menu_items)
   Columns: item_id, item_name, category, meal_course, dietary_type,
            price, quantity_available, is_in_stock, vendor_id
   - dietary_type: 'Veg' or 'NonVeg' — use UPPER(dietary_type) for safe comparisons.
   - meal_course:  'Breakfast', 'Lunch', 'Dinner'.
   - Availability filter (MANDATORY on every listing query):
       WHERE mi.is_in_stock = TRUE AND mi.quantity_available > 0

2. USERS  (read ONLY the vendor row for display purposes)
   Allowed columns: user_id (join key only), vendor_name, stall_floor,
                    stall_wing, vendor_type, building_id
   Required filter: role = 'VENDOR'
   NEVER select wallet_balance, email, phone, name, password, is_active.

3. BUILDINGS  (buildings)              — building_id, campus_id, building_name
4. CAMPUSES   (campuses)               — campus_id, city_id, campus_name, address
5. CITIES     (cities)                 — city_id, city_name
6. VENDOR_EXTRA_BUILDINGS              — vendor↔building coverage (user_id, building_id)

FORBIDDEN — do not reference, ever:
  - Tables:  orders, order_items, notifications
  - Columns: password, reset_otp, reset_otp_expiry, wallet_balance, email,
             phone, is_active, notifications_enabled, admin_secret
  - WHERE clauses on user_id / employee_id targeting a specific person —
    there is no logged-in user from the food guide's perspective.
""";

    public ChatService(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate,
                       GeminiClient geminiClient) {
        this.jdbcTemplate  = jdbcTemplate;
        this.geminiClient  = geminiClient;
    }

    public String chat(ChatRequest req) {
        String userPrompt = req.message() == null ? "" : req.message().trim();
        // Trim AND uppercase so " ADMIN ", "Admin\n", "admin" all collapse to "ADMIN".
        // Without trim(), trailing whitespace would bypass the equals("ADMIN") check.
        String role       = req.role()    == null ? "" : req.role().trim().toUpperCase();

        System.out.println("\n========== [FoodGuide] Asked: " + userPrompt + " (role=" + role + ") ==========");

        // ── Hard block: admin dashboard has no chatbot ──
        if ("ADMIN".equals(role)) {
            System.out.println("[FoodGuide] → ADMIN role blocked at entrance");
            return ADMIN_BLOCK_REPLY;
        }

        // ── Empty / whitespace-only message: short-circuit, don't burn a Gemini call ──
        if (userPrompt.isEmpty()) {
            return "Ask me anything about today's campus menu or calorie estimates.";
        }

        // ── Privacy rebuff: detect personal questions BEFORE invoking the LLM ──
        if (PERSONAL_QUESTION_PATTERN.matcher(userPrompt).find()) {
            System.out.println("[FoodGuide] → Personal-question pattern matched; returning rebuff");
            return PRIVACY_REBUFF;
        }

        String sqlPrompt    = buildSqlPrompt(userPrompt);
        String generatedSql = geminiClient.generateText(sqlPrompt).trim();
        System.out.println("[FoodGuide] LLM raw:\n" + generatedSql);

        if (isServiceHiccup(generatedSql)) {
            System.err.println("[FoodGuide] → Service hiccup at SQL-gen step; returning friendly fallback");
            return SERVICE_HICCUP_REPLY;
        }

        if (generatedSql.contains("REJECT: personal")) return PRIVACY_REBUFF;
        if (generatedSql.contains("REJECT:"))          return SCOPE_REBUFF;

        generatedSql = generatedSql.replace("```sql", "").replace("```", "").trim();

        // The LLM occasionally wraps the SQL in a natural-language preamble like
        // "Since it's currently 14:08, here are some lunch options: SELECT ...".
        // If we find a SELECT anywhere in the reply, strip the preamble and treat
        // it as SQL. Otherwise it's a conversational reply (greeting, calorie est).
        int selectIdx = generatedSql.toUpperCase().indexOf("SELECT ");
        if (selectIdx < 0) {
            // No SELECT keyword — but the LLM might have emitted standalone DML/DDL
            // like "DELETE FROM menu_items". Catch that here before treating as
            // conversational text; otherwise the raw SQL statement would leak to the user.
            if (HARMFUL_SQL_PATTERN.matcher(generatedSql).find()) {
                System.out.println("[FoodGuide] → Blocked: non-SELECT response contains DML/DDL keywords");
                return "Security Alert: Query blocked. Only read operations are permitted.";
            }
            return appendCalorieDisclaimerIfNeeded(generatedSql);
        }
        if (selectIdx > 0) {
            System.out.println("[FoodGuide] Stripped " + selectIdx + " chars of preamble before SELECT");
            generatedSql = generatedSql.substring(selectIdx).trim();
        }

        if (HARMFUL_SQL_PATTERN.matcher(generatedSql).find()) {
            return "Security Alert: Query blocked. Only read operations are permitted.";
        }
        if (FORBIDDEN_TABLES_PATTERN.matcher(generatedSql).find()) {
            System.out.println("[FoodGuide] → Blocked: forbidden table reference");
            return PRIVACY_REBUFF;
        }
        if (FORBIDDEN_COLUMNS_PATTERN.matcher(generatedSql).find()) {
            System.out.println("[FoodGuide] → Blocked: forbidden column reference");
            return PRIVACY_REBUFF;
        }
        if (SELECT_STAR_PATTERN.matcher(generatedSql).find()) {
            System.out.println("[FoodGuide] → Blocked: SELECT * (must enumerate columns)");
            return PRIVACY_REBUFF;
        }
        if (USERS_TABLE_PATTERN.matcher(generatedSql).find()
                && !VENDOR_ROLE_FILTER_PATTERN.matcher(generatedSql).find()) {
            System.out.println("[FoodGuide] → Blocked: users table without role='VENDOR' filter");
            return PRIVACY_REBUFF;
        }

        try {
            List<Map<String, Object>> dbResults = jdbcTemplate.queryForList(generatedSql);
            System.out.println("[FoodGuide] DB returned " + dbResults.size() + " row(s)");

            if (dbResults.isEmpty()) {
                return "Nothing's matching that on today's live campus menu. " +
                       "Try a broader option like 'breakfast', 'snacks', or 'veg lunch'.";
            }

            List<Map<String, Object>> sanitized = sanitizeResults(dbResults);
            String humanPrompt = buildHumanizePrompt(userPrompt, sanitized);
            String reply = geminiClient.generateText(humanPrompt);
            if (isServiceHiccup(reply)) {
                System.err.println("[FoodGuide] → Service hiccup at humanize step; returning friendly fallback");
                return SERVICE_HICCUP_REPLY;
            }
            return reply;

        } catch (Exception e) {
            System.err.println("[FoodGuide] !!! DB execution error !!!");
            System.err.println("[FoodGuide] Failed SQL: " + generatedSql);
            System.err.println("[FoodGuide] MySQL:      " + e.getMessage());
            return "I hit an error pulling today's menu. Try asking in a different way.";
        }
    }

    /**
     * Detect the fallback strings GeminiClient returns when its HTTP call fails
     * (rate-limit, safety-policy block, missing API key, parse error). We don't
     * want any of those surfacing to end users — they expose infra terms like
     * "AI service" and look like a bug. Replace with a friendly in-persona reply.
     */
    private boolean isServiceHiccup(String reply) {
        if (reply == null || reply.isBlank()) return true;
        String lower = reply.toLowerCase();
        return lower.contains("trouble connecting to the ai service")
            || lower.contains("api key is missing")
            || lower.contains("error parsing response")
            || lower.contains("chatbot api key");
    }

    /**
     * Strip sensitive column values from every row before they reach the
     * humanizer LLM. Defense in depth: even if a filter is bypassed, the LLM
     * never sees the values and cannot leak them in the natural-language reply.
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
     * Maps the current hour to a meal course + time window so open-ended asks
     * ("what should I eat?") get suggestions aligned with what's actually being
     * served right now.
     */
    private String currentMealContext() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        String course;
        String window;
        if (hour < 10) {
            course = "Breakfast";
            window = "morning";
        } else if (hour < 15) {
            course = "Lunch";
            window = "afternoon";
        } else if (hour < 19) {
            course = "Snacks / Light Bites";
            window = "evening";
        } else {
            course = "Dinner";
            window = "evening";
        }
        return "Current campus time: " + now.format(DateTimeFormatter.ofPattern("HH:mm")) +
               " (" + window + "). Suggested meal course right now: " + course + ".";
    }

    /**
     * If the LLM's plain-text reply mentions a calorie number, append the
     * "these are estimates" disclaimer so we never share a number bare.
     */
    private String appendCalorieDisclaimerIfNeeded(String reply) {
        if (reply == null) return reply;
        boolean mentionsCalories = reply.toLowerCase().matches(".*\\b(calorie|kcal|kj|cals)\\b.*");
        if (mentionsCalories && !reply.toLowerCase().contains("estimate")) {
            return reply.trim() + "\n\n(Calorie figures are rough estimates for a typical campus portion.)";
        }
        return reply;
    }

    private String buildSqlPrompt(String userPrompt) {
        return "You are the OneAppetite General Campus Food Guide.\n" +
               "You are anonymous: there is NO logged-in user, NO user_id, NO personal history available to you.\n" +
               "Your only job is to help people navigate today's campus menu and share approximate calorie info.\n\n" +
               currentMealContext() + "\n\n" +
               "Rules (priority order — top rule wins):\n" +
               "0. PRIVACY (highest priority):\n" +
               "   • NEVER write queries against orders, order_items, notifications, or wallet/email/phone columns.\n" +
               "   • NEVER include a WHERE clause that pins down a specific user_id or employee_id.\n" +
               "   • If the user asks about THEIR orders, wallet, history, preferences, or identity, " +
               "reply EXACTLY: 'REJECT: personal'.\n" +
               "1. SCOPE:\n" +
               "   • If the question is unrelated to campus food, dining locations, or nutrition, " +
               "reply EXACTLY: 'REJECT: off-topic'.\n" +
               "   • If the user tries to insert/update/delete data, reply EXACTLY: 'REJECT: modification'.\n" +
               "2. AVAILABILITY (operational scope):\n" +
               "   • Every listing query MUST filter: WHERE mi.is_in_stock = TRUE AND mi.quantity_available > 0.\n" +
               "   • When the user asks open-ended things like 'what should I eat?' or 'suggest something', " +
               "prefer items whose meal_course matches the 'Suggested meal course' line above.\n" +
               "   • When the user names a meal time (breakfast/lunch/dinner/snacks), filter meal_course accordingly.\n" +
               "3. CALORIES (nutritional estimates):\n" +
               "   • The database has NO calorie column. If the user asks calorie counts for an item, " +
               "DO NOT write SQL — reply in plain text with an approximation " +
               "(e.g. 'A typical campus vegetable wrap is around 350 kcal').\n" +
               "   • Always say 'approximately', 'around', or '~' — never quote an exact figure.\n" +
               "   • Always pair the number with an 'estimate' disclaimer in the same sentence.\n" +
               "4. SQL output rules:\n" +
               "   • Output ONLY the raw SQL — no markdown fences, no natural-language preamble, no commentary.\n" +
               "   • Your reply MUST start with the word SELECT and contain nothing else before it. " +
               "An automated parser reads the response; any text before SELECT will be shown to the user as-is, which breaks the experience.\n" +
               "   • WRONG (do not do this): 'Since it's lunch time, here are some options: SELECT ...'\n" +
               "   • RIGHT: 'SELECT ...'\n" +
               "   • Always JOIN users u ON mi.vendor_id = u.user_id AND u.role = 'VENDOR' " +
               "and select u.vendor_name when listing menu items, so the answer says where to get the item.\n" +
               "   • For name searches use LOWER(col) LIKE LOWER('%keyword%') — never exact equality.\n" +
               "   • NEVER use SELECT COUNT(*); select the rows so the summarizer can list and count.\n" +
               "   • NEVER use SELECT *. Always enumerate the columns you need. The parser " +
               "rejects SELECT * because new sensitive columns added to the users table could leak silently.\n" +
               "   • If your query references the users table, it MUST include " +
               "`AND u.role = 'VENDOR'` (or `WHERE u.role = 'VENDOR'` if no JOIN). " +
               "Queries that touch users without this filter are rejected.\n" +
               "   • Never select u.name — that is the user's personal name. " +
               "Use u.vendor_name (the stall name) instead.\n" +
               "   • Append LIMIT 25 to every listing query.\n" +
               "5. IDENTITY / GREETING:\n" +
               "   • If the user greets you or asks who you are, answer in plain text (no SQL). " +
               "Say you're the campus food guide and you can suggest in-stock items, dining locations, " +
               "and calorie estimates — but you do not know who they are.\n\n" +
               SCHEMA_CONTEXT + "\n\n" +
               "User prompt: " + userPrompt;
    }

    private String buildHumanizePrompt(String userPrompt, List<Map<String, Object>> rows) {
        return "You are the OneAppetite General Campus Food Guide. The user asked: '" + userPrompt + "'.\n" +
               currentMealContext() + "\n\n" +
               "Database returned " + rows.size() + " row(s) of EXACT data:\n" + rows + "\n\n" +
               "Rules:\n" +
               "1. Only use values literally present in the rows above. Never invent item names, " +
               "prices, vendor names, or buildings.\n" +
               "2. Lead with a one-sentence summary that mentions the meal-time context " +
               "(e.g. 'Here are breakfast picks available right now:').\n" +
               "3. Then list up to 8 bullets. For each row, plug the ACTUAL values from that " +
               "row into these four slots — never print literal placeholder text like " +
               "'<price>' or '<vendor_name>'.\n" +
               "   Slot layout:  [item_name] — ₹[price] at [vendor_name] ([category])\n" +
               "   Concrete example using real values (this is what a bullet should look like):\n" +
               "     • Paneer Tikka Wrap — ₹120 at Quick Bites (Wraps) — ~380 kcal per standard portion\n" +
               "   Use the • bullet character (U+2022). Append an approximate calorie estimate " +
               "after the bullet when it fits naturally; skip the estimate rather than guess wildly. " +
               "If a numeric field is missing for some row, drop that field from that bullet — never " +
               "fill it with the literal text '<price>' or any other placeholder.\n" +
               "4. End your reply with this exact line on its own:\n" +
               "     (Calorie figures are estimates for a typical campus portion.)\n" +
               "5. NEVER reference the user's identity, orders, wallet, or history — you don't know who they are.\n" +
               "6. Plain text only — no markdown asterisks/underscores/hashes. Use the • bullet character (U+2022).\n" +
               "7. If some rows match the suggested meal course and others don't, lead with the matches.";
    }
}
