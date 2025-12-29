package io.github.gdpulsar.built.block;

import io.github.gdpulsar.built.network.MultiblockNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public abstract class NetworkedMultiblock<T extends MultiblockNetwork> extends BaseEntityBlock {
    public NetworkedMultiblock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof NetworkedMultiblockEntity<?> networkedMultiblock) {
            if (!networkedMultiblock.attemptConnect()) {
                networkedMultiblock.getOrCreateNetwork();
            }
        }
        super.onPlace(blockState, level, blockPos, blockState2, bl);
    }

    @Override
    protected void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof NetworkedMultiblockEntity<?> networkedMultiblock) {
            if (networkedMultiblock.hasNetwork()) {
                MultiblockNetwork network = networkedMultiblock.getOrCreateNetwork();
                network.removeFromNetwork(networkedMultiblock);
                network.getNetworkType().attemptSplit(level, network, blockPos);
            }
        }
        super.onRemove(blockState, level, blockPos, blockState2, bl);
    }
}
