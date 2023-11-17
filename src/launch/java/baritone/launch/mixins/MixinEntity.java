package baritone.launch.mixins;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.RotationMoveEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Entity.class)
public class MixinEntity {

    @Shadow
    public float rotationYaw;

    @Shadow
    public float rotationPitch;

    @Unique
    private RotationMoveEvent motionUpdateRotationEvent;

    @Inject(
            method = "moveFlying",
            at = @At("HEAD")
    )
    private void moveFlyingHead(float strafe, float forward, float friction, CallbackInfo ci) {
        this.getBaritone().ifPresent(baritone -> {
            this.motionUpdateRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.rotationYaw, this.rotationPitch);
            baritone.getGameEventHandler().onPlayerRotationMove(this.motionUpdateRotationEvent);
            this.rotationYaw = this.motionUpdateRotationEvent.getYaw();
            this.rotationPitch = this.motionUpdateRotationEvent.getPitch();
        });
    }

    @Inject(
            method = "moveFlying",
            at = @At("RETURN")
    )
    private void moveFlyingReturn(float strafe, float forward, float friction, CallbackInfo ci) {
        if (this.motionUpdateRotationEvent != null) {
            this.rotationYaw = this.motionUpdateRotationEvent.getOriginal().getYaw();
            this.rotationPitch = this.motionUpdateRotationEvent.getOriginal().getPitch();
            this.motionUpdateRotationEvent = null;
        }
    }


    @Unique
    private Optional<IBaritone> getBaritone() {
        // noinspection ConstantConditions
        if (EntityPlayerSP.class.isInstance(this)) {
            return Optional.ofNullable(BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this));
        } else {
            return Optional.empty();
        }
    }
}
