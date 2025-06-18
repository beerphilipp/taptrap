package com.tapjacking.maltapanalyze;

import android.graphics.Path;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

import androidx.core.graphics.PathParser;

import com.tapjacking.maltapanalyze.exceptions.InvalidInterpolatorException;
import com.tapjacking.maltapanalyze.utils.XMLUtils;

import org.w3c.dom.Element;

/**
 * Helper class to create {@link android.view.animation.Interpolator}s from XML elements.
 */
public class InterpolatorHelper {

    /**
     * Creates an {@link Interpolator} from an XML element.
     *
     * @param element The {@link Element} to create the {@link Interpolator} from.
     * @return The {@link Interpolator} created from the XML element.
     */
    public static Interpolator createInterpolator(Element element) {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }

        String name = element.getTagName();
        Interpolator interpolator;

        String rawFactor = XMLUtils.getAttributeIgnoreNS(element, "factor");
        float realFactor = 1.0f;
        if (rawFactor != null && !rawFactor.isEmpty()) {
            realFactor = Float.parseFloat(rawFactor);
        }

        String rawTension = XMLUtils.getAttributeIgnoreNS(element, "tension");
        float realTension = 2.0f;
        if (rawTension != null && !rawTension.isEmpty()) {
            realTension = Float.parseFloat(rawTension);
        }

        String rawExtraTension = XMLUtils.getAttributeIgnoreNS(element, "extraTension");
        float realExtraTension = 1.5f;
        if (rawExtraTension != null && !rawExtraTension.isEmpty()) {
            realExtraTension = Float.parseFloat(rawExtraTension);
        }

        String rawCycles = XMLUtils.getAttributeIgnoreNS(element, "cycles");
        int realCycles = 1;
        if (rawCycles != null && !rawCycles.isEmpty()) {
            realCycles = (int)Float.parseFloat(rawCycles);
        }

        // Parameters for PathInterpolator
        String rawControlX1 = XMLUtils.getAttributeIgnoreNS(element, "controlX1");
        float realControlX1 = 0.0f;
        if (rawControlX1 != null && !rawControlX1.isEmpty()) {
            realControlX1 = Float.parseFloat(rawControlX1);
        }
        String rawControlY1 = XMLUtils.getAttributeIgnoreNS(element, "controlY1");
        float realControlY1 = 0.0f;
        if (rawControlY1 != null && !rawControlY1.isEmpty()) {
            realControlY1 = Float.parseFloat(rawControlY1);
        }
        String rawControlX2 = XMLUtils.getAttributeIgnoreNS(element, "controlX2");
        float realControlX2 = 0.0f;
        if (rawControlX2 != null && !rawControlX2.isEmpty()) {
            realControlX2 = Float.parseFloat(rawControlX2);
        }
        String rawControlY2 = XMLUtils.getAttributeIgnoreNS(element, "controlY2");
        float realControlY2 = 0.0f;
        if (rawControlY2 != null && !rawControlY2.isEmpty()) {
            realControlY2 = Float.parseFloat(rawControlY2);
        }

        String rawPathData = XMLUtils.getAttributeIgnoreNS(element, "pathData");

        switch (name) {
            case "accelerateDecelerateInterpolator":
                interpolator = new AccelerateDecelerateInterpolator();
                break;
            case "accelerateInterpolator":
                interpolator = new AccelerateInterpolator(realFactor);
                break;
            case "anticipateInterpolator":
                interpolator = new AnticipateInterpolator(realTension);
                break;
            case "anticipateOvershootInterpolator":
                interpolator = new AnticipateOvershootInterpolator(realTension, realExtraTension);
                break;
            case "bounceInterpolator":
                interpolator = new BounceInterpolator();
                break;
            case "cycleInterpolator":
                interpolator = new CycleInterpolator(realCycles);
                break;
            case "decelerateInterpolator":
                interpolator = new DecelerateInterpolator(realFactor);
                break;
            case "linearInterpolator":
                interpolator = new LinearInterpolator();
                break;
            case "overshootInterpolator":
                interpolator = new OvershootInterpolator(realTension);
                break;
            case "pathInterpolator":
                if (!(rawPathData == null) && !rawPathData.isEmpty()) {
                    Path path = PathParser.createPathFromPathData(rawPathData);
                    interpolator = new PathInterpolator(path);
                } else {
                    // This is simplified. The real implementation also makes sure that if controlX1 is present, controlY1 is also present and that if controlX2 is present, controlY2 is also present.
                    // If these conditions are not satisfied, the animation will not be created.
                    // Since our goal is to get an superset of the animation, we do not care about that.
                    if (rawControlX2 == null) {
                        interpolator = new PathInterpolator(realControlX1, realControlY1);
                    } else {
                        interpolator = new PathInterpolator(realControlX1, realControlY1, realControlX2, realControlY2);
                    }
                }
                break;
            default:
                throw new InvalidInterpolatorException(name);
        }

        return interpolator;
    }
}
