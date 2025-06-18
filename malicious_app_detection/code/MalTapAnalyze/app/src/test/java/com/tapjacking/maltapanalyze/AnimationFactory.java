package com.tapjacking.maltapanalyze;

import android.content.Context;
import android.icu.number.Scale;
import android.util.TypedValue;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import com.tapjacking.maltapanalyze.exceptions.ConversionException;
import com.tapjacking.maltapanalyze.utils.SizeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import kotlin.Pair;

/**
 * This class is a factory for creating different types of {@link Animation}s.
 */
public class AnimationFactory {

    /**
     * Sets the base values every {@link Animation} object has to the given animation.
     * Every parameter except the {@link Animation} object is optional and may be null.
     * If the parameter is null, the default value will be used.
     *
     * @param animation the {@link Animation} object to which the base values will be applied.
     */
    private static void createBaseAnimation(
            Animation animation,
            Long duration,
            Long startOffset,
            Boolean setFillEnabled,
            Boolean setFillBefore,
            Boolean setFillAfter,
            Integer repeatCount,
            Integer repeatMode,
            Integer zAdjustment,
            Integer backdropColor,
            Boolean detachWallpaper,
            Boolean showWallpaper,
            Boolean hasRoundedCorners,
            Interpolator interpolator
    ) {
        animation.setDuration(duration != null ? duration : 0);
        animation.setStartOffset(startOffset != null ? startOffset : 0);
        animation.setFillEnabled(setFillEnabled != null && setFillEnabled);
        animation.setFillBefore(setFillBefore != null ? setFillBefore : true);
        animation.setFillAfter(setFillAfter != null ? setFillAfter : false);
        animation.setRepeatCount(repeatCount != null ? repeatCount : 0);
        animation.setRepeatMode(repeatMode != null ? repeatMode : Animation.RESTART);

        animation.setZAdjustment(zAdjustment != null ? zAdjustment : 0);
        animation.setBackdropColor(backdropColor != null ? backdropColor : 0);
        animation.setDetachWallpaper(detachWallpaper != null && detachWallpaper);

        try {
            Field showWallpaperField = Animation.class.getDeclaredField("mShowWallpaper");
            showWallpaperField.setAccessible(true);
            showWallpaperField.setBoolean(animation, showWallpaper != null && showWallpaper);

            Field hasRoundedCornersField = Animation.class.getDeclaredField("mHasRoundedCorners");
            hasRoundedCornersField.setAccessible(true);
            hasRoundedCornersField.setBoolean(animation, hasRoundedCorners != null && hasRoundedCorners);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set show wallpaper or has rounded corners", e);
        }

        animation.setInterpolator(interpolator != null ? interpolator : new AccelerateDecelerateInterpolator());
    }

    /**
     * Creates an {@link AnimationSet} object.
     * Every parameter except is optional and may be null.
     * If the parameter is null, the default value will be used.
     *
     * @return the created {@link AnimationSet} object.
     */
    public static AnimationSet createAnimationSet(
            Long duration,
            Long startOffset,
            Boolean fillEnabled,
            Boolean fillBefore,
            Boolean fillAfter,
            Integer repeatCount,
            Integer repeatMode,
            Integer zAdjustment,
            Integer backdropColor,
            Boolean detachWallpaper,
            Boolean showWallpaper,
            Boolean hasRoundedCorners,
            Interpolator interpolator,

            Boolean shareInterpolator
    ) {
        if (shareInterpolator == null) shareInterpolator = true; // default value
        AnimationSet animationSet = new AnimationSet(shareInterpolator);
        createBaseAnimation(animationSet, duration, startOffset, fillEnabled, fillBefore, fillAfter,
                repeatCount, repeatMode, zAdjustment, backdropColor, detachWallpaper, showWallpaper,
                hasRoundedCorners, interpolator);

        try {
            Method setFlagMethod = AnimationSet.class.getDeclaredMethod("setFlag", int.class, boolean.class);
            setFlagMethod.setAccessible(true);
            setFlagMethod.invoke(animationSet, 0x10, shareInterpolator);

            Method initMethod = AnimationSet.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(animationSet);

            Field mFlagsField = AnimationSet.class.getDeclaredField("mFlags");
            mFlagsField.setAccessible(true);
            int mFlags = mFlagsField.getInt(animationSet);

            if (duration != null) mFlags |= 0x20;
            if (fillBefore != null) mFlags |= 0x2;
            if (fillAfter != null) mFlags |= 0x1;
            if (repeatMode != null) mFlags |= 0x4;
            if (startOffset != null) mFlags |= 0x8;

            mFlagsField.setInt(animationSet, mFlags);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set flags", e);
        }

        return animationSet;
    }

    /**
     * Creates an {@link AlphaAnimation} object.
     * @return the created {@link AlphaAnimation} object.
     */
    public static AlphaAnimation createAlphaAnimation(
            Long duration,
            Long startOffset,
            Boolean setFillEnabled,
            Boolean setFillBefore,
            Boolean setFillAfter,
            Integer repeatCount,
            Integer repeatMode,
            Integer zAdjustment,
            Integer backdropColor,
            Boolean detachWallpaper,
            Boolean showWallpaper,
            Boolean hasRoundedCorners,
            Interpolator interpolator,

            Float fromAlpha,
            Float toAlpha
    ) {
        float realFromAlpha = fromAlpha != null ? fromAlpha : 1.0f;
        float realToAlpha = toAlpha != null ? toAlpha : 1.0f;

        AlphaAnimation alphaAnimation = new AlphaAnimation(realFromAlpha, realToAlpha);
        createBaseAnimation(alphaAnimation, duration, startOffset, setFillEnabled, setFillBefore, setFillAfter,
                repeatCount, repeatMode, zAdjustment, backdropColor, detachWallpaper, showWallpaper,
                hasRoundedCorners, interpolator);

        return alphaAnimation;
    }

    /**
     * Creates a {@link ScaleAnimation} object.
     * @return the created {@link ScaleAnimation} object.
     */
    public static Animation createScaleAnimation(
            Long duration,
            Long startOffset,
            Boolean setFillEnabled,
            Boolean setFillBefore,
            Boolean setFillAfter,
            Integer repeatCount,
            Integer repeatMode,
            Integer zAdjustment,
            Integer backdropColor,
            Boolean detachWallpaper,
            Boolean showWallpaper,
            Boolean hasRoundedCorners,
            Interpolator interpolator,

            String rawFromXScale,
            String rawToXScale,
            String rawFromYScale,
            String rawToYScale,
            String rawPivotXValue,
            String rawPivotYValue,
            Context context
    ) {
        TypedValue pivotXSizeTypedValue = rawPivotXValue != null ? SizeUtils.createTypedValueFromString(rawPivotXValue) : null;
        TypedValue pivotYSizeTypedValue = rawPivotYValue != null ? SizeUtils.createTypedValueFromString(rawPivotYValue) : null;
        Pair<Integer, Float> pivotXSize = createDescriptionTypeValue(pivotXSizeTypedValue, context);
        Pair<Integer, Float> pivotYSize = createDescriptionTypeValue(pivotYSizeTypedValue, context);

        int pivotXType = pivotXSize.getFirst();
        float pivotXValue = pivotXSize.getSecond();
        int pivotYType = pivotYSize.getFirst();
        float pivotYValue = pivotYSize.getSecond();

        ScaleAnimation scaleAnimation = new ScaleAnimation(0.0f, 0.0f, 0.0f, 0.0f, pivotXType, pivotXValue, pivotYType, pivotYValue);

        try {

            float fromX = 0.0f;
            int fromXType = 0;
            int fromXData = 0;
            if (rawFromXScale != null) {
                TypedValue fromXTypedValue = SizeUtils.createTypedValueFromString(rawFromXScale);
                if (fromXTypedValue.type == TypedValue.TYPE_FLOAT) {
                    fromX = Float.parseFloat(rawFromXScale);
                } else {
                    fromXType = fromXTypedValue.type;
                    fromXData = fromXTypedValue.data;
                }
            }
            Field field = ScaleAnimation.class.getDeclaredField("mFromX");
            field.setAccessible(true);
            field.set(scaleAnimation, fromX);
            field = ScaleAnimation.class.getDeclaredField("mFromXType");
            field.setAccessible(true);
            field.set(scaleAnimation, fromXType);
            field = ScaleAnimation.class.getDeclaredField("mFromXData");
            field.setAccessible(true);
            field.set(scaleAnimation, fromXData);


            float toX = 0.0f;
            int toXType = 0;
            int toXData = 0;
            if (rawToXScale != null) {
                TypedValue toXTypedValue = SizeUtils.createTypedValueFromString(rawToXScale);
                if (toXTypedValue.type == TypedValue.TYPE_FLOAT) {
                    toX = Float.parseFloat(rawToXScale);
                } else {
                    toXType = toXTypedValue.type;
                    toXData = toXTypedValue.data;
                }
            }
            field = ScaleAnimation.class.getDeclaredField("mToX");
            field.setAccessible(true);
            field.set(scaleAnimation, toX);
            field = ScaleAnimation.class.getDeclaredField("mToXType");
            field.setAccessible(true);
            field.set(scaleAnimation, toXType);
            field = ScaleAnimation.class.getDeclaredField("mToXData");
            field.setAccessible(true);
            field.set(scaleAnimation, toXData);


            float fromY = 0.0f;
            int fromYType = 0;
            int fromYData = 0;
            if (rawFromYScale != null) {
                TypedValue fromYTypedValue = SizeUtils.createTypedValueFromString(rawFromYScale);
                if (fromYTypedValue.type == TypedValue.TYPE_FLOAT) {
                    fromY = Float.parseFloat(rawFromYScale);
                } else {
                    fromYType = fromYTypedValue.type;
                    fromYData = fromYTypedValue.data;
                }
            }
            field = ScaleAnimation.class.getDeclaredField("mFromY");
            field.setAccessible(true);
            field.set(scaleAnimation, fromY);
            field = ScaleAnimation.class.getDeclaredField("mFromYType");
            field.setAccessible(true);
            field.set(scaleAnimation, fromYType);
            field = ScaleAnimation.class.getDeclaredField("mFromYData");
            field.setAccessible(true);
            field.set(scaleAnimation, fromYData);

            float toY = 0.0f;
            int toYType = 0;
            int toYData = 0;
            if (rawToYScale != null) {
                TypedValue toYTypedValue = SizeUtils.createTypedValueFromString(rawToYScale);
                if (toYTypedValue.type == TypedValue.TYPE_FLOAT) {
                    toY = Float.parseFloat(rawToYScale);
                } else {
                    toYType = toYTypedValue.type;
                    toYData = toYTypedValue.data;
                }
            }
            field = ScaleAnimation.class.getDeclaredField("mToY");
            field.setAccessible(true);
            field.set(scaleAnimation, toY);
            field = ScaleAnimation.class.getDeclaredField("mToYType");
            field.setAccessible(true);
            field.set(scaleAnimation, toYType);
            field = ScaleAnimation.class.getDeclaredField("mToYData");
            field.setAccessible(true);
            field.set(scaleAnimation, toYData);
        } catch (ConversionException e) {
            // This is for us to check if we performed the conversion correctly
            // Not all exceptions that occur indicate that the conversion failed
            // Sometimes the value is just slightly off, which is negligible
            System.out.println("Conversion exception: " + e.getMessage());
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to set scale values", e);
        }

        createBaseAnimation(scaleAnimation, duration, startOffset, setFillEnabled, setFillBefore, setFillAfter,
                repeatCount, repeatMode, zAdjustment, backdropColor, detachWallpaper, showWallpaper,
                hasRoundedCorners, interpolator);

        // set the mResources field to context.getResources(). This field is private, so we need to use reflection. Resources are only used to get the display metrics.
        // call initializePivotPoint() method
        try {
            Field mResourcesField = ScaleAnimation.class.getDeclaredField("mResources");
            mResourcesField.setAccessible(true);
            mResourcesField.set(scaleAnimation, context.getResources());

            Method initializePivotPointMethod = ScaleAnimation.class.getDeclaredMethod("initializePivotPoint");
            initializePivotPointMethod.setAccessible(true);
            initializePivotPointMethod.invoke(scaleAnimation);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set resources or initialize pivot point", e);
        }

        return scaleAnimation;
    }

    /**
     * Creates a {@link TranslateAnimation} object.
     * @return the created {@link TranslateAnimation} object.
     */
    public static Animation createTranslateAnimation(
            Long duration,
            Long startOffset,
            Boolean setFillEnabled,
            Boolean setFillBefore,
            Boolean setFillAfter,
            Integer repeatCount,
            Integer repeatMode,
            Integer zAdjustment,
            Integer backdropColor,
            Boolean detachWallpaper,
            Boolean showWallpaper,
            Boolean hasRoundedCorners,
            Interpolator interpolator,

            String rawFromXDelta,
            String rawToXDelta,
            String rawFromYDelta,
            String rawToYDelta,
            Context context
    ) {

        TypedValue fromXDeltaTypedValue = rawFromXDelta != null ? SizeUtils.createTypedValueFromString(rawFromXDelta) : null;
        TypedValue toXDeltaTypedValue = rawToXDelta != null ? SizeUtils.createTypedValueFromString(rawToXDelta) : null;
        TypedValue fromYDeltaTypedValue = rawFromYDelta != null ? SizeUtils.createTypedValueFromString(rawFromYDelta) : null;
        TypedValue toYDeltaTypedValue = rawToYDelta != null ? SizeUtils.createTypedValueFromString(rawToYDelta) : null;

        Pair<Integer, Float> fromXDelta = createDescriptionTypeValue(fromXDeltaTypedValue, context);
        Pair<Integer, Float> toXDelta = createDescriptionTypeValue(toXDeltaTypedValue, context);
        Pair<Integer, Float> fromYDelta = createDescriptionTypeValue(fromYDeltaTypedValue, context);
        Pair<Integer, Float> toYDelta = createDescriptionTypeValue(toYDeltaTypedValue, context);

        int fromXType = fromXDelta.getFirst();
        float fromXValue = fromXDelta.getSecond();
        int toXType = toXDelta.getFirst();
        float toXValue = toXDelta.getSecond();
        int fromYType = fromYDelta.getFirst();
        float fromYValue = fromYDelta.getSecond();
        int toYType = toYDelta.getFirst();
        float toYValue = toYDelta.getSecond();

        TranslateAnimation translateAnimation = new TranslateAnimation(fromXType, fromXValue, toXType, toXValue, fromYType, fromYValue, toYType, toYValue);
        createBaseAnimation(translateAnimation, duration, startOffset, setFillEnabled, setFillBefore, setFillAfter,
                repeatCount, repeatMode, zAdjustment, backdropColor, detachWallpaper, showWallpaper,
                hasRoundedCorners, interpolator);
        return translateAnimation;
    }

    /**
     * Creates a {@link RotateAnimation} object.
     * @return the created {@link RotateAnimation} object.
     */
    public static Animation createRotateAnimation(
            Long duration,
            Long startOffset,
            Boolean setFillEnabled,
            Boolean setFillBefore,
            Boolean setFillAfter,
            Integer repeatCount,
            Integer repeatMode,
            Integer zAdjustment,
            Integer backdropColor,
            Boolean detachWallpaper,
            Boolean showWallpaper,
            Boolean hasRoundedCorners,
            Interpolator interpolator,

            Float fromDegrees,
            Float toDegrees,
            String rawPivotXValue,
            String rawPivotYValue,
            Context context
    ) {
        float realFromDegrees = fromDegrees != null ? fromDegrees : 0.0f;
        float realToDegrees = toDegrees != null ? toDegrees : 0.0f;

        TypedValue pivotXTypedValue = rawPivotXValue != null ? SizeUtils.createTypedValueFromString(rawPivotXValue) : null;
        TypedValue pivotYTypedValue = rawPivotYValue != null ? SizeUtils.createTypedValueFromString(rawPivotYValue) : null;

        Pair<Integer, Float> pivotXPair = createDescriptionTypeValue(pivotXTypedValue, context);
        Pair<Integer, Float> pivotYPair = createDescriptionTypeValue(pivotYTypedValue, context);

        int pivotXType = pivotXPair.getFirst();
        float pivotXValue = pivotXPair.getSecond();
        int pivotYType = pivotYPair.getFirst();
        float pivotYValue = pivotYPair.getSecond();

        RotateAnimation rotateAnimation = new RotateAnimation(realFromDegrees, realToDegrees, pivotXType, pivotXValue, pivotYType, pivotYValue);
        createBaseAnimation(rotateAnimation, duration, startOffset, setFillEnabled, setFillBefore, setFillAfter,
                repeatCount, repeatMode, zAdjustment, backdropColor, detachWallpaper, showWallpaper,
                hasRoundedCorners, interpolator);
        return rotateAnimation;
    }


    /**
     * Animation.Description is a protected class, so we need to use reflection to create an instance of it.
     * This method creates an instance of Animation.Description from a given {@link TypedValue} object and returns the type and value of the description as a pair.
     *
     * @param typedValue the {@link TypedValue} object to create the Description from.
     * @param context the {@link Context} object to use for the creation of the Description.
     * @return a pair of the type and value of the created Description. The first element of the pair is the type and the second element is the value.
     */
    public static Pair<Integer, Float> createDescriptionTypeValue(TypedValue typedValue, Context context) {
        try {
            Class<?> descriptionClass = Class.forName("android.view.animation.Animation$Description");
            Method parseValueMethod = descriptionClass.getDeclaredMethod("parseValue", TypedValue.class, Context.class);
            parseValueMethod.setAccessible(true);
            Object descriptionInstance = parseValueMethod.invoke(null, typedValue, context);

            Field typeField = descriptionClass.getDeclaredField("type");
            Field valueField = descriptionClass.getDeclaredField("value");
            typeField.setAccessible(true);
            valueField.setAccessible(true);
            int type = typeField.getInt(descriptionInstance);
            float value = valueField.getFloat(descriptionInstance);
            return new Pair<>(type, value);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to create Animation.Description", e);
        }

    }
}
