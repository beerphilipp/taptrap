package com.tapjacking.maltapanalyze;

import android.content.Context;
import android.view.animation.Animation;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tapjacking.maltapanalyze.exceptions.ConversionException;
import com.tapjacking.maltapanalyze.exceptions.InvalidInterpolatorException;
import com.tapjacking.maltapanalyze.utils.XMLUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.List;

import kotlin.Triple;
import me.tongfei.progressbar.ProgressBar;

/**
 * Main class to run the analysis.
 */
@RunWith(AndroidJUnit4.class)
@Config
public class Main {

    /**
     * Checks the animations in the database for potentially malicious behavior and saves the scores in the database.
     * The scores of an animation ranges from 0 to 100, where 0 means that the animation is not malicious and 100 means that the animation is highly suspicious.
     * The database must be provided as a system property with the key "database".
     */
    @Test
    public void run() {
        String database = System.getProperty("database", null);
        if (database == null) {
            throw new IllegalArgumentException("Database path must be provided");
        }

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        AnimationChecker animationChecker = new AnimationChecker();

        try {
            DataReaderWriter dataReaderWriter = new DataReaderWriter(database);
            List<String> uniqueAnimationHashes = dataReaderWriter.getUniqueAnimations();
            for (String hash : ProgressBar.wrap(uniqueAnimationHashes, "Analyzing animations")) {
                String animationXML = dataReaderWriter.getAnimationContent(hash);
                try {
                    Element element = XMLUtils.parseXML(animationXML);
                    Animation animation = AnimationHelper.createAnimation(dataReaderWriter, context, null, element);
                    Triple<Integer, Integer, Boolean> result = animationChecker.checkAnimation(animation);
                    dataReaderWriter.saveScore(hash, result.getFirst(), result.getSecond(), result.getThird(), animationXML);
                } catch (InvalidInterpolatorException e) {
                    dataReaderWriter.saveScoreException(hash, -2, animationXML, e);
                } catch (SQLException e) {
                    throw e;
                } catch (Exception e) {
                    dataReaderWriter.saveScoreException(hash, -1, animationXML, e);
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}