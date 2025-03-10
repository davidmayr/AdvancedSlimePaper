package com.infernalsuite.asp.moonrise;


import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO;
import ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.ChunkDataController;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.serialization.slime.ChunkPruner;
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlimeRegionDataController extends ChunkDataController {

    private WritableSlimeWorld slimeWorld;

    public SlimeRegionDataController(ServerLevel world, ChunkTaskScheduler taskScheduler, WritableSlimeWorld slimeWorld) {
        super(world, taskScheduler);
        this.slimeWorld = slimeWorld;
    }

    @Override
    public WriteData startWrite(int chunkX, int chunkZ, CompoundTag compound) {
        if(ChunkPruner.shouldPruneAtAllCost(slimeWorld, chunkX, chunkZ)) {
            return new WriteData(compound, WriteData.WriteResult.DELETE, null, null);
        }
        return new WriteData(compound, WriteData.WriteResult.WRITE, null, null);
    }

    @Override
    public void finishWrite(int chunkX, int chunkZ, WriteData writeData) {
        if(writeData.result() == WriteData.WriteResult.DELETE) {
            //The only delete case is forced prune so this is fine.
            slimeWorld.deleteChunk(chunkX, chunkZ);
            return;
        }
        WriteableSlimeChunk chunk = slimeWorld.getOrCreateChunk(chunkX, chunkZ);

        ListTag sections = writeData.input().getList("sections", Tag.TAG_COMPOUND);
        SlimeChunkSectionSkeleton[] sectionSkeletons = new SlimeChunkSectionSkeleton[sections.size()];

        for (int i = 0; i < sections.size(); i++) {
            CompoundTag sectionTag = sections.getCompound(i);

            int lightSIze = 2048;

            NibbleArray skyLight;
            if(sectionTag.contains("SkyLight") && sectionTag.getByteArray("SkyLight").length == lightSIze) {
                skyLight = new NibbleArray(sectionTag.getByteArray("SkyLight"));
            } else {
                skyLight = new NibbleArray(2048);
            }

            NibbleArray blockLight;
            if(sectionTag.contains("BlockLight") && sectionTag.getByteArray("BlockLight").length == lightSIze) {
                blockLight = new NibbleArray(sectionTag.getByteArray("BlockLight"));
            } else {
                blockLight = new NibbleArray(2048);
            }

            try {
                System.out.println(TagStringIO.get().asString(Converter.convertTag(sectionTag.getCompound("block_states"))));
                System.out.println(TagStringIO.get().asString(Converter.convertTag(sectionTag.getCompound("biomes"))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            sectionSkeletons[i] = new SlimeChunkSectionSkeleton(
                    Converter.convertTag(sectionTag.getCompound("block_states")),
                    Converter.convertTag(sectionTag.getCompound("biomes")),
                    blockLight,
                    skyLight
            );
        }

        chunk.setSections(sectionSkeletons);
        chunk.setHeightmaps(Converter.convertTag(writeData.input().getCompound("Heightmaps")));

        ListTag blockEntitiesMC = writeData.input().getList("block_entities", Tag.TAG_COMPOUND);
        List<CompoundBinaryTag> blockEntities = new ArrayList<>(blockEntitiesMC.size());
        for (Tag tag : blockEntitiesMC) {
            blockEntities.add(Converter.convertTag(tag));
        }
        chunk.setTileEntities(blockEntities);

        //TODO: Upgrade data

        if(ChunkPruner.canBePruned(slimeWorld, chunk)) {
            slimeWorld.deleteChunk(chunkX, chunkZ);
        }
    }

    @Override
    public ReadData readData(int chunkX, int chunkZ) {
        SlimeChunk chunk = slimeWorld.getChunk(chunkX, chunkZ);
        if(chunk == null) return new ReadData(ReadData.ReadResult.NO_DATA, null, null);

        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", slimeWorld.getDataVersion());
        tag.putString("Status", "minecraft:full");
        tag.putInt("xPos", chunkX);
        tag.putInt("zPos", chunkZ);

        ListTag sections = new ListTag();
        int yStart = -5;
        for (SlimeChunkSection section : chunk.getSections()) {
            CompoundTag sectionTag = new CompoundTag();
            sectionTag.put("block_states", Converter.convertTag(section.getBlockStatesTag()));
            sectionTag.put("biomes", Converter.convertTag(section.getBiomeTag()));
            sectionTag.putInt("Y", yStart);
            if(section.getSkyLight() != null) {
                sectionTag.putByteArray("SkyLight", section.getSkyLight().getBacking());
            }
            if(section.getBlockLight() != null) {
                sectionTag.putByteArray("BlockLight", section.getBlockLight().getBacking());
            }
            sections.add(sectionTag);
            yStart+=1;
        }
        tag.put("sections", sections);
        tag.put("Heightmaps", Converter.convertTag(chunk.getHeightMaps()));

        ListTag blockEntities = new ListTag();
        for (CompoundBinaryTag blockEntity : chunk.getTileEntities()) {
            blockEntities.add(Converter.convertTag(blockEntity));
        }
        tag.put("block_entities", blockEntities);

        return new ReadData(ReadData.ReadResult.HAS_DATA, null, tag);
    }

    @Override
    public CompoundTag finishRead(int chunkX, int chunkZ, ReadData readData) throws IOException {
        return readData.syncRead();
    }
}
