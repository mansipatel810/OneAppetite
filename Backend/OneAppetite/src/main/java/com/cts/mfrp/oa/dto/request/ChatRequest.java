package com.cts.mfrp.oa.dto.request;

import java.util.List;
import java.util.Map;

/**
 * Chat request from the frontend.
 *
 * <p>Per the campus zero-data privacy policy, the chatbot is anonymous: we do
 * NOT carry a userId on this payload. The {@code role} field is retained only
 * so the server can hard-block ADMIN (no chatbot on the admin dashboard) as a
 * second line of defense behind the UI hiding it.
 *
 * @param message  The user's natural-language question.
 * @param history  Prior conversation turns (role + text). Capped client-side.
 * @param role     EMPLOYEE / VENDOR / ADMIN — used only to block ADMIN at the
 *                 endpoint. Never used to scope SQL to a specific person.
 */
public record ChatRequest(
        String message,
        List<Map<String, String>> history,
        String role
) {}
