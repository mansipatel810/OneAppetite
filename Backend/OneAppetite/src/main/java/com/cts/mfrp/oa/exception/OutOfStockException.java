package com.cts.mfrp.oa.exception;

/**
 * Thrown by the order-placement flow when an item the customer is trying to
 * buy has run out of stock between the cart-add and the pay click. Maps to
 * HTTP 409 Conflict.
 */
public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) {
        super(message);
    }
}
