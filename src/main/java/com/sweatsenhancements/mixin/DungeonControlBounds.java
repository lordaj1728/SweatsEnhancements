package com.sweatsenhancements.mixin;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.DungeonRunHud;
import com.sweatsenhancements.feature.DungeonControlShape;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockLever;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-side selection bounds only; does not change collision, reach or server blocks. */
@Mixin({BlockButton.class, BlockLever.class})
public abstract class DungeonControlBounds {
    @Inject(method = "func_180654_a", at = @At("HEAD"), cancellable = true, remap = false)
    private void sweatsenhancements$widenDungeonControl(IBlockAccess world, BlockPos pos, CallbackInfo ci) {
        if (SweatsEnhancements.cfg == null || !DungeonRunHud.isInDungeon() || world == null || pos == null) return;
        try {
            Block block = (Block)(Object)this;
            boolean lever = block instanceof BlockLever;
            if (!SweatsEnhancements.cfg.getBool(Cfg.key(lever ? "DU017" : "DU018"),false)) return;
            IBlockState state = world.getBlockState(pos);
            EnumFacing facing = lever ? state.getValue(BlockLever.FACING).getFacing()
                    : state.getValue(BlockButton.FACING);
            float[] b = DungeonControlShape.bounds(facing,lever ? .5f : .25f);
            if (b == null) return;
            block.setBlockBounds(b[0],b[1],b[2],b[3],b[4],b[5]);
            ci.cancel();
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] control bounds failed: " + t); }
    }
}
