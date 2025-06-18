package com.tapjacking.maltapextract.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for handling animation-related constants and attributes.
 * This class provides lists of attributes and tags that are used in tween animations.
 */
public class AnimUtils {

    /**
     * List of attributes that are used in tween animations.
     */
    public static List<String> TWEEN_ANIM_ATTRIBUTES = Arrays.stream(new String[]{
            "duration",
            "startOffset",
            "fillEnabled",
            "fillBefore",
            "fillAfter",
            "repeatCount",
            "repeatMode",
            "zAdjustment",
            "backdropColor",
            "detachWallpaper",
            "showWallpaper",
            "hasRoundedCorners",
            "interpolator",

            "shareInterpolator",

            "fromAlpha",
            "toAlpha",

            "fromXScale",
            "toXScale",
            "fromYScale",
            "toYScale",
            "pivotX",
            "pivotY",

            "fromDegrees",
            "toDegrees",

            "fromXDelta",
            "toXDelta",
            "fromYDelta",
            "toYDelta",
    }).toList();

    /**
     * List of tags that are used in tween animations.
     */
    public static final List<String> TWEEN_TAGS = new ArrayList<>(Arrays.asList("alpha", "scale", "translate", "rotate", "set"));
}
