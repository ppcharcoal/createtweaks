package com.ppcharcoal.arcticfox.mixin;

import com.ppcharcoal.arcticfox.compat.SprinklerRain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Serene Seasons and Create Slice & Dice (old versions directly, 4.3+ through its Atmosphere library)
 * both cancel Level#isRainingAt at HEAD, so whichever runs first wins. When Serene Seasons wins it
 * hides the sprinkler rain. We append the sprinkler check to Serene Seasons' own hook.
 */
@Mixin(targets = "sereneseasons.season.SeasonHooks", remap = false)
public abstract class SeasonHooksMixin {
    @Inject(method = "isRainingAtHook", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void arcticfox$sprinklerRain(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && SprinklerRain.isSprinklerRain(level, pos)) {
            cir.setReturnValue(true);
        }
    }
}
