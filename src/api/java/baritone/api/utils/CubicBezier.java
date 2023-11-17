package baritone.api.utils;

import net.minecraft.util.MathHelper;

import java.util.function.Function;

public class CubicBezier {

    private static final double EPSILON = 1e-6;

    private final Function<Float, Float> calculateX;
    private final Function<Float, Float> calculateY;

    private final Function<Float, Float> calculateGradientX;

    public CubicBezier(double p1x, double p1y, double p2x, double p2y) {
        this(0, 0, p1x, p1y, p2x, p2y, 1, 1);
    }

    public CubicBezier(double p0x, double p0y, double p1x, double p1y, double p2x, double p2y, double p3x, double p3y) {
        this((float) p0x, (float) p0y, (float) p1x, (float) p1y, (float) p2x, (float) p2y, (float) p3x, (float) p3y);
    }

    public CubicBezier(float p1x, float p1y, float p2x, float p2y) {
        this(0, 0, p1x, p1y, p2x, p2y, 1, 1);
    }

    public CubicBezier(float p0x, float p0y, float p1x, float p1y, float p2x, float p2y, float p3x, float p3y) {
        this.calculateX = t -> (float) (
                Math.pow(1 - t, 3) * p0x +
                3 * Math.pow(1 - t, 2) * t * p1x +
                3 * (1 - t) * Math.pow(t, 2) * p2x +
                Math.pow(t, 3) * p3x
        );
        this.calculateY = t -> (float) (
                Math.pow(1 - t, 3) * p0y +
                3 * Math.pow(1 - t, 2) * t * p1y +
                3 * (1 - t) * Math.pow(t, 2) * p2y +
                Math.pow(t, 3) * p3y
        );
        this.calculateGradientX = t -> (float) (
                3 * Math.pow(1 - t, 2) * (p1x - p0x) +
                6 * (1 - t) * t * (p2x - p1x) +
                3 * Math.pow(t, 2) * (p3x - p2x)
        );
    }

    public float calculateYWithX(float x) {
        float currentT = x;
        for (int i = 0; i < 8; i++) {
            float delta = this.calculateX.apply(currentT) - x;
            if (Math.abs(delta) < EPSILON) {
                return this.calculateY.apply(currentT);
            }

            float slope = this.calculateGradientX.apply(currentT);
            if (Math.abs(slope) < EPSILON) {
                break;
            }

            currentT = currentT - delta / slope;
        }

        float lowerBound = 0;
        float upperBound = 1;

        currentT = x;

        if (currentT < lowerBound) {
            return this.calculateY.apply(lowerBound);
        }
        if (currentT > upperBound) {
            return this.calculateY.apply(upperBound);
        }

        while (lowerBound < upperBound) {
            float delta = this.calculateX.apply(currentT);
            if (Math.abs(delta) < EPSILON) {
                return this.calculateY.apply(currentT);
            }

            if (delta < 0) {
                lowerBound = currentT;
            } else {
                upperBound = currentT;
            }

            currentT = (upperBound - lowerBound) * 0.5f + lowerBound;
        }

        return this.calculateY.apply(currentT);
    }

    /*

    private static final double EPSILON = 1e-6;

    private final Function<Double, Double> xT;

    private final Function<Double, Double> yT;

    private final Function<Double, Double> derivativeXT;

    private final long startTime;

    public CubicBezier(double p1x, double p1y, double p2x, double p2y) {
        this(0, 0, p1x, p1y, p2x, p2y, 1, 1);
    }

    public CubicBezier(double p0x, double p0y, double p1x, double p1y, double p2x, double p2y, double p3x, double p3y) {
        this.xT = t -> Math.pow(1 - t, 3) * p0x +
                3 * Math.pow(1 - t, 2) * t * p1x +
                3 * (1 - t) * Math.pow(t, 2) * p2x +
                Math.pow(t, 3) * p3x;
        this.yT = t -> Math.pow(1 - t, 3) * p0y +
                3 * Math.pow(1 - t, 2) * t * p1y +
                3 * (1 - t) * Math.pow(t, 2) * p2y +
                Math.pow(t, 3) * p3y;
        this.derivativeXT = t -> 3 * Math.pow(1 - t, 2) * (p1x - p0x) +
                6 * (1 - t) * t * (p2x - p1x) +
                3 * Math.pow(t, 2) * (p3x - p2x);
        this.startTime = System.currentTimeMillis();
    }

    public double getInterpolation() {
        if (isFinished()) {
            return yOfX(1);
        }
        long dt = System.currentTimeMillis() - startTime;
        return yOfX((double) dt / 500d);
    }

    public boolean isFinished() {
        return System.currentTimeMillis() - startTime >= 500;
    }

    private double yOfX(double x) {
        double currentT = x;
        for (int i = 0; i < 8; i++) {
            double delta = this.xT.apply(currentT) - x;
            if (Math.abs(delta) < EPSILON) {
                return this.yT.apply(currentT);
            }

            double slope = this.derivativeXT.apply(currentT);
            if (Math.abs(slope) < EPSILON) {
                break;
            }

            currentT = currentT - delta / slope;
        }

        double lowerBound = 0;
        double upperBound = 1;

        currentT = x;

        if (currentT < lowerBound) {
            return this.yT.apply(lowerBound);
        }
        if (currentT > upperBound) {
            return this.yT.apply(upperBound);
        }

        while (lowerBound < upperBound) {
            double delta = this.xT.apply(currentT);
            if (Math.abs(delta) < EPSILON) {
                return this.yT.apply(currentT);
            }

            if (delta < 0) {
                lowerBound = currentT;
            } else {
                upperBound = currentT;
            }

            currentT = (upperBound - lowerBound) * 0.5 + lowerBound;
        }

        return this.yT.apply(currentT);
    }

     */
}
