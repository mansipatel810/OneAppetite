package com.cts.mfrp.oa.dto.request;

import java.util.List;
import java.util.Map;

/**
 * Chat request from the frontend.
 *
 * @param message  The user's natural-language question.
 * @param history  Prior conversation turns (role + text). Capped client-side.
 * @param role     Optional. Logged-in user's role: EMPLOYEE, VENDOR, ADMIN, or null
 *                 for anonymous pre-login users. Used by ChatService to scope SQL
 *                 queries to the right data subset (e.g. a vendor only sees their
 *                 own orders, not other vendors').
 * @param userId   Optional. Logged-in user's user_id. Used to plug into role-scoped
 *                 WHERE clauses (e.g. WHERE vendor_id = {userId}).
 */
public record ChatRequest(
        String message,
        List<Map<String, String>> history,
        String role,
        Integer userId
) {}
