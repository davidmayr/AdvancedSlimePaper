package com.infernalsuite.asp.moonrise;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeWorld;

import java.util.Collection;

public interface WritableSlimeWorld extends SlimeWorld {

    WriteableSlimeChunk getOrCreateChunk(int chunkX, int chunkZ);

    @Override
    WriteableSlimeChunk getChunk(int x, int z);

    void deleteChunk(int chunkX, int chunkZ);


    @Override
    Collection<SlimeChunk> getChunkStorage();
}
