package baritone.api.pathing.movement;

import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.BlockPos;

public interface IMovement {

    double getCost();

    MovementStatus update();

    void reset();

    boolean safeToCancel();

    boolean calculatedWhileLoaded();

    BetterBlockPos getSrc();

    BetterBlockPos getDest();

    BlockPos getDirection();
}
