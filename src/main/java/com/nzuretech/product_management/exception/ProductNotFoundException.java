package com.nzuretech.product_management.exception;

/**
 * Exception thrown when a requested product cannot be found.
 */
public class ProductNotFoundException extends RuntimeException {

    /**
     * Creates an exception with a descriptive message.
     *
     * @param message the reason the product was not found
     */
    public ProductNotFoundException(String message) {
        super(message);
    }
}
