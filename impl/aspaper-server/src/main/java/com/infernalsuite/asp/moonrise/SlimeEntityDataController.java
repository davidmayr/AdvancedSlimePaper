package com.infernalsuite.asp.moonrise;

import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO;
import ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.EntityDataController;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.serialization.slime.ChunkPruner;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlimeEntityDataController extends EntityDataController {

    private WritableSlimeWorld slimeWorld;

    public SlimeEntityDataController(EntityRegionFileStorage storage, ChunkTaskScheduler taskScheduler, WritableSlimeWorld slimeWorld) {
        super(storage, taskScheduler);
        this.slimeWorld = slimeWorld;
    }

    @Override
    public WriteData startWrite(int chunkX, int chunkZ, CompoundTag compound) {
        if(ChunkPruner.shouldPruneAtAllCost(slimeWorld, chunkX, chunkZ) || compound.getList("Entities", Tag.TAG_COMPOUND).isEmpty()) {
            return new WriteData(null, WriteData.WriteResult.DELETE, null, null);
        }

        return new WriteData(compound, WriteData.WriteResult.WRITE, null, null);
    }

    @Override
    public void finishWrite(int chunkX, int chunkZ, WriteData writeData) {
        if(writeData.result() == WriteData.WriteResult.DELETE) {
            WriteableSlimeChunk slimeChunk = slimeWorld.getChunk(chunkX, chunkZ);
            if(slimeChunk == null) return;
            slimeChunk.setEntities(Collections.emptyList());

            if(ChunkPruner.canBePruned(slimeWorld, slimeChunk)) {
                slimeWorld.deleteChunk(chunkX, chunkZ);
            }
            return;
        }
        WriteableSlimeChunk chunk = slimeWorld.getOrCreateChunk(chunkX, chunkZ);
        ListTag entities = writeData.input().getList("Entities", Tag.TAG_COMPOUND);

        List<CompoundBinaryTag> entitiesConverted = new ArrayList<>(entities.size());
        for (Tag entity : entities) {
            entitiesConverted.add(Converter.convertTag(entity));
        }

        chunk.setEntities(entitiesConverted);
    }

    @Override
    public ReadData readData(int chunkX, int chunkZ) {
        SlimeChunk chunk = slimeWorld.getChunk(chunkX, chunkZ);
        if(chunk == null || true) return new ReadData(ReadData.ReadResult.NO_DATA, null, null);

        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Position", new int[]{chunkX, chunkZ});
        tag.putInt("DataVersion", slimeWorld.getDataVersion());

        ListTag listTag = new ListTag();
        for (CompoundBinaryTag entity : chunk.getEntities()) {
            listTag.add(Converter.convertTag(entity));
        }
        tag.put("Entities", listTag);

        return new ReadData(ReadData.ReadResult.HAS_DATA, null, tag);
    }

    @Override
    public CompoundTag finishRead(int chunkX, int chunkZ, ReadData readData) throws IOException {
        return null;
    }
}
