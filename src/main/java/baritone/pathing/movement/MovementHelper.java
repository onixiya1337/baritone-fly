package baritone.pathing.movement;

import baritone.Baritone;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.pathing.precompute.Ternary;
import baritone.utils.BlockStateInterface;
import baritone.utils.HorizontalMovements;
import baritone.utils.InputOverrideHandler;
import baritone.utils.PlayerSimulation;
import net.minecraft.block.*;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;

import java.util.Arrays;

import static baritone.pathing.precompute.Ternary.*;

public interface MovementHelper extends ActionCosts, Helper {

    static boolean canWalkThrough(IPlayerContext ctx, BetterBlockPos pos) {
        return canWalkThrough(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkThrough(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canWalkThrough(CalculationContext context, int x, int y, int z, IBlockState state) {
        return context.precomputedData.canWalkThrough(context.bsi, x, y, z, state);
    }

    static boolean canWalkThrough(CalculationContext context, int x, int y, int z) {
        return context.precomputedData.canWalkThrough(context.bsi, x, y, z, context.get(x, y, z));
    }

    static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Ternary canWalkThrough = canWalkThroughBlockState(state);
        if (canWalkThrough == YES) {
            return true;
        }
        if (canWalkThrough == NO) {
            return false;
        }
        return canWalkThroughPosition(bsi, x, y, z, state);
    }

    static Ternary canWalkThroughBlockState(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.air) {
            return YES;
        }
        if (block == Blocks.fire || block == Blocks.tripwire || block == Blocks.web || block == Blocks.end_portal || block == Blocks.cocoa || block instanceof BlockSkull || block instanceof BlockTrapDoor) {
            return NO;
        }
        if (Baritone.settings().blocksToAvoid.value.contains(block)) {
            return NO;
        }
        if (block instanceof BlockDoor || block instanceof BlockFenceGate) {
            if (block == Blocks.iron_door) {
                return NO;
            }
            return YES;
        }
        if (block == Blocks.carpet) {
            return MAYBE;
        }
        if (block instanceof BlockSnow) {
            return MAYBE;
        }
        if (block instanceof BlockLiquid) {
            if (state.getValue(BlockLiquid.LEVEL) != 0) {
                return NO;
            } else {
                return MAYBE;
            }
        }
        if (block instanceof BlockCauldron) {
            return NO;
        }
        try {
            if (block.isPassable(null, null)) {
                return YES;
            } else {
                return NO;
            }
        } catch (Throwable exception) {
            System.out.println("The block " + state.getBlock().getLocalizedName() + " requires a special case due to the exception " + exception.getMessage());
            return MAYBE;
        }
    }

    static boolean canWalkThroughPosition(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.carpet) {
            return canWalkOn(bsi, x, y - 1, z);
        }

        if (block instanceof BlockSnow) {
            if (!bsi.worldContainsLoadedChunk(x, z)) {
                return true;
            }
            if (state.getValue(BlockSnow.LAYERS) >= 3) {
                return false;
            }
            return canWalkOn(bsi, x, y - 1, z);
        }

        if (block instanceof BlockLiquid) {
            if (isFlowing(x, y, z, state, bsi)) {
                return false;
            }
            if (Baritone.settings().assumeWalkOnWater.value) {
                return false;
            }

            IBlockState up = bsi.get0(x, y + 1, z);
            if (up.getBlock() instanceof BlockLiquid || up.getBlock() instanceof BlockLilyPad) {
                return false;
            }
            return block == Blocks.water || block == Blocks.flowing_water;
        }

        return block.isPassable(bsi.access, bsi.isPassableBlockPos.set(x, y, z));
    }

    static Ternary fullyPassableBlockState(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.air) {
            return YES;
        }
        if (block == Blocks.fire
                || block == Blocks.tripwire
                || block == Blocks.web
                || block == Blocks.vine
                || block == Blocks.ladder
                || block == Blocks.cocoa
                || block instanceof BlockDoor
                || block instanceof BlockFenceGate
                || block instanceof BlockSnow
                || block instanceof BlockLiquid
                || block instanceof BlockTrapDoor
                || block instanceof BlockEndPortal
                || block instanceof BlockSkull) {
            return NO;
        }
        try {
            if (block.isPassable(null, null)) {
                return YES;
            } else {
                return NO;
            }
        } catch (Throwable exception) {
            System.out.println("The block " + state.getBlock().getLocalizedName() + " requires a special case due to the exception " + exception.getMessage());
            return MAYBE;
        }
    }

    static boolean fullyPassable(CalculationContext context, int x, int y, int z) {
        return fullyPassable(context, x, y, z, context.get(x, y, z));
    }

    static boolean fullyPassable(CalculationContext context, int x, int y, int z, IBlockState state) {
        return context.precomputedData.fullyPassable(context.bsi, x, y, z, state);
    }

    static boolean fullyPassable(IPlayerContext ctx, BlockPos pos) {
        IBlockState state = ctx.world().getBlockState(pos);
        Ternary fullyPassable = fullyPassableBlockState(state);
        if (fullyPassable == YES) {
            return true;
        }
        if (fullyPassable == NO) {
            return false;
        }
        return fullyPassablePosition(new BlockStateInterface(ctx), pos.getX(), pos.getY(), pos.getZ(), state); // meh
    }

    static boolean fullyPassablePosition(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        return state.getBlock().isPassable(bsi.access, bsi.isPassableBlockPos.set(x, y, z));
    }

    static boolean isReplaceable(int x, int y, int z, IBlockState state, BlockStateInterface bsi) {
        Block block = state.getBlock();
        if (block == Blocks.air || isWater(block)) {
            return true;
        }
        if (block instanceof BlockSnow) {
            if (!bsi.worldContainsLoadedChunk(x, z)) {
                return true;
            }
            return state.getValue(BlockSnow.LAYERS) == 1;
        }
        if (block instanceof BlockDoublePlant) {
            BlockDoublePlant.EnumPlantType kek = state.getValue(BlockDoublePlant.VARIANT);
            return kek == BlockDoublePlant.EnumPlantType.FERN || kek == BlockDoublePlant.EnumPlantType.GRASS;
        }
        return state.getBlock().getMaterial().isReplaceable();
    }

    @Deprecated
    static boolean isReplacable(int x, int y, int z, IBlockState state, BlockStateInterface bsi) {
        return isReplaceable(x, y, z, state, bsi);
    }

    static boolean isDoorPassable(IPlayerContext ctx, BlockPos doorPos, BlockPos playerPos) {
        if (playerPos.equals(doorPos)) {
            return false;
        }

        IBlockState state = BlockStateInterface.get(ctx, doorPos);
        if (!(state.getBlock() instanceof BlockDoor)) {
            return true;
        }

        return isHorizontalBlockPassable(doorPos, state, playerPos, BlockDoor.OPEN);
    }

    static boolean isGatePassable(IPlayerContext ctx, BlockPos gatePos, BlockPos playerPos) {
        if (playerPos.equals(gatePos)) {
            return false;
        }

        IBlockState state = BlockStateInterface.get(ctx, gatePos);
        if (!(state.getBlock() instanceof BlockFenceGate)) {
            return true;
        }

        return state.getValue(BlockFenceGate.OPEN);
    }

    static boolean isHorizontalBlockPassable(BlockPos blockPos, IBlockState blockState, BlockPos playerPos, PropertyBool propertyOpen) {
        if (playerPos.equals(blockPos)) {
            return false;
        }

        EnumFacing.Axis facing = blockState.getValue(PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL)).getAxis();
        boolean open = blockState.getValue(propertyOpen);

        EnumFacing.Axis playerFacing;
        if (playerPos.north().equals(blockPos) || playerPos.south().equals(blockPos)) {
            playerFacing = EnumFacing.Axis.Z;
        } else if (playerPos.east().equals(blockPos) || playerPos.west().equals(blockPos)) {
            playerFacing = EnumFacing.Axis.X;
        } else {
            return true;
        }

        return (facing == playerFacing) == open;
    }

    static boolean avoidWalkingInto(Block block) {
        return block instanceof BlockLiquid
                || block == Blocks.cactus
                || block == Blocks.fire
                || block == Blocks.end_portal
                || block == Blocks.web;
    }

    static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Ternary canWalkOn = canWalkOnBlockState(state);
        if (canWalkOn == YES) {
            return true;
        }
        if (canWalkOn == NO) {
            return false;
        }
        return canWalkOnPosition(bsi, x, y, z, state);
    }

    static Ternary canWalkOnBlockState(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.slime_block) {
            return NO;
        }
        if (state.getBlock().isBlockNormalCube()) {
            return YES;
        }
        if (block == Blocks.ladder || (block == Blocks.vine && Baritone.settings().allowVines.value)) {
            return YES;
        }
        if (block == Blocks.farmland || block == Blocks.grass) {
            return YES;
        }
        if (block == Blocks.ender_chest || block == Blocks.chest || block == Blocks.trapped_chest) {
            return YES;
        }
        if (block == Blocks.glass || block == Blocks.stained_glass) {
            return YES;
        }
        if (block instanceof BlockStairs) {
            return YES;
        }
        if (isWater(block)) {
            return MAYBE;
        }
        if (MovementHelper.isLava(block) && Baritone.settings().assumeWalkOnLava.value) {
            return MAYBE;
        }
        if (block instanceof BlockSlab) {
            if (!Baritone.settings().allowWalkOnBottomSlab.value) {
                if (((BlockSlab) block).isDouble()) {
                    return YES;
                }
                if (state.getValue(BlockSlab.HALF) != BlockSlab.EnumBlockHalf.BOTTOM) {
                    return YES;
                }
                return NO;
            }
            return YES;
        }
        return NO;
    }

    static boolean canWalkOnPosition(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();
        if (isWater(block)) {
            Block up = bsi.get0(x, y + 1, z).getBlock();
            if (up == Blocks.waterlily || up == Blocks.carpet) {
                return true;
            }
            if (MovementHelper.isFlowing(x, y, z, state, bsi) || block == Blocks.flowing_water) {
                return isWater(up) && !Baritone.settings().assumeWalkOnWater.value;
            }
            return isWater(up) ^ Baritone.settings().assumeWalkOnWater.value;
        }

        return MovementHelper.isLava(block) && !MovementHelper.isFlowing(x, y, z, state, bsi) && Baritone.settings().assumeWalkOnLava.value;
    }

    static boolean canWalkOn(CalculationContext context, int x, int y, int z, IBlockState state) {
        return context.precomputedData.canWalkOn(context.bsi, x, y, z, state);
    }

    static boolean canWalkOn(CalculationContext context, int x, int y, int z) {
        return canWalkOn(context, x, y, z, context.get(x, y, z));
    }

    static boolean canWalkOn(IPlayerContext ctx, BetterBlockPos pos, IBlockState state) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z, state);
    }

    static boolean canWalkOn(IPlayerContext ctx, BlockPos pos) {
        return canWalkOn(new BlockStateInterface(ctx), pos.getX(), pos.getY(), pos.getZ());
    }

    static boolean canWalkOn(IPlayerContext ctx, BetterBlockPos pos) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkOn(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean mustBeSolidToWalkOn(CalculationContext context, int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.ladder || block == Blocks.vine) {
            return false;
        }
        if (block instanceof BlockLiquid) {
            if (context.assumeWalkOnWater) {
                return false;
            }
            Block blockAbove = context.getBlock(x, y + 1, z);
            return !(blockAbove instanceof BlockLiquid);
        }
        return true;
    }


    static boolean isBottomSlab(IBlockState state) {
        return state.getBlock() instanceof BlockSlab
                && !((BlockSlab) state.getBlock()).isDouble()
                && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
    }

    static void moveTowards(IPlayerContext ctx, MovementState state, BlockPos pos) {
        state.setTarget(new MovementState.MovementTarget(
                RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                        VecUtils.getBlockPosCenter(pos),
                        ctx.playerRotations()).withPitch(ctx.playerRotations().getPitch()),
                false
        )).setInput(Input.MOVE_FORWARD, true);
    }

    static void rotate(IPlayerContext ctx, MovementState state, Vec3 dest) {
        state.setTarget(new MovementState.MovementTarget(
                RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                        dest,
                        ctx.playerRotations()).withPitch(ctx.playerRotations().getPitch()),
                false
        ));
    }

    static void rotate(IPlayerContext ctx, MovementState state, BetterBlockPos src, Vec3 dest) {
        state.setTarget(new MovementState.MovementTarget(
                RotationUtils.calcRotationFromVec3d(VecUtils.getBlockPosCenter(src),
                        dest,
                        ctx.playerRotations()).withPitch(ctx.playerRotations().getPitch()),
                false
        ));
    }

    static void setInputs(IPlayerContext ctx, MovementState state, Vec3 dest) {

        PlayerSimulation simulation = new PlayerSimulation(ctx.world());

        HorizontalMovements[] movements = HorizontalMovements.values();

        Tuple<HorizontalMovements, Double> closest = null;

        for (HorizontalMovements movement : movements) {
            if (!Baritone.settings().allowSprint.value && Arrays.asList(movement.getKeys()).contains(Input.SPRINT)) {
                continue;
            }

            simulation.copy(ctx.player(), ctx);
            Vec3 prediction = movement.pred(simulation, ctx);

            double distance = prediction.distanceTo(dest);

            if (closest == null || distance < closest.getSecond()) {
                closest = new Tuple<>(movement, distance);
            }
        }

        if (closest != null) {
            for (Input input : closest.getFirst().getKeys()) {
                state.setInput(input, true);
            }
        }
    }

    static void setInputsAccurate(IPlayerContext ctx, MovementState state, Vec3 dest) {
        PlayerSimulation simulation = new PlayerSimulation(ctx.world());

        HorizontalMovements[] movements = HorizontalMovements.values();

        Tuple<HorizontalMovements, Double> closest = null;

        for (HorizontalMovements movement : movements) {
            if (!Baritone.settings().allowSprint.value && Arrays.asList(movement.getKeys()).contains(Input.SPRINT)) {
                continue;
            }

            for (HorizontalMovements nextMovement : movements) {
                if (!Baritone.settings().allowSprint.value && Arrays.asList(nextMovement.getKeys()).contains(Input.SPRINT)) {
                    continue;
                }

                simulation.copy(ctx.player(), ctx);
                movement.pred(simulation, ctx);
                Vec3 prediction = nextMovement.pred(simulation, ctx);

                double distance = prediction.distanceTo(dest);

                if (closest == null || distance < closest.getSecond()) {
                    closest = new Tuple<>(movement, distance);
                }
            }
        }

        if (closest != null) {
            for (Input input : closest.getFirst().getKeys()) {
                state.setInput(input, true);
            }
        }
    }

    static void decreaseMotion(MovementState state, IPlayerContext ctx) {
        PlayerSimulation simulation = new PlayerSimulation(ctx.world());

        HorizontalMovements[] movements = HorizontalMovements.values();

        Tuple<HorizontalMovements, Double> closest = null;

        for (HorizontalMovements movement : movements) {
            if (!Baritone.settings().allowSprint.value && Arrays.asList(movement.getKeys()).contains(Input.SPRINT)) {
                continue;
            }

            simulation.copy(ctx.player(), ctx);
            movement.pred(simulation, ctx);

            double distance = simulation.motionX * simulation.motionX + simulation.motionZ * simulation.motionZ;

            if (closest == null || distance < closest.getSecond()) {
                closest = new Tuple<>(movement, distance);
            }
        }

        if (closest != null) {
            for (Input input : closest.getFirst().getKeys()) {
                state.setInput(input, true);
            }
        }
    }

    static void decreaseMotion(InputOverrideHandler handler, IPlayerContext ctx) {
        PlayerSimulation simulation = new PlayerSimulation(ctx.world());

        HorizontalMovements[] movements = HorizontalMovements.values();

        Tuple<HorizontalMovements, Double> closest = null;

        for (HorizontalMovements movement : movements) {
            if (!Baritone.settings().allowSprint.value && Arrays.asList(movement.getKeys()).contains(Input.SPRINT)) {
                continue;
            }

            simulation.copy(ctx.player(), ctx);
            movement.pred(simulation, ctx);

            double distance = simulation.motionX * simulation.motionX + simulation.motionZ * simulation.motionZ;

            if (closest == null || distance < closest.getSecond()) {
                closest = new Tuple<>(movement, distance);
            }
        }

        if (closest != null) {
            for (Input input : closest.getFirst().getKeys()) {
                handler.setInputForceState(input, true);
            }
        }
    }

    static boolean isWater(Block b) {
        return b == Blocks.flowing_water || b == Blocks.water;
    }

    static boolean isWater(IPlayerContext ctx, BlockPos bp) {
        return isWater(BlockStateInterface.getBlock(ctx, bp));
    }

    static boolean isLava(Block b) {
        return b == Blocks.flowing_lava || b == Blocks.lava;
    }

    static boolean isLiquid(IPlayerContext ctx, BlockPos p) {
        return BlockStateInterface.getBlock(ctx, p) instanceof BlockLiquid;
    }

    static boolean possiblyFlowing(IBlockState state) {
        return state.getBlock() instanceof BlockLiquid
                && state.getValue(BlockLiquid.LEVEL) != 0;
    }

    static boolean isFlowing(int x, int y, int z, IBlockState state, BlockStateInterface bsi) {
        if (!(state.getBlock() instanceof BlockLiquid)) {
            return false;
        }
        if (state.getValue(BlockLiquid.LEVEL) != 0) {
            return true;
        }
        return possiblyFlowing(bsi.get0(x + 1, y, z))
                || possiblyFlowing(bsi.get0(x - 1, y, z))
                || possiblyFlowing(bsi.get0(x, y, z + 1))
                || possiblyFlowing(bsi.get0(x, y, z - 1));
    }


    static boolean isTransparent(Block b) {

        return b == Blocks.air ||
                b == Blocks.flowing_lava ||
                b == Blocks.flowing_water ||
                b == Blocks.water;
    }
}
