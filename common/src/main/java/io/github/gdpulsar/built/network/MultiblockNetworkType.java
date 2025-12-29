package io.github.gdpulsar.built.network;

import io.github.gdpulsar.built.Built;
import io.github.gdpulsar.built.block.NetworkedMultiblock;
import io.github.gdpulsar.built.block.NetworkedMultiblockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Function;

public class MultiblockNetworkType<T extends MultiblockNetwork> {
    private final ResourceLocation id;

    private final HashMap<Level, HashMap<Integer, T>> networks;
    private final Function<Integer, T> networkConstructor;

    public MultiblockNetworkType(ResourceLocation id, Function<Integer, T> constructor) {
        this.id = id;

        this.networks = new HashMap<>();
        this.networkConstructor = constructor;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    private HashMap<Integer, T> getLevelNetworks(Level level) {
        if (!networks.containsKey(level)) {
            networks.put(level, new HashMap<>());
        }
        if (!networks.get(level).isEmpty()) {
            for (Integer id : Set.copyOf(networks.get(level).keySet())) {
                if (networks.get(level).get(id).getConnectedBlocks().isEmpty()) {
                    networks.get(level).remove(id);
                }
            }
        }
        return networks.get(level);
    }

    public T createNetwork(Level level) {
        HashMap<Integer, T> levelNetworks = getLevelNetworks(level);
        int id = 0;
        while (levelNetworks.containsKey(id)) {
            id++;
        }
        T network = networkConstructor.apply(id);
        network.init(level, this);
        levelNetworks.put(id, network);
        return network;
    }

    private void getConnected(Level level, BlockPos pos, Set<BlockPos> connected) {
        for (Direction direction : Direction.values()) {
            BlockPos offset = pos.relative(direction);
            if (!connected.contains(offset)) {
                if (level.getBlockState(pos).is(level.getBlockState(offset).getBlock()) && level.getBlockState(pos).getBlock() instanceof NetworkedMultiblock<?>) {
                    connected.add(offset);
                    getConnected(level, offset, connected);
                }
            }
        }
    }

    public void attemptSplit(Level level, MultiblockNetwork network, BlockPos pos) {
        List<BlockPos> checked = new ArrayList<>();
        List<Set<BlockPos>> newNetworks = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos offset = pos.relative(direction);
            if (checked.contains(offset)) {
                continue;
            }
            Set<BlockPos> connected = new HashSet<>(List.of(offset));
            getConnected(level, offset, connected);
            if (!connected.isEmpty()) {
                newNetworks.add(connected);
                checked.addAll(connected);
            }
        }
        if (newNetworks.size() > 1) {
            getLevelNetworks(level).remove(network.getId());
            List<T> networks = new ArrayList<>();
            for (Set<BlockPos> networkBlocks : newNetworks) {
                T newNetwork = this.createNetwork(level);
                for (BlockPos blockPos : networkBlocks) {
                    if (level.getBlockEntity(blockPos) instanceof NetworkedMultiblockEntity<? extends MultiblockNetwork> networkedMultiblock) {
                        networkedMultiblock.setNetwork(newNetwork);
                    }
                }
                if (!newNetwork.getConnectedBlocks().isEmpty()) {
                    networks.add(newNetwork);
                }
            }
            network.splitAcross(networks);
        }
    }

    public void attemptMerge(Level level, BlockPos pos) {
        List<MultiblockNetwork> merging = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos offset = pos.relative(direction);
            if (level.getBlockEntity(offset) instanceof NetworkedMultiblockEntity<?> networkEntity) {
                if (networkEntity.hasNetwork() && networkEntity.getNetworkType() == this) {
                    if (!merging.contains(networkEntity.getOrCreateNetwork())) {
                        merging.add(networkEntity.getOrCreateNetwork());
                    }
                }
            }
        }
        if (merging.size() > 1) {
            T merged = this.createNetwork(level);
            if (level.getBlockEntity(pos) instanceof NetworkedMultiblockEntity<?> networkEntity) {
                networkEntity.setNetwork(merged);
            }
            for (MultiblockNetwork network : merging) {
                for (BlockPos connected : network.getConnectedBlocks()) {
                    if (level.getBlockEntity(connected) instanceof NetworkedMultiblockEntity<?> networkEntity) {
                        networkEntity.setNetwork(merged);
                    }
                }
                getLevelNetworks(level).remove(network.getId());
            }
            merged.mergeWith(merging);
            getLevelNetworks(level).put(merged.getId(), merged);
        }
    }

    public T getNetworkById(Level level, int id) {
        return getLevelNetworks(level).get(id);
    }

    public ListTag saveToNbt(ServerLevel level) {
        ListTag networksList = new ListTag();
        for (Map.Entry<Integer, T> entry : getLevelNetworks(level).entrySet()) {
            CompoundTag networkTag = new CompoundTag();
            networkTag.putInt("id", entry.getKey());
            entry.getValue().saveToNbt(networkTag);
            networksList.add(networkTag);
        }
        return networksList;
    }

    public void readFromNbt(ServerLevel serverLevel, ListTag networksList) {
        for (Tag tag : networksList) {
            if (tag instanceof CompoundTag networkTag) {
                int id = networkTag.getInt("id");
                T network = this.networkConstructor.apply(id);
                network.init(serverLevel, this);
                network.readFromNbt(networkTag);
                getLevelNetworks(serverLevel).put(id, network);
            }
        }
    }
}
