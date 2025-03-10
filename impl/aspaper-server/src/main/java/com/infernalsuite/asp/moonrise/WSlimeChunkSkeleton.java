package com.infernalsuite.asp.moonrise;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.List;
import java.util.Objects;

public final class WSlimeChunkSkeleton implements WriteableSlimeChunk {
    private final int x;
    private final int z;
    private SlimeChunkSection[] sections;
    private CompoundBinaryTag heightMap;
    private List<CompoundBinaryTag> blockEntities;
    private List<CompoundBinaryTag> entities;
    private final CompoundBinaryTag extra;
    private final CompoundBinaryTag upgradeData;

    public WSlimeChunkSkeleton(int x, int z, SlimeChunkSection[] sections,
                               CompoundBinaryTag heightMap,
                               List<CompoundBinaryTag> blockEntities,
                               List<CompoundBinaryTag> entities,
                               CompoundBinaryTag extra,
                               CompoundBinaryTag upgradeData) {
        this.x = x;
        this.z = z;
        this.sections = sections;
        this.heightMap = heightMap;
        this.blockEntities = blockEntities;
        this.entities = entities;
        this.extra = extra;
        this.upgradeData = upgradeData;
    }

    public WSlimeChunkSkeleton(SlimeChunk chunk) {
        this(
                chunk.getX(),
                chunk.getZ(),
                chunk.getSections(),
                chunk.getHeightMaps(),
                chunk.getTileEntities(),
                chunk.getEntities(),
                chunk.getExtraData(),
                chunk.getUpgradeData()
        );
    }

    @Override
    public void setEntities(List<CompoundBinaryTag> objects) {
        this.entities = objects;
    }

    @Override
    public void setSections(SlimeChunkSection[] sections) {
        this.sections = sections;
    }

    @Override
    public void setTileEntities(List<CompoundBinaryTag> objects) {
        this.blockEntities = objects;
    }

    @Override
    public void setHeightmaps(CompoundBinaryTag heightmaps) {
        this.heightMap = heightmaps;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getZ() {
        return this.z;
    }

    @Override
    public SlimeChunkSection[] getSections() {
        return this.sections;
    }

    @Override
    public CompoundBinaryTag getHeightMaps() {
        return this.heightMap;
    }

    @Override
    public List<CompoundBinaryTag> getTileEntities() {
        return this.blockEntities;
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        return this.entities;
    }

    @Override
    public CompoundBinaryTag getExtraData() {
        return this.extra;
    }

    @Override
    public CompoundBinaryTag getUpgradeData() {
        return this.upgradeData;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public SlimeChunkSection[] sections() {
        return sections;
    }

    public CompoundBinaryTag heightMap() {
        return heightMap;
    }

    public List<CompoundBinaryTag> blockEntities() {
        return blockEntities;
    }

    public List<CompoundBinaryTag> entities() {
        return entities;
    }

    public CompoundBinaryTag extra() {
        return extra;
    }

    public CompoundBinaryTag upgradeData() {
        return upgradeData;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WSlimeChunkSkeleton) obj;
        return this.x == that.x &&
                this.z == that.z &&
                Objects.equals(this.sections, that.sections) &&
                Objects.equals(this.heightMap, that.heightMap) &&
                Objects.equals(this.blockEntities, that.blockEntities) &&
                Objects.equals(this.entities, that.entities) &&
                Objects.equals(this.extra, that.extra) &&
                Objects.equals(this.upgradeData, that.upgradeData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, sections, heightMap, blockEntities, entities, extra, upgradeData);
    }

    @Override
    public String toString() {
        return "WSlimeChunkSkeleton[" +
                "x=" + x + ", " +
                "z=" + z + ", " +
                "sections=" + sections + ", " +
                "heightMap=" + heightMap + ", " +
                "blockEntities=" + blockEntities + ", " +
                "entities=" + entities + ", " +
                "extra=" + extra + ", " +
                "upgradeData=" + upgradeData + ']';
    }

}
