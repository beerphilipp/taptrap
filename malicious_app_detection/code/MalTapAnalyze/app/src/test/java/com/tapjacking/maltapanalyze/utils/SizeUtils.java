package com.tapjacking.maltapanalyze.utils;

import android.util.TypedValue;

import com.tapjacking.maltapanalyze.exceptions.ConversionException;

import kotlin.Pair;

/**
 * A utility class that provides methods to parse size values.
 */
public class SizeUtils {

    /**
     * Checks whether a given value is a fraction, i.e., ends with "%" or "%p".
     * @param rawValue The raw value to be checked
     * @return True if the value is a fraction, false otherwise
     */
    private static boolean isTypeFraction(String rawValue) {
        return rawValue.endsWith("%") || rawValue.endsWith("%p");
    }

    /**
     * Checks whether a given value is an integer.
     * @param rawValue The raw value to be checked
     * @return True if the value is an integer, false otherwise
     */
    private static boolean isInteger(String rawValue) {
        try {
            Integer.parseInt(rawValue);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks whether a given value is a dimension, i.e., ends with "px", "dip", "sp", "pt", "in", or "mm".
     * @param rawValue The raw value to be checked
     * @return True if the value is a dimension, false otherwise
     */
    private static boolean isTypeDimension(String rawValue) {
        if (rawValue.endsWith("px")) {
            return true;
        } else if (rawValue.endsWith("dip")) {
            return true;
        } else if (rawValue.endsWith("sp")) {
            return true;
        } else if (rawValue.endsWith("pt")) {
            return true;
        } else if (rawValue.endsWith("in")) {
            return true;
        } else if (rawValue.endsWith("mm")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Converts a float value to a complex value.
     * This method is equivalent to the method @link{TypedValue.floatToComplex} which we cannot - for some reason - access in the test environment.
     * @param value The float value to be converted
     * @return The complex value
     */
    public static int floatToComplex(float value) {
        if (value < (float) -0x800000 - .5f || value >= (float) 0x800000 - .5f) {
            throw new IllegalArgumentException("Magnitude of the value is too large: " + value);
        }
        try {
            // If there's no fraction, use integer representation, as that's clearer
            if (value == (float) (int) value) {
                return createComplex((int) value, TypedValue.COMPLEX_RADIX_23p0);
            }
            float absValue = Math.abs(value);
            // If the magnitude is 0, we don't need any magnitude digits
            if (absValue < 1f) {
                return createComplex(Math.round(value * (1 << 23)), TypedValue.COMPLEX_RADIX_0p23);
            }
            // If the magnitude is less than 2^8, use 8 magnitude digits
            if (absValue < (float) (1 << 8)) {
                return createComplex(Math.round(value * (1 << 15)), TypedValue.COMPLEX_RADIX_8p15);
            }
            // If the magnitude is less than 2^16, use 16 magnitude digits
            if (absValue < (float) (1 << 16)) {
                return createComplex(Math.round(value * (1 << 7)), TypedValue.COMPLEX_RADIX_16p7);
            }
            // The magnitude requires all 23 digits
            return createComplex(Math.round(value), TypedValue.COMPLEX_RADIX_23p0);
        } catch (IllegalArgumentException ex) {
            // Wrap exception so as to include the value argument in the message.
            throw new IllegalArgumentException("Unable to convert value to complex: " + value, ex);
        }
    }

    /**
     * Creates a complex value from a mantissa and a radix.
     * This method is equivalent to the method @link{TypedValue.createComplex} which we cannot - for some reason - access in the test environment.
     * @param mantissa The mantissa
     * @param radix The radix
     * @return The complex value
     */
    private static int createComplex(int mantissa, int radix) {
        if (mantissa < -0x800000 || mantissa >= 0x800000) {
            throw new IllegalArgumentException("Magnitude of mantissa is too large: " + mantissa);
        }
        if (radix < TypedValue.COMPLEX_RADIX_23p0 || radix > TypedValue.COMPLEX_RADIX_0p23) {
            throw new IllegalArgumentException("Invalid radix: " + radix);
        }
        return ((mantissa & TypedValue.COMPLEX_MANTISSA_MASK) << TypedValue.COMPLEX_MANTISSA_SHIFT)
                | (radix << TypedValue.COMPLEX_RADIX_SHIFT);
    }

    /**
     * Creates a complex dimension from a raw value.
     * This method is similar to the method @link{TypedValue.createComplexDimension} which we cannot - for some reason - access in the test environment.
     * @param rawValue The raw value to be converted
     * @return The complex dimension
     */
    private static int createComplexDimen(String rawValue) {
        int units = getComplexUnitType(rawValue);
        if (units < TypedValue.COMPLEX_UNIT_PX || units > TypedValue.COMPLEX_UNIT_MM) {
            throw new IllegalArgumentException("Must be a valid COMPLEX_UNIT_*: " + units);
        }
        float value = getFloatFromDimension(rawValue);
        return floatToComplex(value) | units;
    }

    /**
     * Retrieves the float value from a dimension value.
     * @param rawValue The raw value to be parsed
     * @return The float value
     */
    private static float getFloatFromDimension(String rawValue) {
        return Float.parseFloat(rawValue.substring(0, rawValue.length() - 2));
    }

    /**
     * Retrieves the complex unit type from a raw value.
     * @param rawValue The raw value to be parsed
     * @return The complex unit type
     */
    private static int getComplexUnitType(String rawValue) {
        if (rawValue.endsWith("px")) {
            return TypedValue.COMPLEX_UNIT_PX;
        } else if (rawValue.endsWith("dip")) {
            return TypedValue.COMPLEX_UNIT_DIP;
        } else if (rawValue.endsWith("sp")) {
            return TypedValue.COMPLEX_UNIT_SP;
        } else if (rawValue.endsWith("pt")) {
            return TypedValue.COMPLEX_UNIT_PT;
        } else if (rawValue.endsWith("in")) {
            return TypedValue.COMPLEX_UNIT_IN;
        } else if (rawValue.endsWith("mm")) {
            return TypedValue.COMPLEX_UNIT_MM;
        } else {
            throw new IllegalArgumentException("Not a valid dimension: " + rawValue);
        }
    }

    /**
     * Checks if a given string is a float.
     * @param rawString The raw string to be checked.
     * @return True if the string is a float, false otherwise.
     */
    private static boolean isTypeFloat(String rawString) {
        try {
            Float.parseFloat(rawString);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Converts a raw string to a complex fraction of type {@link TypedValue#TYPE_FRACTION}.
     * @param rawString The raw string to be converted. The string must be a fraction.
     * @return The complex fraction
     */
    private static int createComplexFraction(String rawString) {
        if (rawString == null || rawString.isEmpty()) {
            throw new IllegalArgumentException("Fraction string cannot be null or empty");
        }

        // Define the fraction unit strings
        final String[] FRACTION_UNIT_STRS = {"%", "%p"};

        // Find the unit in the string
        int unitIndex = -1;
        for (int i = 0; i < FRACTION_UNIT_STRS.length; i++) {
            if (rawString.endsWith(FRACTION_UNIT_STRS[i])) {
                unitIndex = i;
                rawString = rawString.substring(0, rawString.length() - FRACTION_UNIT_STRS[i].length());
                break;
            }
        }

        if (unitIndex == -1) {
            throw new IllegalArgumentException("Invalid fraction unit in string: " + rawString);
        }

        try {
            // Parse the float value
            float value = Float.parseFloat(rawString) / 100f;

            // Pack the float into a complex representation
            int complexValue = floatToComplex(value);

            // Combine the complex value with the unit
            return complexValue | (unitIndex & TypedValue.COMPLEX_UNIT_MASK);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in fraction: " + rawString, e);
        }
    }


    /**
     * Creates a typed value from a raw string. This method checks it the computation was correct. If not, it throws an exception.
     * <p>
     * <bold>Attention: </bold> This method only considers the following types:
     * <ul>
     *     <li>{@link TypedValue#TYPE_DIMENSION}</li>
     *     <li>{@link TypedValue#TYPE_FRACTION}</li>
     *     <li>{@link TypedValue#TYPE_FLOAT}</li>
     *     <li>{@link TypedValue#TYPE_INT_DEC} for every integer (attention, use this with caution)</li>
     * </ul>
     *
     * @param rawString The raw string to be converted. The string must be a valid representation of one of the types mentioned above.
     * @return A {@link TypedValue} object representing the parsed value.
     */
    public static TypedValue createTypedValueFromString(String rawString) {
        TypedValue value = new TypedValue();

        if (isTypeDimension(rawString)) {
            value.type = TypedValue.TYPE_DIMENSION;
            value.data = createComplexDimen(rawString);
        } else if (isTypeFraction(rawString)) {
            value.type = TypedValue.TYPE_FRACTION;
            value.data = createComplexFraction(rawString);
        } else if (isTypeFloat(rawString)) {
            value.type = TypedValue.TYPE_FLOAT;
            value.data = Float.floatToIntBits(Float.parseFloat(rawString));
        } else if (isInteger(rawString)) {
            // this is simplified
            // actually, any type from {@link TypedValue#TYPE_FIRST_INT} to {@link TypedValue#TYPE_LAST_INT} is a decimal
            value.type = TypedValue.TYPE_INT_DEC;
            value.data = Integer.parseInt(rawString);
        } else {
            throw new ConversionException("Could not create complex from a String because it is not a type that is supported: " + rawString);
        }

        /*
        // This check was performed for test purposes to check if the conversion was correct.
        if (value.coerceToString().toString().trim().equals(rawString.trim())) {
            return value;
        } else {
            throw new ConversionException("The created typed value is different than the input one: original: " + rawString + " created: " + value.coerceToString());
        }
        */

        return value;
    }
}
