package baritone.bezier;

import baritone.api.utils.CubicBezier;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Arrays;

public class BezierTest {

    @Test
    public void test() {
        double[] x = {0, 0.25, 0.5, 0.8, 0.95, 1};
        double[] y = {0, 0.6, 0.5, 0.7, 0.5, 1};

        long start = System.currentTimeMillis();
        PolynomialSplineFunction bezierCurve = new SplineInterpolator().interpolate(x, y);
        long dt = System.currentTimeMillis() - start;
        System.out.println(dt);

        double errSum = 0;
        for (int i = 0; i < x.length; i++) {
            errSum += Math.abs(bezierCurve.value(x[i]) - y[i]);
        }

        System.out.println(errSum);
    }

}
