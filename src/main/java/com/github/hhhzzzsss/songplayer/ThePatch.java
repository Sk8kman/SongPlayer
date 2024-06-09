package com.github.hhhzzzsss.songplayer;

import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ThePatch {
    public static void patch(BlockPos bp) {
        World world = MinecraftClient.getInstance().world;
        assert world != null;
        BlockState thisState = world.getBlockState(bp);
        BlockState thatState = world.getBlockState(bp.down());
        if (thisState.getBlock() instanceof NoteBlock) {
            //patch some server or some core hides note block attributes
            world.setBlockState(bp, thisState.with(NoteBlock.INSTRUMENT, thatState.getInstrument()));
        }
    }
}
