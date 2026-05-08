package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String groqModel;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String SYSTEM_PROMPT =
            "You are OneBot, a friendly AI assistant for OneAppetite — a campus food ordering platform. " +
                    "Help users browse menus, place orders, track deliveries, manage their wallet, and navigate the app. " +
                    "Keep answers concise, friendly, and relevant to food ordering. " +
                    "If you don't know something specific about a user's order, ask them to check the My Orders page. " +
                    "Never make up order details or prices.";

    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(ChatRequest req) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            return "Chatbot is not configured. Please set the GROQ_API_KEY environment variable.";
        }

        // Groq uses a flat messages list
        List<Map<String, String>> messages = new ArrayList<>();

        // Add System Instruction
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        // Add conversation history
        if (req.history() != null) {
            for (Map<String, String> turn : req.history()) {
                messages.add(Map.of(
                        "role", turn.get("role"), // ensure these are 'user' or 'assistant'
                        "content", turn.get("text")
                ));
            }
        }

        // Add current user message
        messages.add(Map.of("role", "user", "content", req.message()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", groqModel);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 512);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Groq requires Bearer authentication
        headers.setBearerAuth(groqApiKey);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GROQ_URL,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            // Parsing Groq's OpenAI-style response
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            return (String) message.get("content");
        } catch (HttpClientErrorException e) {
            log.error("Groq API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 429) {
                return "I'm a little overwhelmed right now! Please wait a moment and try again.";
            }
            return "Groq API error: " + e.getStatusCode() + ". Check your API key and model selection.";
        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage(), e);
            return "Sorry, I'm having trouble right now. Error: " + e.getMessage();
        }
    }
}