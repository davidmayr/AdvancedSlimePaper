package com.infernalsuite.asp.moonrise;

import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.Util;
import com.infernalsuite.asp.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.level.*;
import com.infernalsuite.asp.pdc.AdventurePersistentDataContainer;
import com.infernalsuite.asp.serialization.slime.SlimeSerializer;
import com.infernalsuite.asp.skeleton.SkeletonCloning;
import com.infernalsuite.asp.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minecraft.SharedConstants;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/*
The concept of this is a bit flawed, since ideally this should be a 1:1 representation of the MC world.
However, due to the complexity of the chunk system we essentially need to wrap around it.
This stores slime chunks, and when unloaded, will properly convert it to a normal slime chunk for storage.
 */
public class WSlimeInMemoryWorld implements WritableSlimeWorld, SlimeWorldInstance {

    private final SlimeLevelInstance instance;

    private final ConcurrentMap<String, BinaryTag> extra;
    private final AdventurePersistentDataContainer extraPDC;
    private final SlimePropertyMap propertyMap;
    private final SlimeLoader loader;

    private final Long2ObjectMap<WriteableSlimeChunk> chunkStorage = new Long2ObjectOpenHashMap<>();
    private boolean readOnly;
    // private final Map<ChunkPos, List<CompoundTag>> entityStorage = new HashMap<>();

    public WSlimeInMemoryWorld(SlimeBootstrap bootstrap, SlimeLevelInstance instance) {
        this.instance = instance;
        this.extra = bootstrap.initial().getExtraData();
        this.propertyMap = bootstrap.initial().getPropertyMap();
        this.loader = bootstrap.initial().getLoader();
        this.readOnly = bootstrap.initial().isReadOnly();

        for (SlimeChunk initial : bootstrap.initial().getChunkStorage()) {
            long pos = Util.chunkPosition(initial.getX(), initial.getZ());
            List<CompoundBinaryTag> tags = new ArrayList<>(initial.getEntities());

            //  this.entityStorage.put(pos, tags);
            this.chunkStorage.put(pos, new WSlimeChunkSkeleton(initial));
        }

        this.extraPDC = new AdventurePersistentDataContainer(this.extra);
    }

    @Override
    public String getName() {
        return this.instance.getMinecraftWorld().serverLevelData.getLevelName();
    }

    @Override
    public SlimeLoader getLoader() {
        return this.loader;
    }

    @Override
    public WriteableSlimeChunk getOrCreateChunk(int chunkX, int chunkZ) {
        if(chunkStorage.containsKey(Util.chunkPosition(chunkX, chunkZ))) {
            return getChunk(chunkX, chunkZ);
        }

        WriteableSlimeChunk chunk = new WSlimeChunkSkeleton(chunkX, chunkZ, new SlimeChunkSection[0], CompoundBinaryTag.empty(),
                Collections.emptyList(), Collections.emptyList(), CompoundBinaryTag.empty(), CompoundBinaryTag.empty());
        chunkStorage.put(Util.chunkPosition(chunkX, chunkZ), chunk);
        return chunk;
    }

    @Override
    public WriteableSlimeChunk getChunk(int x, int z) {
        return this.chunkStorage.get(Util.chunkPosition(x, z));
    }

    @Override
    public void deleteChunk(int chunkX, int chunkZ) {
        this.chunkStorage.remove(Util.chunkPosition(chunkX, chunkZ));
    }

    @Override
    public Collection<SlimeChunk> getChunkStorage() {
        return (ObjectCollection<SlimeChunk>) (Object) this.chunkStorage.values();
    }

    @Override
    public World getBukkitWorld() {
        return this.instance.getWorld();
    }

    @Override
    public SlimeWorld getSlimeWorldMirror() {
        return this;
    }

    @Override
    public SlimePropertyMap getPropertyMap() {
        return this.propertyMap;
    }

    @Override
    public boolean isReadOnly() {
        return this.getLoader() == null || this.readOnly;
    }

    @Override
    public SlimeWorld clone(String worldName) {
        try {
            return clone(worldName, null);
        } catch (WorldAlreadyExistsException | IOException ignored) {
            return null; // Never going to happen
        }
    }

    @Override
    public SlimeWorld clone(String worldName, SlimeLoader loader) throws WorldAlreadyExistsException, IOException {
        if (this.getName().equals(worldName)) {
            throw new IllegalArgumentException("The clone world cannot have the same name as the original world!");
        }

        if (worldName == null) {
            throw new IllegalArgumentException("The world name cannot be null!");
        }
        if (loader != null) {
            if (loader.worldExists(worldName)) {
                throw new WorldAlreadyExistsException(worldName);
            }
        }

        SlimeWorld cloned = SkeletonCloning.fullClone(worldName, this, loader);
        if (loader != null) {
            loader.saveWorld(worldName, SlimeSerializer.serialize(cloned));
        }

        return cloned;
    }

    @Override
    public int getDataVersion() {
        return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    }

    @Override
    public ConcurrentMap<String, BinaryTag> getExtraData() {
        return this.extra;
    }

    @Override
    public Collection<CompoundBinaryTag> getWorldMaps() {
        return List.of();
    }

    //    public Map<ChunkPos, List<CompoundTag>> getEntityStorage() {
    //        return entityStorage;
    //    }

    public SlimeLevelInstance getInstance() {
        return instance;
    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        return this.extraPDC;
    }

}
