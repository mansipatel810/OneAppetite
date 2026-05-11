package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.ChatRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private final JdbcTemplate jdbcTemplate;
    private final GeminiClient geminiClient;

    private static final Pattern HARMFUL_SQL_PATTERN = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|replace)\\b"
    );

    private static final String SCHEMA_CONTEXT = """
Database Schema for OneAppetite (a campus food ordering platform)

1. USERS (table name: USERS)
   Columns: user_id (INT, PK), name (VARCHAR), email (VARCHAR, UNIQUE),
   role (ENUM: 'EMPLOYEE','VENDOR','ADMIN'), building_id (INT, FK->buildings),
   vendor_name (VARCHAR), vendor_description (VARCHAR), vendor_type (VARCHAR),
   stall_floor (VARCHAR), stall_wing (VARCHAR), is_active (BOOLEAN),
   wallet_balance (DOUBLE), notifications_enabled (BOOLEAN)

2. CITIES (table name: cities)
   Columns: city_id (INT, PK), city_name (VARCHAR)

3. CAMPUSES (table name: campuses)
   Columns: campus_id (INT, PK), city_id (INT, FK->cities), campus_name (VARCHAR), address (VARCHAR)

4. BUILDINGS (table name: buildings)
   Columns: building_id (INT, PK), campus_id (INT, FK->campuses), building_name (VARCHAR)

5. MENU ITEMS (table name: menu_items)
   Columns: item_id (INT, PK), item_name (VARCHAR), category (VARCHAR),
   meal_course (VARCHAR), dietary_type (VARCHAR), price (DOUBLE),
   quantity_available (INT), is_in_stock (BOOLEAN), vendor_id (INT, FK->USERS), min_prep_time (INT)

6. ORDERS (table name: orders)
   Columns: order_id (INT, PK), employee_id (INT, FK->USERS), vendor_id (INT, FK->USERS),
   token_number (VARCHAR), status (ENUM: 'CART','PLACED','PREPARING','READY','PICKED_UP','COMPLETED','PENDING'),
   total_amount (FLOAT), order_time (DATETIME), ready_time (DATETIME)

7. ORDER ITEMS (table name: order_items)
   Columns: order_item_id (INT, PK), order_id (INT, FK->orders),
   item_id (INT, FK->menu_items), quantity (INT), price (FLOAT)

8. NOTIFICATIONS (table name: notifications)
   Columns: id (INT, PK), user_id (INT, FK->USERS), message (VARCHAR),
   timestamp (DATETIME), is_read (BOOLEAN)

9. VENDOR EXTRA BUILDINGS (table name: vendor_extra_buildings)
   Columns: user_id (INT, FK->USERS), building_id (INT, FK->buildings)

Query Generation Rules:
- Always use proper JOINs based on the listed FKs.
- Respect EXACT ENUM values for WHERE clauses.
- USERS.role values are exactly: 'EMPLOYEE', 'VENDOR', 'ADMIN'.
- Order status values are exactly: 'CART','PLACED','PREPARING','READY','PICKED_UP','COMPLETED','PENDING'.
- employee_id and vendor_id in orders both reference USERS.user_id.
- vendor_id in menu_items references USERS.user_id.
""";

    public ChatService(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate, GeminiClient geminiClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.geminiClient = geminiClient;
    }

    public String chat(ChatRequest req) {
        String userPrompt = req.message();

        String sqlPrompt = buildSqlPrompt(userPrompt);
        String generatedSql = geminiClient.generateText(sqlPrompt).trim();

        if (generatedSql.contains("REJECT:")) {
            return "I am OneBot, an assistant for OneAppetite. I can only answer questions about menus, orders, vendors, and campus information.";
        }

        generatedSql = generatedSql.replace("```sql", "").replace("```", "").trim();

        if (!generatedSql.toUpperCase().startsWith("SELECT")) {
            return generatedSql;
        }

        if (HARMFUL_SQL_PATTERN.matcher(generatedSql).find()) {
            return "Security Alert: Query blocked. Only read operations are permitted.";
        }

        try {
            List<Map<String, Object>> dbResults = jdbcTemplate.queryForList(generatedSql);

            if (dbResults.isEmpty()) {
                return "I couldn't find any data matching your request.";
            }

            String humanPrompt = "User asked: '" + userPrompt + "'. Database returned: " + dbResults +
                    ". Summarize this data in a short, friendly, professional sentence. Do not mention SQL or databases.";

            return geminiClient.generateText(humanPrompt);

        } catch (Exception e) {
            System.err.println("Database Execution Error: " + e.getMessage());
            return "I encountered an error analyzing the data. Please try asking in a different way.";
        }
    }

    private String buildSqlPrompt(String userPrompt) {
        return "You are OneBot, an AI assistant restricted strictly to the OneAppetite food ordering platform database.\n\n" +
                "Rules:\n" +
                "1. If the user asks about ANY topic outside of food ordering, menus, vendors, orders, campuses, or buildings, reply EXACTLY with 'REJECT: off-topic'.\n" +
                "2. If the user tries to modify data (add, delete, update), reply EXACTLY with 'REJECT: modification'.\n" +
                "3. Otherwise, write a highly optimized MySQL SELECT query to answer the question.\n" +
                "4. Use NOW() for current datetime references.\n" +
                "5. Output ONLY the raw SQL query. NO markdown, NO explanations.\n" +
                "6. If the user greets you or asks who you are, respond with a friendly message explaining that you are OneBot, the AI assistant for OneAppetite — a campus food ordering platform.\n\n" +
                SCHEMA_CONTEXT + "\n\n" +
                "User Prompt: " + userPrompt;
    }
}
