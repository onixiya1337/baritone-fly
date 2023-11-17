package baritone.api.event.events;

import baritone.api.utils.Rotation;

public final class RotationMoveEvent {

    private final Type type;

    private final Rotation original;

    private float yaw;

    private float pitch;

    public RotationMoveEvent(Type type, float yaw, float pitch) {
        this.type = type;
        this.original = new Rotation(yaw, pitch);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Rotation getOriginal() {
        return this.original;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getPitch() {
        return pitch;
    }

    public Type getType() {
        return this.type;
    }

    public enum Type {

        MOTION_UPDATE,

        JUMP
    }
}
