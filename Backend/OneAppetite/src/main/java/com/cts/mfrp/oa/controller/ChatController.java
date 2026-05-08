package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.request.ChatRequest;
import com.cts.mfrp.oa.dto.response.ChatResponse;
import com.cts.mfrp.oa.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest req) {
        return ResponseEntity.ok(new ChatResponse(chatService.chat(req)));
    }
}
