package baritone.utils;

import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import net.minecraft.util.*;

import java.util.Arrays;
import java.util.List;

public enum HorizontalMovements {

    NO_MOVEMENT(0),

    FORWARD(0, Input.MOVE_FORWARD),

    SPRINT_FORWARD(0, Input.MOVE_FORWARD, Input.SPRINT),

    BACKWARD(180, Input.MOVE_BACK),

    //SNEAK_BACKWARD(180, Input.MOVE_BACK, Input.SNEAK),

    LEFT(-90, Input.MOVE_LEFT),

    //SNEAK_LEFT(-90, Input.MOVE_LEFT, Input.SNEAK),

    RIGHT(90, Input.MOVE_RIGHT),

    //SNEAK_RIGHT(90, Input.MOVE_RIGHT, Input.SNEAK),

    FORWARD_LEFT(-45, Input.MOVE_FORWARD, Input.MOVE_LEFT),

    SPRINT_FORWARD_LEFT(-45, Input.MOVE_FORWARD, Input.MOVE_LEFT, Input.SPRINT),

    FORWARD_RIGHT(45, Input.MOVE_FORWARD, Input.MOVE_RIGHT),

    SPRINT_FORWARD_RIGHT(45, Input.MOVE_FORWARD, Input.MOVE_RIGHT, Input.SPRINT),

    BACKWARD_LEFT(-135, Input.MOVE_BACK, Input.MOVE_LEFT),

    BACKWARD_RIGHT(135, Input.MOVE_BACK, Input.MOVE_RIGHT)
    ;

    private final int movementAngle;

    private final Input[] keys;

    HorizontalMovements(int movementAngle, Input... keys) {
        this.movementAngle = movementAngle;
        this.keys = keys;
    }

    public Input[] getKeys() {
        return keys;
    }

    public static float getMoveSpeed(List<Input> keys, double speed) {
        double movementFactor = 1;

        if (keys.contains(Input.SPRINT)) {
            movementFactor *= 1.3;
        }

        return (float) (0.1 * movementFactor * speed);
    }

    public static float getSpeed(IPlayerContext ctx) {
        return ctx.player().getAIMoveSpeed() / (0.1f * (ctx.player().isSprinting() ? 1.3f : 1f));
    }

    public Vec3 pred(PlayerSimulation sim, IPlayerContext ctx) {
        List<Input> inputs = Arrays.asList(keys);

        float moveStrafe = 0.0F;
        float moveForward = 0.0F;

        if (inputs.contains(Input.MOVE_FORWARD)) {
            moveForward++;
        }

        if (inputs.contains(Input.MOVE_BACK)) {
            moveForward--;
        }

        if (inputs.contains(Input.MOVE_LEFT)) {
            moveStrafe++;
        }

        if (inputs.contains(Input.MOVE_RIGHT)) {
            moveStrafe--;
        }

        if (inputs.contains(Input.SNEAK)) {
            moveStrafe *= 0.3D;
            moveForward *= 0.3D;
        }

        sim.moveForward = moveForward;
        sim.moveStrafing = moveStrafe;
        sim.landMovementFactor = getMoveSpeed(inputs, getSpeed(ctx));
        sim.isSneaking = inputs.contains(Input.SNEAK);
        if (inputs.contains(Input.SPRINT)) {
            sim.jumpMovementFactor *= 1.3;
        }

        sim.onLivingUpdate();

        return new Vec3(sim.posX, sim.posY, sim.posZ);
    }

    public Vec3 predict(IPlayerContext ctx) {
        List<Input> inputs = Arrays.asList(keys);

        float moveStrafe = 0.0F;
        float moveForward = 0.0F;

        if (inputs.contains(Input.MOVE_FORWARD)) {
            moveForward++;
        }

        if (inputs.contains(Input.MOVE_BACK)) {
            moveForward--;
        }

        if (inputs.contains(Input.MOVE_LEFT)) {
            moveStrafe++;
        }

        if (inputs.contains(Input.MOVE_RIGHT)) {
            moveStrafe--;
        }

        if (inputs.contains(Input.SNEAK)) {
            moveStrafe *= 0.3D;
            moveForward *= 0.3D;
        }

        PlayerSimulation sim = new PlayerSimulation(ctx.world());

        sim.copy(ctx.player());

        sim.moveForward = moveForward;
        sim.moveStrafing = moveStrafe;
        sim.landMovementFactor = getMoveSpeed(inputs, getSpeed(ctx));
        sim.isSneaking = inputs.contains(Input.SNEAK);

        sim.onLivingUpdate();

        return new Vec3(sim.posX, sim.posY, sim.posZ);
    }
}
