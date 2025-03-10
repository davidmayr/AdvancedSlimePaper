package com.infernalsuite.asp.moonrise;

import ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.PoiDataController;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;

public class SlimePoiDataController extends PoiDataController {

    private WritableSlimeWorld slimeWorld;

    public SlimePoiDataController(ServerLevel world, ChunkTaskScheduler taskScheduler, WritableSlimeWorld slimeWorld) {
        super(world, taskScheduler);
        this.slimeWorld = slimeWorld;
    }

    @Override
    public WriteData startWrite(int chunkX, int chunkZ, CompoundTag compound) {
        return new WriteData(compound, WriteData.WriteResult.DELETE, null, null);
    }

    @Override
    public void finishWrite(int chunkX, int chunkZ, WriteData writeData) {

    }

    @Override
    public ReadData readData(int chunkX, int chunkZ) {
        return new ReadData(ReadData.ReadResult.NO_DATA, null, null);
    }

    @Override
    public CompoundTag finishRead(int chunkX, int chunkZ, ReadData readData) throws IOException {
        return null;
    }
}
