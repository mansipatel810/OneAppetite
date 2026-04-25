package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.request.CartRequest;
import com.cts.mfrp.oa.dto.response.CartResponseDTO;
import com.cts.mfrp.oa.dto.response.OrderItemDTO;
import java.util.List;
import com.cts.mfrp.oa.service.AuthGuardService;
import com.cts.mfrp.oa.service.OrderItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class OrderItemController {

    @Autowired
    private OrderItemService service;

    @Autowired
    private AuthGuardService authGuard;

    @PostMapping("/add")
    public ResponseEntity<OrderItemDTO> add(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @Valid @RequestBody CartRequest request) {
        authGuard.verifySelfOrAdmin(callerId, request.userId());
        return ResponseEntity.ok(service.addProductToCart(request));
    }

    @PostMapping("/reduce/{orderItemId}")
    public ResponseEntity<Void> reduce(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer orderItemId) {
        authGuard.verifyCaller(callerId);
        service.reduceQuantity(orderItemId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/total/{userId}")
    public ResponseEntity<Float> getTotal(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer userId) {
        authGuard.verifySelfOrAdmin(callerId, userId);
        return ResponseEntity.ok(service.getCartTotal(userId));
    }

    @GetMapping("/view/{userId}")
    public ResponseEntity<CartResponseDTO> getCart(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer userId) {
        authGuard.verifySelfOrAdmin(callerId, userId);
        return ResponseEntity.ok(service.getActiveCart(userId));
    }

    @PostMapping("/place/{userId}")
    public ResponseEntity<CartResponseDTO> placeOrder(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer userId) {
        authGuard.verifySelfOrAdmin(callerId, userId);
        return ResponseEntity.ok(service.placeOrder(userId));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<CartResponseDTO>> getOrderHistory(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer userId) {
        authGuard.verifySelfOrAdmin(callerId, userId);
        return ResponseEntity.ok(service.getOrderHistory(userId));
    }
}
