package com.tapjacking.maltapextract.util;

public class MiscUtil {

    /**
     * Checks if the given value is a reference.
     * @param value The value to check, which can be a string that starts with '@' or '?'.
     * @return true if the value is a reference, false otherwise.
     */
    public static boolean isReference(String value) {
        if (value == null) {
            return false;
        }

        if (value.equals("@null")) {
            return false;
        }

        if (value.equals("@empty")) {
            return false;
        }

        if (value.startsWith("@")) {
            return true;
        }

        if (value.startsWith("?")) {
            return true;
        }

        return false;
    }
}
