package io.github.gdpulsar.built;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;
import io.github.gdpulsar.built.network.MultiblockNetwork;
import io.github.gdpulsar.built.network.MultiblockNetworkType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class MultiblockRegistry {
    private static final HashMap<ResourceLocation, MultiblockNetworkType<?>> networks = new HashMap<>();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static MultiblockNetworkType<?> getNetworkType(ResourceLocation id) {
        return networks.get(id);
    }

    public static <T extends MultiblockNetwork> MultiblockNetworkType<T> register(MultiblockNetworkType<T> networkType) {
        networks.put(networkType.getId(), networkType);
        return networkType;
    }

    public static void load(ServerLevel level) {
        ResourceLocation dimensionId = level.dimension().location();
        Path multiblockSavePath = level.getServer().getWorldPath(LevelResource.ROOT).resolve(
                "data/multiblocks_" + dimensionId.getNamespace() + "_" + dimensionId.getPath() + ".json");

        try {
            String json = Files.readString(multiblockSavePath);
            Tag tag = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, GsonHelper.parse(json));
            if (tag instanceof CompoundTag compoundTag) {
                for (ResourceLocation typeId : networks.keySet()) {
                    MultiblockNetworkType<?> networkType = networks.get(typeId);
                    networkType.readFromNbt(level, compoundTag.getList(typeId.toString(), Tag.TAG_COMPOUND));
                }
            }
        } catch (Exception e) {

        }
    }

    public static void save(ServerLevel level) {
        ResourceLocation dimensionId = level.dimension().location();
        Path multiblockSavePath = level.getServer().getWorldPath(LevelResource.ROOT).resolve(
                "data/multiblocks_" + dimensionId.getNamespace() + "_" + dimensionId.getPath() + ".json");

        try {
            CompoundTag tag = new CompoundTag();
            for (ResourceLocation typeId : networks.keySet()) {
                MultiblockNetworkType<?> networkType = networks.get(typeId);
                ListTag listTag = networkType.saveToNbt(level);
                tag.put(typeId.toString(), listTag);
            }
            Files.writeString(multiblockSavePath, gson.toJson(NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tag)));
        } catch (Exception e) {

        }
    }
}
