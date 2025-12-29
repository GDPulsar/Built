package io.github.gdpulsar.built.block;

import io.github.gdpulsar.built.Built;
import io.github.gdpulsar.built.network.MultiblockNetwork;
import io.github.gdpulsar.built.network.MultiblockNetworkType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public abstract class NetworkedMultiblockEntity<T extends MultiblockNetwork> extends BlockEntity {
    public NetworkedMultiblockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public abstract MultiblockNetworkType<T> getNetworkType();

    public boolean attemptConnect() {
        if (this.getLevel() == null) return false;
        List<MultiblockNetwork> networks = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = this.getLevel().getBlockEntity(this.getBlockPos().relative(direction));
            if (neighbour instanceof NetworkedMultiblockEntity<?> neighbourMultiblock) {
                if (neighbourMultiblock.getNetworkType() == this.getNetworkType()) {
                    if (neighbourMultiblock.hasNetwork()) {
                        networks.add(neighbourMultiblock.getOrCreateNetwork());
                    }
                }
            }
        }
        if (networks.size() == 1) {
            this.setNetwork(networks.getFirst());
        } else if (networks.size() > 1) {
            this.getNetworkType().attemptMerge(this.getLevel(), this.getBlockPos());
        }
        return false;
    }

    private int networkId = -1;

    public boolean hasNetwork() {
        return this.networkId != -1;
    }

    public T getOrCreateNetwork() {
        if (this.hasNetwork()) {
            T network = this.getNetworkType().getNetworkById(this.getLevel(), this.networkId);
            if (network != null) {
                return network;
            }
        }
        T network = getNetworkType().createNetwork(this.getLevel());
        setNetwork(network);
        return network;
    }

    public void setNetwork(MultiblockNetwork network) {
        this.networkId = network.getId();
        network.addToNetwork(this);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        this.networkId = compoundTag.getInt("networkId");
        super.loadAdditional(compoundTag, provider);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        compoundTag.putInt("networkId", this.networkId);
        super.saveAdditional(compoundTag, provider);
    }
}
