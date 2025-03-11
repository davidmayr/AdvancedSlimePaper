package com.infernalsuite.asp.moonrise;


import ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.ChunkDataController;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.serialization.slime.ChunkPruner;
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlimeRegionDataController extends ChunkDataController {

    private final WritableSlimeWorld slimeWorld;

    public SlimeRegionDataController(ServerLevel world, ChunkTaskScheduler taskScheduler, WritableSlimeWorld slimeWorld) {
        super(world, taskScheduler);
        this.slimeWorld = slimeWorld;
    }

    @Override
    public WriteData startWrite(int chunkX, int chunkZ, CompoundTag compound) {
        if(ChunkPruner.shouldPruneAtAllCost(slimeWorld, chunkX, chunkZ) || !compound.getString("Status").equals("minecraft:full")) {
            return new WriteData(compound, WriteData.WriteResult.DELETE, null, null);
        }
        return new WriteData(compound, WriteData.WriteResult.WRITE, null, null);
    }

    @Override
    public void finishWrite(int chunkX, int chunkZ, WriteData writeData) {
        long start = System.currentTimeMillis();
        synchronized (slimeWorld) {
            if(writeData.result() == WriteData.WriteResult.DELETE) {
                //The only delete case is forced prune so this is fine.
                slimeWorld.deleteChunk(chunkX, chunkZ);
                return;
            }
            WriteableSlimeChunk chunk = slimeWorld.getOrCreateChunk(chunkX, chunkZ);

            ListTag sections = writeData.input().getList("sections", Tag.TAG_COMPOUND);
            SlimeChunkSectionSkeleton[] sectionSkeletons = new SlimeChunkSectionSkeleton[sections.size()];

            boolean hasChunkData = false;
            for (int i = 0; i < sections.size(); i++) {
                CompoundTag sectionTag = sections.getCompound(i);

                NibbleArray skyLight = null;
                if(sectionTag.contains("SkyLight")) {
                    skyLight = new NibbleArray(sectionTag.getByteArray("SkyLight"));
                }

                NibbleArray blockLight = null;
                if(sectionTag.contains("BlockLight")) {
                    blockLight = new NibbleArray(sectionTag.getByteArray("BlockLight"));
                }

                CompoundTag blockStates = sectionTag.getCompound("block_states");
                if(!hasChunkData) {
                    ListTag palette = blockStates.getList("palette", Tag.TAG_COMPOUND);

                    if(palette.size() > 1 || (palette.size() == 1 && !((CompoundTag) palette.getFirst()).getString("Name").equals("minecraft:air"))) {
                        hasChunkData = true;
                    }
                }

                sectionSkeletons[i] = new SlimeChunkSectionSkeleton(
                        Converter.convertTag(blockStates),
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


            chunk.setHasSectionData(hasChunkData);

            //TODO: Upgrade data

            if(!hasChunkData && chunk.getEntities().isEmpty() && chunk.getTileEntities().isEmpty()) {
                slimeWorld.deleteChunk(chunkX, chunkZ);
            }
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
        int yStart = slimeWorld.getPropertyMap().getValue(SlimeProperties.CHUNK_SECTION_MIN);

        for (SlimeChunkSection section : chunk.getSections()) {
            CompoundTag sectionTag = new CompoundTag();
            if(section.getBlockStatesTag() != null) {
                sectionTag.put("block_states", Converter.convertTag(section.getBlockStatesTag()));
            }
            if(section.getBiomeTag() != null) {
                sectionTag.put("biomes", Converter.convertTag(section.getBiomeTag()));
            }
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
        if(chunk.getHeightMaps() != null) {
            tag.put("Heightmaps", Converter.convertTag(chunk.getHeightMaps()));
        }

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
