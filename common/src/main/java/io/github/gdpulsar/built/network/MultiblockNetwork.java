package io.github.gdpulsar.built.network;

import io.github.gdpulsar.built.block.NetworkedMultiblockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public abstract class MultiblockNetwork {
    private final int networkId;
    private final List<BlockPos> connectedBlocks;
    private Level level;
    private MultiblockNetworkType<?> networkType;

    public MultiblockNetwork(int networkId) {
        this.networkId = networkId;
        this.connectedBlocks = new ArrayList<>();
    }

    public void init(Level level, MultiblockNetworkType<?> networkType) {
        this.level = level;
        this.networkType = networkType;
    }

    public void addToNetwork(NetworkedMultiblockEntity<? extends MultiblockNetwork> blockEntity) {
        this.connectedBlocks.add(blockEntity.getBlockPos());
    }

    public void removeFromNetwork(NetworkedMultiblockEntity<? extends MultiblockNetwork> blockEntity) {
        this.connectedBlocks.remove(blockEntity.getBlockPos());
    }

    public abstract void splitAcross(List<? extends MultiblockNetwork> networks);
    public abstract void mergeWith(List<? extends MultiblockNetwork> networks);

    public final Level getLevel() {
        return this.level;
    }

    public final int getId() {
        return this.networkId;
    }

    public final List<BlockPos> getConnectedBlocks() {
        return this.connectedBlocks;
    }

    public final MultiblockNetworkType<?> getNetworkType() {
        return this.networkType;
    }

    public CompoundTag saveToNbt(CompoundTag tag) {
        ListTag connectedList = new ListTag();
        for (BlockPos pos : this.connectedBlocks) {
            connectedList.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("connected", connectedList);
        return tag;
    }

    public void readFromNbt(CompoundTag tag) {
        ListTag connectedList = tag.getList("connected", Tag.TAG_INT_ARRAY);
        for (Tag posTag : connectedList) {
            if (posTag instanceof IntArrayTag intArrayTag) {
                int[] pos = intArrayTag.getAsIntArray();
                this.connectedBlocks.add(new BlockPos(pos[0], pos[1], pos[2]));
            }
        }
    }
}
