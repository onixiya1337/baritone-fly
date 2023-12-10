package baritone.behavior;

import baritone.Baritone;
import baritone.api.Settings;
import baritone.api.behavior.ILookBehavior;
import baritone.api.behavior.look.IAimProcessor;
import baritone.api.behavior.look.ITickableAimProcessor;
import baritone.api.event.events.*;
import baritone.api.utils.CubicBezier;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.behavior.look.ForkableRandom;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.Optional;

public class LookBehavior extends Behavior implements ILookBehavior {

    private Target target;

    private Rotation serverRotation;

    private Rotation prevRotation;

    private final AimProcessor processor;

    private boolean updated;

    public LookBehavior(Baritone baritone) {
        super(baritone);
        this.processor = new AimProcessor(baritone.getPlayerContext());
        this.updated = false;
    }

    @Override
    public void updateTarget(Rotation rotation, boolean blockInteract) {
        this.target = new Target(ctx.playerRotations(), rotation, Target.Mode.resolve(blockInteract));
        this.updated = true;
    }


    @Override
    public IAimProcessor getAimProcessor() {
        return this.processor;
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.IN) {
            this.processor.tick();
        }
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (target == null) {
            return;
        }

        switch (event.getState()) {
            case PRE: {
                if (this.target.mode == Target.Mode.NONE) {
                    return;
                }

                this.prevRotation = new Rotation(ctx.player().rotationYaw, ctx.player().rotationPitch);
                final Rotation interpolated = this.processor.interpolate(this.target.initial, this.target.rotation);
                final Rotation actual = this.processor.peekRotation(interpolated);
                ctx.player().rotationYaw = actual.getYaw();
                ctx.player().rotationPitch = actual.getPitch();
                break;
            }
            case POST: {
                if (this.prevRotation != null) {
                    if (this.target.mode == Target.Mode.SERVER && this.updated) {
                        ctx.player().rotationYaw = prevRotation.getYaw();
                        ctx.player().rotationPitch = prevRotation.getPitch();
                    }

                    this.prevRotation = null;
                }

                if (!this.updated) {
                    this.target = null;
                }

                this.updated = false;

                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onSendPacket(PacketEvent event) {
        if (!(event.getPacket() instanceof C03PacketPlayer)) {
            return;
        }

        final C03PacketPlayer packet = (C03PacketPlayer) event.getPacket();
        if (packet instanceof C03PacketPlayer.C05PacketPlayerLook || packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            this.serverRotation = new Rotation(packet.getYaw(), packet.getPitch());
        }
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        this.serverRotation = null;
        this.target = null;
    }

    public Optional<Rotation> getEffectiveRotation() {
        if (Baritone.settings().freeLook.value) {
            return Optional.ofNullable(this.serverRotation);
        }
        return Optional.empty();
    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent event) {
        if (target != null) {
            final Rotation interpolated = this.processor.interpolate(this.target.initial, this.target.rotation);
            final Rotation actual = this.processor.peekRotation(interpolated);
            event.setYaw(actual.getYaw());
            event.setPitch(actual.getPitch());
        }
    }

    private static final class AimProcessor extends AbstractAimProcessor {

        public AimProcessor(final IPlayerContext ctx) {
            super(ctx);
        }

        @Override
        protected Rotation getPrevRotation() {
            return ctx.playerRotations();
        }
    }

    private static double calculateXForY(PolynomialSplineFunction bezierCurve, double targetY) {
        double epsilon = 1e-6;

        double x = 0.5;
        double yValue = bezierCurve.value(x);
        double derivative = bezierCurve.derivative().value(x);

        while (Math.abs(yValue - targetY) > epsilon) {
            x = x - (yValue - targetY) / derivative;
            if (x > 1) {
                return 1;
            }
            if (x < 0) {
                return 0;
            }
            yValue = bezierCurve.value(x);
            derivative = bezierCurve.derivative().value(x);
        }
        return x;
    }

    private static abstract class AbstractAimProcessor implements ITickableAimProcessor {

        protected final IPlayerContext ctx;

        private final ForkableRandom rand;
        private double randomYawOffset;
        private double randomPitchOffset;

        private final PolynomialSplineFunction yawBezier;
        private final PolynomialSplineFunction pitchBezier;


        public AbstractAimProcessor(IPlayerContext ctx) {
            this.ctx = ctx;
            this.rand = new ForkableRandom();

            CubicBezier yawBezier = new CubicBezier(0.56, 0.17, 0.29, 1); // TODO: Custom beziers perhaps fitting

            double[] yawX = new double[11];
            double[] yawY = new double[11];

            for (int i = 0; i < 11; i++) {
                float x = ((float) i) / 10f;
                yawX[i] = x;
                yawY[i] = yawBezier.calculateYWithX(x);
            }

            this.yawBezier = new SplineInterpolator().interpolate(yawX, yawY);

            CubicBezier pitchBezier = new CubicBezier(0.22,1, 0.36,1);
            double[] pitchX = new double[11];
            double[] pitchY = new double[11];

            for (int i = 0; i < 11; i++) {
                float x = ((float) i) / 10f;
                pitchX[i] = x;
                pitchY[i] = pitchBezier.calculateYWithX(x);
            }

            this.pitchBezier = new SplineInterpolator().interpolate(pitchX, pitchY);
        }

        private AbstractAimProcessor(final AbstractAimProcessor source) {
            this.ctx = source.ctx;
            this.rand = source.rand.fork();
            this.randomYawOffset = source.randomYawOffset;
            this.randomPitchOffset = source.randomPitchOffset;
            this.yawBezier = source.yawBezier;
            this.pitchBezier = source.pitchBezier;
        }

        @Override
        public final Rotation peekRotation(final Rotation rotation) {
            final Rotation prev = this.getPrevRotation();

            float desiredYaw = rotation.getYaw();
            float desiredPitch = rotation.getPitch();

            if (desiredPitch == prev.getPitch()) {
                desiredPitch = nudgeToLevel(desiredPitch);
            }

            desiredYaw += this.randomYawOffset;
            desiredPitch += this.randomPitchOffset;

            return new Rotation(
                    this.calculateMouseMove(prev.getYaw(), desiredYaw),
                    this.calculateMouseMove(prev.getPitch(), desiredPitch)
            ).clamp();
        }

        @Override
        public final Rotation interpolate(final Rotation initial, final Rotation rotation) {
            final Rotation prev = this.getPrevRotation();

            Rotation deltaInitial = prev.subtract(initial).normalizeAndClamp();
            Rotation delta = rotation.subtract(initial).normalizeAndClamp();

            double yawProgress = calculateXForY(yawBezier, deltaInitial.getYaw() / delta.getYaw());
            yawProgress = Math.ceil(yawProgress * 10) / 10d + Baritone.settings().yawSmoothingFactor.value; // TODO: manage to get this with ticks instead of a factor somehow
            if (yawProgress > 1) {
                yawProgress = 1;
            }
            if (yawProgress < 0) {
                yawProgress = 0;
            }
            float interpolatedYaw = (float) (initial.getYaw() + yawBezier.value(yawProgress) * delta.getYaw()); //TODO: the shit (factor) is to sensitive aswell

            double pitchProgress = calculateXForY(pitchBezier, deltaInitial.getPitch() / delta.getPitch());
            pitchProgress = Math.ceil(pitchProgress * 10) / 10d + Baritone.settings().pitchSmoothingFactor.value;
            if (pitchProgress > 1) {
                pitchProgress = 1;
            }
            if (pitchProgress < 0) {
                pitchProgress = 0;
            }
            float interpolatedPitch = (float) (initial.getPitch() + pitchBezier.value(pitchProgress) * delta.getPitch());

            return new Rotation(
                    this.calculateMouseMove(prev.getYaw(), interpolatedYaw),
                    this.calculateMouseMove(prev.getPitch(), interpolatedPitch)
            ).clamp();
        }

        @Override
        public final void tick() {
            this.randomYawOffset = (this.rand.nextDouble() - 0.5) * Baritone.settings().randomLooking.value;
            this.randomPitchOffset = (this.rand.nextDouble() - 0.5) * Baritone.settings().randomLooking.value;

            // randomLooking113
            double random = this.rand.nextDouble() - 0.5;
            if (Math.abs(random) < 0.1) {
                random *= 4;
            }
            this.randomYawOffset += random * Baritone.settings().randomLooking113.value;
        }

        @Override
        public final void advance(int ticks) {
            for (int i = 0; i < ticks; i++) {
                this.tick();
            }
        }

        @Override
        public Rotation nextRotation(final Rotation initial, final Rotation rotation) {
            final Rotation interpolated = this.interpolate(initial, rotation);
            final Rotation actual = this.peekRotation(interpolated);
            this.tick();
            return actual;
        }

        @Override
        public final ITickableAimProcessor fork() {
            return new AbstractAimProcessor(this) {

                private Rotation prev = AbstractAimProcessor.this.getPrevRotation();

                @Override
                public Rotation nextRotation(final Rotation initial, final Rotation rotation) {
                    return (this.prev = super.nextRotation(initial, rotation));
                }

                @Override
                protected Rotation getPrevRotation() {
                    return this.prev;
                }
            };
        }

        protected abstract Rotation getPrevRotation();

        private float nudgeToLevel(float pitch) {
            if (pitch < -20) {
                return pitch + 1;
            } else if (pitch > 10) {
                return pitch - 1;
            }
            return pitch;
        }

        private float calculateMouseMove(float current, float target) {
            final float delta = target - current;
            final int deltaPx = angleToMouse(delta);
            return current + mouseToAngle(deltaPx);
        }

        private int angleToMouse(float angleDelta) {
            final float minAngleChange = mouseToAngle(1);
            return Math.round(angleDelta / minAngleChange);
        }

        private float mouseToAngle(int mouseDelta) {
            final float f = ctx.minecraft().gameSettings.mouseSensitivity * 0.6f + 0.2f;
            return mouseDelta * f * f * f * 8.0f * 0.15f;
        }
    }

    private static class Target {

        public final Rotation initial;

        public final Rotation rotation;

        public final Mode mode;

        public Target(Rotation initial, Rotation rotation, Mode mode) {
            this.initial = initial;
            this.rotation = rotation;
            this.mode = mode;
        }

        enum Mode {

            CLIENT,

            SERVER,

            NONE;

            static Mode resolve(boolean blockInteract) {
                final Settings settings = Baritone.settings();
                final boolean antiCheat = settings.antiCheatCompatibility.value;
                final boolean blockFreeLook = settings.blockFreeLook.value;

                if (settings.freeLook.value) {
                    if (blockInteract) {
                        return blockFreeLook ? SERVER : CLIENT;
                    }
                    return antiCheat ? SERVER : NONE;
                }

                return CLIENT;
            }
        }
    }
}
