package com.tapjacking.maltapanalyze.utils;

/**
 * Provides utility methods for parsing data types.
 */
public class ParseUtils {

    /**
     * Parses a string to a Long. If it is not a valid Long, it returns null.
     * @param value The string to parse.
     * @return The Long value or null if it is not a valid Long.
     */
    public static Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a string to an Integer. If it is not a valid Integer, it returns null.
     * @param value The string to parse.
     * @return The Integer value or null if it is not a valid Integer.
     */
    public static Integer parseInt(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a string to a Float. If it is not a valid Float, it returns null.
     * @param value The string to parse.
     * @return The Float value or null if it is not a valid Float.
     */
    public static Float parseFloat(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a string to a Boolean. If it is not a valid Boolean, it returns null.
     * @param value The string to parse.
     * @return The Boolean value or null if it is not a valid Boolean.
     */
    public static Boolean parseBoolean(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }
}
