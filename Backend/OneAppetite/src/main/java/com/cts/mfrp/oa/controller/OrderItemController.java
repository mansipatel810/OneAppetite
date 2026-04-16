package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.request.CartRequest;
import com.cts.mfrp.oa.dto.response.CartResponseDTO;
import com.cts.mfrp.oa.dto.response.OrderItemDTO;
import com.cts.mfrp.oa.model.Order;
import com.cts.mfrp.oa.model.OrderItem;
import com.cts.mfrp.oa.service.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class OrderItemController {

    @Autowired
    private OrderItemService service;

    @PostMapping("/add")
    public ResponseEntity<OrderItemDTO> add(@RequestBody CartRequest request) {
        return ResponseEntity.ok(service.addProductToCart(request));
    }

    @PostMapping("/reduce/{orderItemId}")
    public ResponseEntity<Void> reduce(@PathVariable Integer orderItemId) {
        service.reduceQuantity(orderItemId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/total/{userId}")
    public ResponseEntity<Float> getTotal(@PathVariable Integer userId) {
        return ResponseEntity.ok(service.getCartTotal(userId));
    }

    @GetMapping("/view/{userId}")
    public ResponseEntity<CartResponseDTO> getCart(@PathVariable Integer userId) {
        return ResponseEntity.ok(service.getActiveCart(userId));
    }
}