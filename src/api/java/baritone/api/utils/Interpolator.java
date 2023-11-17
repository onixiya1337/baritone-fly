package baritone.api.utils;

public class Interpolator {

    private final CubicBezier bezier;

    private final int animationTimeTicks;

    private int currentTicks;

    public Interpolator(int animationTimeTicks, CubicBezier bezier) {
        this.bezier = bezier;
        this.animationTimeTicks = animationTimeTicks;
        this.currentTicks = 0;
    }

    public boolean finished() {
        return currentTicks >= animationTimeTicks;
    }

    public float nextInterpolation() {
        if (finished()) {
            return bezier.calculateYWithX(1);
        }

        currentTicks++;

        float progress = (float) currentTicks / (float) animationTimeTicks;
        return bezier.calculateYWithX(progress);
    }
}
