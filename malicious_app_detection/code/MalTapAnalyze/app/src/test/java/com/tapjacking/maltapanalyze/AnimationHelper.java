package com.tapjacking.maltapanalyze;

import static com.tapjacking.maltapanalyze.utils.ParseUtils.parseBoolean;
import static com.tapjacking.maltapanalyze.utils.ParseUtils.parseFloat;
import static com.tapjacking.maltapanalyze.utils.ParseUtils.parseInt;
import static com.tapjacking.maltapanalyze.utils.ParseUtils.parseLong;

import android.content.Context;
import android.view.InflateException;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;

import com.tapjacking.maltapanalyze.utils.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.sql.SQLException;

/**
 * Helper class to create animations from XML elements.
 */
public class AnimationHelper {

    /**
     * Creates an animation from the given XML element.
     *
     * @param dataReaderWriter The database reader and writer. This is used to read interpolators that are associated to the given animation from the database.
     * @param context The context.
     * @param parent The parent animation set. If the animation is a child of an animation set, this should be the parent animation set. Otherwise, this should be null.
     * @param element The XML element.
     * @return The animation that was created.
     * @throws SQLException If an exception occurs while reading interpolators from the database.
     */
    public static Animation createAnimation(DataReaderWriter dataReaderWriter, Context context, AnimationSet parent, Element element) throws SQLException {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }

        String name = element.getTagName();
        Animation anim;

        // Read standard attributes of the animation.
        // These attributes are common to all animations.
        Long duration = parseLong(XMLUtils.getAttributeIgnoreNS(element, "duration"));
        Long startOffset = parseLong(XMLUtils.getAttributeIgnoreNS(element, "startOffset"));
        Boolean setFillEnabled = parseBoolean(XMLUtils.getAttributeIgnoreNS(element, "fillEnabled"));
        Boolean setFillBefore = parseBoolean(XMLUtils.getAttributeIgnoreNS(element, "fillBefore"));
        Boolean setFillAfter = parseBoolean(XMLUtils.getAttributeIgnoreNS(element, "fillAfter"));
        Integer repeatCount = parseInt(XMLUtils.getAttributeIgnoreNS(element, "repeatCount"));
        Integer repeatMode = parseInt(XMLUtils.getAttributeIgnoreNS(element, "repeatMode"));
        Integer zAdjustment = parseInt(XMLUtils.getAttributeIgnoreNS(element, "zAdjustment"));
        Integer backdropColor = parseInt(XMLUtils.getAttributeIgnoreNS(element, "backdropColor"));
        Boolean detachWallpaper = parseBoolean(XMLUtils.getAttributeIgnoreNS(element, "detachWallpaper"));
        Boolean showWallpaper = parseBoolean(XMLUtils.getAttributeIgnoreNS(element, "showWallpaper"));
        Boolean hasRoundedCorners = parseBoolean(XMLUtils.getAttributeIgnoreNS(element, "hasRoundedCorners"));
        String interpolatorReference = XMLUtils.getAttributeIgnoreNS(element, "interpolator");
        Interpolator interpolator = null;
        if (!(interpolatorReference == null || interpolatorReference.isEmpty())) {
            interpolator = createInterpolator(dataReaderWriter, interpolatorReference);
        }

        // Depending on the name of the tag, we create a different animation
        switch (name) {
            case "set":
                Boolean shareInterpolator = parseBoolean(XMLUtils.getAttributeIgnoreNS(element, "shareInterpolator"));
                AnimationSet animSet = AnimationFactory.createAnimationSet(duration, startOffset, setFillEnabled,
                        setFillBefore, setFillAfter, repeatCount, repeatMode, zAdjustment, backdropColor,
                        detachWallpaper, showWallpaper, hasRoundedCorners, interpolator, shareInterpolator);

                NodeList children = element.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        Element childElement = (Element) child;
                        createAnimation(dataReaderWriter, context, animSet, childElement);
                    }
                }
                anim = animSet;
                break;

            case "alpha":
                Float fromAlpha = parseFloat(XMLUtils.getAttributeIgnoreNS(element, "fromAlpha"));
                Float toAlpha = parseFloat(XMLUtils.getAttributeIgnoreNS(element, "toAlpha"));
                anim = AnimationFactory.createAlphaAnimation(duration, startOffset, setFillEnabled, setFillBefore,
                        setFillAfter, repeatCount, repeatMode, zAdjustment, backdropColor, detachWallpaper,
                        showWallpaper, hasRoundedCorners, interpolator, fromAlpha, toAlpha);
                break;

            case "scale":
                String fromXScale = XMLUtils.getAttributeIgnoreNS(element, "fromXScale");
                String toXScale = XMLUtils.getAttributeIgnoreNS(element, "toXScale");
                String fromYScale = XMLUtils.getAttributeIgnoreNS(element, "fromYScale");
                String toYScale = XMLUtils.getAttributeIgnoreNS(element, "toYScale");
                String pivotX = XMLUtils.getAttributeIgnoreNS(element, "pivotX");
                String pivotY = XMLUtils.getAttributeIgnoreNS(element, "pivotY");
                anim = AnimationFactory.createScaleAnimation(duration, startOffset, setFillEnabled, setFillBefore,
                        setFillAfter, repeatCount, repeatMode, zAdjustment, backdropColor, detachWallpaper,
                        showWallpaper, hasRoundedCorners, interpolator, fromXScale, toXScale, fromYScale, toYScale,
                        pivotX, pivotY, context);
                break;

            case "rotate":
                Float fromDegrees = parseFloat(XMLUtils.getAttributeIgnoreNS(element, "fromDegrees"));
                Float toDegrees = parseFloat(XMLUtils.getAttributeIgnoreNS(element, "toDegrees"));
                String pivotX2 = XMLUtils.getAttributeIgnoreNS(element, "pivotX");
                String pivotY2 = XMLUtils.getAttributeIgnoreNS(element, "pivotY");

                anim = AnimationFactory.createRotateAnimation(duration, startOffset, setFillEnabled, setFillBefore,
                        setFillAfter, repeatCount, repeatMode, zAdjustment, backdropColor, detachWallpaper,
                        showWallpaper, hasRoundedCorners, interpolator, fromDegrees, toDegrees, pivotX2, pivotY2, context);
                break;

            case "translate":
                String fromXDelta = XMLUtils.getAttributeIgnoreNS(element, "fromXDelta");
                String toXDelta = XMLUtils.getAttributeIgnoreNS(element, "toXDelta");
                String fromYDelta = XMLUtils.getAttributeIgnoreNS(element, "fromYDelta");
                String toYDelta = XMLUtils.getAttributeIgnoreNS(element, "toYDelta");
                anim = AnimationFactory.createTranslateAnimation(duration, startOffset, setFillEnabled, setFillBefore,
                        setFillAfter, repeatCount, repeatMode, zAdjustment, backdropColor, detachWallpaper,
                        showWallpaper, hasRoundedCorners, interpolator, fromXDelta, toXDelta, fromYDelta, toYDelta, context);
                break;

            case "cliprect":
                // These are not documented by Android
                // We have not encountered a cliprect animation in the wild yet
                throw new InflateException("Cliprect animation used: " + name);

            case "extend":
                // These are not documented by Android
                // We have not encountered an extend animation in the wild yet
                throw new InflateException("Extend animation used: " + name);

            default:
                throw new InflateException("Unknown animation name: " + name);
        }

        if (parent != null) {
            parent.addAnimation(anim);
        }

        return anim;
    }


    /**
     * Creates an interpolator from the given interpolator reference.
     * @param interpolatorReference The interpolator reference. This needs to start with @@.
     * @return The interpolator that was created.
     * @throws SQLException If an exception occurs while reading the interpolator from the database.
     */
    public static Interpolator createInterpolator(DataReaderWriter dataReaderWriter, String interpolatorReference) throws SQLException {
        if (interpolatorReference.startsWith("@@")) {
            String hash = interpolatorReference.substring(2);
            // we have to get the interpolator xml from the database and parse it
            String interpolatorXML = dataReaderWriter.getInterpolatorContent(hash);
            Element interpolatorElement = XMLUtils.parseXML(interpolatorXML);
            return InterpolatorHelper.createInterpolator(interpolatorElement);
        } else {
            throw new IllegalArgumentException("Found an interpolator reference that does not start with @@: " + interpolatorReference);
        }
    }
}
