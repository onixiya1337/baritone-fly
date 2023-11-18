package baritone.behavior;

import baritone.Baritone;
import baritone.api.Settings;
import baritone.api.behavior.ILookBehavior;
import baritone.api.behavior.look.IAimProcessor;
import baritone.api.behavior.look.ITickableAimProcessor;
import baritone.api.event.events.*;
import baritone.api.utils.CubicBezier;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Interpolator;
import baritone.api.utils.Rotation;
import baritone.behavior.look.ForkableRandom;
import net.minecraft.network.play.client.C03PacketPlayer;

import java.util.Optional;

public class LookBehavior extends Behavior implements ILookBehavior {

    private Target target;

    private Rotation serverRotation;

    private Rotation prevRotation;

    private AimProcessor processor;

    public LookBehavior(Baritone baritone) {
        super(baritone);
        this.processor = new AimProcessor(baritone.getPlayerContext());
    }

    @Override
    public void updateTarget(Rotation rotation, boolean blockInteract) {
        if (target != null && rotation.isReallyCloseTo(target.rotation)) {
            return;
        }
        if (target == null || Math.abs(target.rotation.subtract(rotation).getYaw()) > 25) {
            this.processor = new AimProcessor(ctx);
        }
        target = new Target(ctx.playerRotations(), rotation, Target.Mode.resolve(blockInteract));
    }

    @Override
    public IAimProcessor getAimProcessor() {
        return this.processor;
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }

        if (this.processor.finished()) {
            this.target = null;
            return;
        }

        this.processor.tick();
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
                final Rotation actual = this.processor.peekRotation(this.target.initial, this.target.rotation);
                ctx.player().rotationYaw = actual.getYaw();
                ctx.player().rotationPitch = actual.getPitch();
                break;
            }
            case POST: {
                if (this.prevRotation != null) {

                    if (this.target.mode == Target.Mode.SERVER) {
                        ctx.player().rotationYaw = prevRotation.getYaw();
                        ctx.player().rotationPitch = prevRotation.getPitch();
                    }

                    this.prevRotation = null;
                }
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
            final Rotation actual = this.processor.peekRotation(this.target.initial, this.target.rotation);
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

    private static abstract class AbstractAimProcessor implements ITickableAimProcessor {

        protected final IPlayerContext ctx;

        private final ForkableRandom rand;
        private double randomYawOffset;
        private double randomPitchOffset;


        private final Interpolator yawInterpolator;
        private final Interpolator pitchInterpolator;

        private float yawInterpolation;

        private float pitchInterpolation;

        public AbstractAimProcessor(IPlayerContext ctx) {
            this.ctx = ctx;
            this.rand = new ForkableRandom();
            this.yawInterpolator = new Interpolator(
                    Baritone.settings().yawRotationTimeTicks.value,
                    new CubicBezier(0.85, 0, 0.15, 1)
            );
            this.pitchInterpolator = new Interpolator(
                    Baritone.settings().pitchRotationTimeTicks.value,
                    new CubicBezier(0.22, 1, 0.36, 1)
            );
        }

        private AbstractAimProcessor(final AbstractAimProcessor source) {
            this.ctx = source.ctx;
            this.rand = source.rand.fork();
            this.randomYawOffset = source.randomYawOffset;
            this.randomPitchOffset = source.randomPitchOffset;
            this.yawInterpolator = source.yawInterpolator;
            this.pitchInterpolator = source.pitchInterpolator;
            this.yawInterpolation = source.yawInterpolation;
            this.pitchInterpolation = source.pitchInterpolation;
        }

        @Override
        public boolean finished() {
            return yawInterpolator.finished() && pitchInterpolator.finished();
        }

        @Override
        public final Rotation peekRotation(final Rotation initial, final Rotation rotation) {
            final Rotation prev = this.getPrevRotation();

            Rotation delta = rotation.subtract(initial).normalizeAndClamp();

            float desiredYaw = initial.getYaw() + delta.getYaw() * this.yawInterpolation;
            float desiredPitch = initial.getPitch() + delta.getPitch() * this.pitchInterpolation;

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
        public final void tick() {
            this.yawInterpolation = this.yawInterpolator.nextInterpolation();
            this.pitchInterpolation = this.pitchInterpolator.nextInterpolation();

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
            final Rotation actual = this.peekRotation(initial, rotation);
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
