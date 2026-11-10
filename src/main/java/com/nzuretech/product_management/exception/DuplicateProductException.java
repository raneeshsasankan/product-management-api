package com.nzuretech.product_management.exception;

/**
 * Exception thrown when attempting to create or update
 * a product with a name that already exists.
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public class DuplicateProductException extends RuntimeException {

    /**
     * Creates an exception with a descriptive message.
     *
     * @param message the reason the product name is considered a duplicate
     */
    public DuplicateProductException(String message) {
        super(message);
    }
}
