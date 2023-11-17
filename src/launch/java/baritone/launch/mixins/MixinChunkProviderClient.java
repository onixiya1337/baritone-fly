package baritone.launch.mixins;

import baritone.api.utils.ChunkPos;
import baritone.api.utils.Helper;
import baritone.utils.accessor.IChunkProviderClient;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ChunkProviderClient.class)
public class MixinChunkProviderClient implements IChunkProviderClient {

    @Unique
    private Long2ObjectMap<Chunk> map;

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void onInit(World world, CallbackInfo ci) {
        this.map = new Long2ObjectOpenHashMap<>();
    }

    @Inject(
            method = "loadChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/LongHashMap;add(JLjava/lang/Object;)V"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void loadChunk(int chunkX, int chunkZ, CallbackInfoReturnable<Chunk> cir, Chunk chunk) {
        map.put(ChunkPos.asLong(chunkX, chunkZ), chunk);
    }

    @Inject(
            method = "unloadChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/LongHashMap;remove(J)Ljava/lang/Object;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void remove(int chunkX, int chunkZ, CallbackInfo ci, Chunk chunk) {
        map.remove(ChunkPos.asLong(chunkX, chunkZ));
    }

    @Override
    public Long2ObjectMap<Chunk> loadedChunks() {
        return map;
    }
}
