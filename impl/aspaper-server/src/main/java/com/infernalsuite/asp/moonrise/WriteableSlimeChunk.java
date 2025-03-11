package com.infernalsuite.asp.moonrise;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.List;

public interface WriteableSlimeChunk extends SlimeChunk {
    void setEntities(List<CompoundBinaryTag> objects);
    void setSections(SlimeChunkSection[] sections);
    void setTileEntities(List<CompoundBinaryTag> objects);
    void setHeightmaps(CompoundBinaryTag heightmaps);

    void setHasSectionData(boolean state);
    boolean hasSectionData();
}
