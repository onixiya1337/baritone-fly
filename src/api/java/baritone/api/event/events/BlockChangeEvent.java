package baritone.api.event.events;

import baritone.api.utils.ChunkPos;
import baritone.api.utils.Pair;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

import java.util.List;

public final class BlockChangeEvent {

    private final ChunkPos chunk;
    private final List<Pair<BlockPos, IBlockState>> blocks;

    public BlockChangeEvent(ChunkPos pos, List<Pair<BlockPos, IBlockState>> blocks) {
        this.chunk = pos;
        this.blocks = blocks;
    }

    public ChunkPos getChunkPos() {
        return this.chunk;
    }

    public List<Pair<BlockPos, IBlockState>> getBlocks() {
        return this.blocks;
    }
}
