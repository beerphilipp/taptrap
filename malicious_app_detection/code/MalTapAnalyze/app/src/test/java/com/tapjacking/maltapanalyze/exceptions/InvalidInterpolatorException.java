package com.tapjacking.maltapanalyze.exceptions;

/**
 * Exception thrown when an invalid interpolator is encountered in an animation.
 */
public class InvalidInterpolatorException extends RuntimeException{

    /**
     * Creates a new InvalidInterpolatorException with the specified message.
     * @param message the detail message for the exception
     */
    public InvalidInterpolatorException(String message) {
        super(message);
    }
}
