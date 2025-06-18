package com.tapjacking.maltapanalyze;

import android.util.Pair;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.util.stream.IntStream;

import kotlin.Triple;

/**
 * This class is used to check if an animation is potentially malicious.
 */
public class AnimationChecker {

    private final int MAX_DURATION = 3000;
    private final double LOW_ALPHA_THRESHOLD = 0.1;
    private final double LOW_SCALE_THRESHOLD = 4;

    /**
     * Computes the score of an {@link Animation} ranging from 0 to 100 where 100 is the most suspicious.
     * The score is computed by simulating the {@link Animation} and querying the {@link Transformation} at each point in time.
     *
     * @param animation the {@link Animation} to check
     * @return the score of the {@link Animation} ranging from 0 to 100
     */
    public Triple<Integer, Integer, Boolean> checkAnimation(Animation animation) {

        // we perform the functions that WindowContainer.loadAnimation performs
        animation.restrictDuration(MAX_DURATION);

        animation.initialize(1080, 2400, 1080, 2400);
        animation.start();

        Transformation[] transformations = new Transformation[MAX_DURATION + 1];

        boolean isAnimationLengthExceeded = false;
        int transformationLength = 0;

        for (int i = 0; i <= MAX_DURATION + 1; i++) {
            if (animation.hasEnded()) {
                break;
            }

            Transformation transformation = new Transformation();
            animation.getTransformation(i, transformation);

            if (i > MAX_DURATION) {
                if (!animation.hasEnded()) {
                    // The animation was able to run longer than the maximum duration
                    isAnimationLengthExceeded = true;
                }
                break;
            }

            transformations[i] = transformation;
            transformationLength++;
        }

        // Create an array that does not contain null values at the end
        Transformation[] finalTransformations = new Transformation[transformationLength];
        System.arraycopy(transformations, 0, finalTransformations, 0, transformationLength);

        int alphaScore = calculateAlphaScore(finalTransformations);
        int viewScore = calculateScaleScore(finalTransformations);
        return new Triple<>(alphaScore, viewScore, isAnimationLengthExceeded);
    }

    /**
     * Calculates the scale score of the animation.
     * @param transformations the transformations of the animation
     * @return the scale score ranging from 0 to 100
     */
    private int calculateScaleScore(Transformation[] transformations) {
        Pair<Float, Float>[] scales = new Pair[MAX_DURATION];
        for (int i = 0; i < scales.length; i++) {
            if (transformations.length > i) {
                Transformation transformation = transformations[i];
                float[] values = new float[9];
                transformation.getMatrix().getValues(values);
                float xScale = values[0];
                float yScale = values[4];
                scales[i] = new Pair<>(xScale, yScale);
            } else {
                scales[i] = new Pair<>(1f, 1f);
            }
        }

        WeightFunction weightFunction = new WeightFunction();
        double score = IntStream.range(0, MAX_DURATION)
                .mapToDouble(i -> {
                    Pair<Float, Float> scale = scales[i];
                    float xScale = (float) scale.first;
                    float yScale = (float) scale.second;
                    double v = xScale >= LOW_SCALE_THRESHOLD || yScale >= LOW_SCALE_THRESHOLD ? 1 : 0;
                    return weightFunction.normalizedFunction(i) * v;
                })
                .sum();
        return (int)(score*100);
    }

    /**
     * Calculates the alpha score of the {@link Animation}.
     * @param transformations the {@link Transformation}s of the {@link Animation}
     * @return the alpha score ranging from 0 to 100
     */
    private int calculateAlphaScore(Transformation[] transformations) {
        double[] alphas = new double[MAX_DURATION];
        for (int i = 0; i < alphas.length; i++) {
            if (transformations.length > i) {
                float alpha = transformations[i].getAlpha();
                alphas[i] = alpha;
            } else {
                alphas[i] = 1;
            }
        }

        WeightFunction weightFunction = new WeightFunction();
        double score = IntStream.range(0, MAX_DURATION)
                .mapToDouble(i -> {
                    double v = (alphas[i] > LOW_ALPHA_THRESHOLD || alphas[i] == 0) ? 0 : 1;
                    return weightFunction.normalizedFunction(i) * v;
                })
                .sum();
        return (int)(score*100);
    }

    /**
     * This class represents the weighted score function.
     */
    public static class WeightFunction {

        private final double NORMALIZATION_FACTOR = IntStream.rangeClosed(0, 3000)
                .mapToDouble(this::unnormalizedFunction)
                .sum();

        private double unnormalizedFunction(int x) {
            return 1 - (1 / (1 + Math.exp(-0.005 * (x - 1500))));
        }

        public double normalizedFunction(int x) {
            return unnormalizedFunction(x) / NORMALIZATION_FACTOR;
        }
    }
}
