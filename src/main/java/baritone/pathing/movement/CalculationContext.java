package baritone.pathing.movement;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.ActionCosts;
import baritone.pathing.precompute.PrecomputedData;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class CalculationContext {

    public final boolean safeForThreadedUse;
    public final IBaritone baritone;
    public final World world;
    public final BlockStateInterface bsi;
    public final boolean canSprint;
    public final boolean assumeWalkOnWater;
    public final boolean allowDownward;
    public final int maxFallHeightNoWater;
    public final double waterWalkSpeed;
    public final double backtrackCostFavoringCoefficient;
    public final double jumpPenalty;
    public final double walkOnWaterOnePenalty;


    public final PrecomputedData precomputedData;

    public CalculationContext(IBaritone baritone) {
        this(baritone, false);
    }

    public CalculationContext(IBaritone baritone, boolean forUseOnAnotherThread) {
        this.precomputedData = new PrecomputedData();
        this.safeForThreadedUse = forUseOnAnotherThread;
        this.baritone = baritone;
        EntityPlayerSP player = baritone.getPlayerContext().player();
        this.world = baritone.getPlayerContext().world();
        this.bsi = new BlockStateInterface(baritone.getPlayerContext(), forUseOnAnotherThread);
        this.canSprint = Baritone.settings().allowSprint.value && player.getFoodStats().getFoodLevel() > 6;
        this.assumeWalkOnWater = Baritone.settings().assumeWalkOnWater.value;
        this.allowDownward = Baritone.settings().allowDownward.value;
        this.maxFallHeightNoWater = Baritone.settings().maxFallHeightNoWater.value;
        int depth = EnchantmentHelper.getDepthStriderModifier(player);
        if (depth > 3) {
            depth = 3;
        }
        float mult = depth / 3.0F;
        this.waterWalkSpeed = ActionCosts.WALK_ONE_IN_WATER_COST * (1 - mult) + ActionCosts.WALK_ONE_BLOCK_COST * mult;
        this.backtrackCostFavoringCoefficient = Baritone.settings().backtrackCostFavoringCoefficient.value;
        this.jumpPenalty = Baritone.settings().jumpPenalty.value;
        this.walkOnWaterOnePenalty = Baritone.settings().walkOnWaterOnePenalty.value;
    }

    public final IBaritone getBaritone() {
        return baritone;
    }

    public IBlockState get(int x, int y, int z) {
        return bsi.get0(x, y, z);
    }

    public boolean isLoaded(int x, int z) {
        return bsi.isLoaded(x, z);
    }

    public IBlockState get(BlockPos pos) {
        return get(pos.getX(), pos.getY(), pos.getZ());
    }

    public Block getBlock(int x, int y, int z) {
        return get(x, y, z).getBlock();
    }

    public boolean isPossiblyProtected(int x, int y, int z) {
        return false;
    }
}