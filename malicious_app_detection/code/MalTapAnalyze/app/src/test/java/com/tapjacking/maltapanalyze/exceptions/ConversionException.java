package com.tapjacking.maltapanalyze.exceptions;

/**
 * Exception thrown when there is an error during the conversion of a Typed Value.
 */
public class ConversionException extends RuntimeException {

    /**
     * Creates a new ConversionException with the specified message.
     * @param message the detail message for the exception
     */
    public ConversionException(String message) {
        super(message);
    }
}
