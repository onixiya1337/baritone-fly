package baritone.utils;

import baritone.Baritone;
import baritone.api.utils.ChunkPos;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.utils.accessor.IChunkProviderClient;
import baritone.utils.pathing.BetterWorldBorder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class BlockStateInterface {

    private final Long2ObjectMap<Chunk> loadedChunks;
    public final BlockPos.MutableBlockPos isPassableBlockPos;
    public final IBlockAccess access;
    public final BetterWorldBorder worldBorder;

    private Chunk prev = null;

    private final boolean useTheRealWorld;

    private static final IBlockState AIR = Blocks.air.getDefaultState();

    public BlockStateInterface(IPlayerContext ctx) {
        this(ctx, false);
    }

    public BlockStateInterface(IPlayerContext ctx, boolean copyLoadedChunks) {
        final World world = ctx.world();
        this.worldBorder = new BetterWorldBorder(world.getWorldBorder());
        Long2ObjectMap<Chunk> worldLoaded = ((IChunkProviderClient) world.getChunkProvider()).loadedChunks();
        if (copyLoadedChunks) {
            this.loadedChunks = new Long2ObjectOpenHashMap<>(worldLoaded);
        } else {
            this.loadedChunks = worldLoaded;
        }
        this.useTheRealWorld = !Baritone.settings().pathThroughCachedOnly.value;
        if (!ctx.minecraft().isCallingFromMinecraftThread()) {
            throw new IllegalStateException();
        }
        this.isPassableBlockPos = new BlockPos.MutableBlockPos();
        this.access = new BlockStateInterfaceAccessWrapper(this, world);
    }

    public boolean worldContainsLoadedChunk(int blockX, int blockZ) {
        return loadedChunks.containsKey(ChunkPos.asLong(blockX >> 4, blockZ >> 4));
    }

    public static Block getBlock(IPlayerContext ctx, BlockPos pos) {
        return get(ctx, pos).getBlock();
    }

    public static IBlockState get(IPlayerContext ctx, BlockPos pos) {
        return new BlockStateInterface(ctx).get0(pos.getX(), pos.getY(), pos.getZ()); // immense iq
    }

    public IBlockState get0(BlockPos pos) {
        return get0(pos.getX(), pos.getY(), pos.getZ());
    }

    public IBlockState get0(int x, int y, int z) {
        if (y < 0 || y >= 256) {
            return AIR;
        }

        if (useTheRealWorld) {
            Chunk cached = prev;
            if (cached != null && cached.xPosition == x >> 4 && cached.zPosition == z >> 4) {
                return cached.getBlockState(new BlockPos(x, y, z));
            }
            Chunk chunk = loadedChunks.get(ChunkPos.asLong(x >> 4, z >> 4));

            if (chunk != null && chunk.isLoaded()) {
                prev = chunk;
                return chunk.getBlockState(new BlockPos(x, y, z));
            }
        }
        return AIR;
    }

    public boolean isLoaded(int x, int z) {
        Chunk prevChunk = prev;
        if (prevChunk != null && prevChunk.xPosition == x >> 4 && prevChunk.zPosition == z >> 4) {
            return true;
        }
        prevChunk = loadedChunks.get(ChunkPos.asLong(x >> 4, z >> 4));
        if (prevChunk != null && prevChunk.isLoaded()) {
            prev = prevChunk;
            return true;
        }
        return false;
    }
}
