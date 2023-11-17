package baritone.launch.mixins;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.BlockChangeEvent;
import baritone.api.event.events.ChunkEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.utils.ChunkPos;
import baritone.api.utils.Pair;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Shadow
    private Minecraft gameController;

    @Shadow
    private WorldClient clientWorldController;

    @Inject(
            method = "handleChunkData",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/chunk/Chunk.fillChunk([BIZ)V"
            )
    )
    private void preRead(S21PacketChunkData packetIn, CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForConnection((NetHandlerPlayClient) (Object) this);
        if (baritone == null) {
            return;
        }
        baritone.getGameEventHandler().onChunkEvent(
                new ChunkEvent(
                        EventState.PRE,
                        packetIn.func_149274_i() ? ChunkEvent.Type.POPULATE_FULL : ChunkEvent.Type.POPULATE_PARTIAL,
                        packetIn.getChunkX(),
                        packetIn.getChunkZ()
                )
        );
    }

    @Inject(
            method = "handleChunkData",
            at = @At("TAIL")
    )
    private void postHandleChunkData(S21PacketChunkData packetIn, CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForConnection((NetHandlerPlayClient) (Object) this);
        if (baritone == null) {
            return;
        }
        baritone.getGameEventHandler().onChunkEvent(
                new ChunkEvent(
                        EventState.POST,
                        packetIn.func_149274_i() ? ChunkEvent.Type.POPULATE_FULL : ChunkEvent.Type.POPULATE_PARTIAL,
                        packetIn.getChunkX(),
                        packetIn.getChunkZ()
                )
        );
    }

    @Inject(
            method = "handleMapChunkBulk",
            at = @At("HEAD"),
            cancellable = true
    )
    private void handleMapChunkBulk(S26PacketMapChunkBulk packetIn, CallbackInfo ci) {
        NetHandlerPlayClient connection = (NetHandlerPlayClient) (Object) this;
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForConnection(connection);
        if (baritone == null) {
            return;
        }

        PacketThreadUtil.checkThreadAndEnqueue(packetIn, connection, this.gameController);

        for(int i = 0; i < packetIn.getChunkCount(); ++i) {
            int chunkX = packetIn.getChunkX(i);
            int chunkZ = packetIn.getChunkZ(i);
            this.clientWorldController.doPreChunk(chunkX, chunkZ, true);
            this.clientWorldController.invalidateBlockReceiveRegion(chunkX << 4, 0, chunkZ << 4, (chunkX << 4) + 15, 256, (chunkZ << 4) + 15);
            Chunk chunk = this.clientWorldController.getChunkFromChunkCoords(chunkX, chunkZ);

            baritone.getGameEventHandler().onChunkEvent(
                    new ChunkEvent(
                            EventState.PRE,
                            ChunkEvent.Type.POPULATE_FULL,
                            chunkX,
                            chunkZ
                    )
            );
            chunk.fillChunk(packetIn.getChunkBytes(i), packetIn.getChunkSize(i), true);
            this.clientWorldController.markBlockRangeForRenderUpdate(chunkX << 4, 0, chunkZ << 4, (chunkX << 4) + 15, 256, (chunkZ << 4) + 15);
            if (!(this.clientWorldController.provider instanceof WorldProviderSurface)) {
                chunk.resetRelightChecks();
            }

            baritone.getGameEventHandler().onChunkEvent(
                    new ChunkEvent(
                            EventState.POST,
                            ChunkEvent.Type.POPULATE_FULL,
                            chunkX,
                            chunkZ
                    )
            );
        }

        ci.cancel();
    }

    @Inject(
            method = "handleBlockChange",
            at = @At("RETURN")
    )
    private void postHandleBlockChange(S23PacketBlockChange packetIn, CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForConnection((NetHandlerPlayClient) (Object) this);
        if (baritone == null) {
            return;
        }

        ChunkPos pos = new ChunkPos(packetIn.getBlockPosition().getX() >> 4, packetIn.getBlockPosition().getZ() >> 4);
        Pair<BlockPos, IBlockState> changed = new Pair<>(packetIn.getBlockPosition(), packetIn.getBlockState());
        baritone.getGameEventHandler().onBlockChange(new BlockChangeEvent(pos, Collections.singletonList(changed)));
    }

    @Inject(
            method = "handleMultiBlockChange",
            at = @At("RETURN")
    )
    private void postHandleMultiBlockChange(S22PacketMultiBlockChange packetIn, CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForConnection((NetHandlerPlayClient) (Object) this);
        if (baritone == null) {
            return;
        }

        ChunkPos pos = new ChunkPos(packetIn.getChangedBlocks()[0].getPos());

        List<Pair<BlockPos, IBlockState>> changed = new ArrayList<>();
        for (S22PacketMultiBlockChange.BlockUpdateData updateData : packetIn.getChangedBlocks()) {
            changed.add(new Pair<>(updateData.getPos(), updateData.getBlockState()));
        }

        baritone.getGameEventHandler().onBlockChange(new BlockChangeEvent(pos, changed));
    }
}
