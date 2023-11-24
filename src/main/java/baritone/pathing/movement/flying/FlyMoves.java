package baritone.pathing.movement.flying;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.flying.flyMovements.MovementFlyDown;
import baritone.pathing.movement.flying.flyMovements.MovementFlyTraverse;
import baritone.pathing.movement.flying.flyMovements.MovementFlyUp;
import baritone.utils.pathing.MutableMoveResult;

public enum FlyMoves {
    FLY_TRAVERSE_NORTH(0, 0, -1) {
        @Override
        public Movement apply0(CalculationContext context, BetterBlockPos src) {
            return new MovementFlyTraverse(context.getBaritone(), src, src.north());
        }

        @Override
        public double cost(CalculationContext context, int x, int y, int z) {
            return MovementFlyTraverse.cost(context, x, y, z, x, z - 1);
        }
    },
    FLY_TRAVERSE_SOUTH(0, 0, +1) {
        @Override
        public Movement apply0(CalculationContext context, BetterBlockPos src) {
            return new MovementFlyTraverse(context.getBaritone(), src, src.south());
        }

        @Override
        public double cost(CalculationContext context, int x, int y, int z) {
            return MovementFlyTraverse.cost(context, x, y, z, x, z + 1);
        }
    },
    FLY_TRAVERSE_EAST(+1, 0, 0) {
        @Override
        public Movement apply0(CalculationContext context, BetterBlockPos src) {
            return new MovementFlyTraverse(context.getBaritone(), src, src.east());
        }

        @Override
        public double cost(CalculationContext context, int x, int y, int z) {
            return MovementFlyTraverse.cost(context, x, y, z, x + 1, z);
        }
    },
    FLY_TRAVERSE_WEST(-1, 0, 0) {
        @Override
        public Movement apply0(CalculationContext context, BetterBlockPos src) {
            return new MovementFlyTraverse(context.getBaritone(), src, src.west());
        }

        @Override
        public double cost(CalculationContext context, int x, int y, int z) {
            return MovementFlyTraverse.cost(context, x, y, z, x - 1, z);
        }
    },
    FLY_UP(0, +1, 0) {
        @Override
        public Movement apply0(CalculationContext context, BetterBlockPos src) {
            return new MovementFlyUp(context.getBaritone(), src, src.up());
        }

        @Override
        public double cost(CalculationContext context, int x, int y, int z) {
            return MovementFlyUp.cost(context, x, y, z);
        }
    },
    FLY_DOWN(0, -1, 0) {
        @Override
        public Movement apply0(CalculationContext context, BetterBlockPos src) {
            return new MovementFlyDown(context.getBaritone(), src, src.down());
        }

        @Override
        public double cost(CalculationContext context, int x, int y, int z) {
            return MovementFlyDown.cost(context, x, y, z);
        }
    }
    ;

    public final int xOffset;
    public final int yOffset;
    public final int zOffset;

    FlyMoves(int x, int y, int z) {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;
    }

    public abstract Movement apply0(CalculationContext context, BetterBlockPos src);

    public void apply(CalculationContext context, int x, int y, int z, MutableMoveResult result) {
        result.x = x + xOffset;
        result.y = y + yOffset;
        result.z = z + zOffset;
        result.cost = cost(context, x, y, z);
    }

    public double cost(CalculationContext context, int x, int y, int z) {
        throw new UnsupportedOperationException();
    }
}
