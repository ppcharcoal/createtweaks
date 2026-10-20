package com.ppcharcoal.arcticfox.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ppcharcoal.arcticfox.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

/**
 * Swaps blocks in a structure for blocks from an optional mod, only when that block exists
 * (e.g. Create tracks become Steam 'n' Rails spruce tracks). Properties with matching names are kept.
 */
public class OptionalSwapProcessor extends StructureProcessor {
    public record Rule(ResourceLocation input, ResourceLocation output) {
        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("input").forGetter(Rule::input),
                ResourceLocation.CODEC.fieldOf("output").forGetter(Rule::output)
        ).apply(i, Rule::new));
    }

    public static final MapCodec<OptionalSwapProcessor> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Rule.CODEC.listOf().fieldOf("rules").forGetter(p -> p.rules)
    ).apply(i, OptionalSwapProcessor::new));

    private final List<Rule> rules;

    public OptionalSwapProcessor(List<Rule> rules) {
        this.rules = rules;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
                                                             StructureTemplate.StructureBlockInfo original,
                                                             StructureTemplate.StructureBlockInfo current,
                                                             StructurePlaceSettings settings) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(current.state().getBlock());
        for (Rule rule : rules) {
            if (!rule.input().equals(id)) continue;
            Block target = BuiltInRegistries.BLOCK.getOptional(rule.output()).orElse(null);
            if (target == null || target == Blocks.AIR) return current;
            return new StructureTemplate.StructureBlockInfo(current.pos(), copyProperties(current.state(), target.defaultBlockState()), current.nbt());
        }
        return current;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyProperties(BlockState from, BlockState to) {
        for (Property property : from.getProperties()) {
            for (Property targetProperty : to.getProperties()) {
                if (targetProperty.getName().equals(property.getName())) {
                    var value = targetProperty.getValue(from.getValue(property).toString().toLowerCase(java.util.Locale.ROOT));
                    if (value.isPresent()) to = to.setValue(targetProperty, (Comparable) value.get());
                }
            }
        }
        return to;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModWorldgen.OPTIONAL_SWAP.get();
    }
}
