package com.ppcharcoal.arcticfox.registry;

import com.ppcharcoal.arcticfox.ArcticFox;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ArcticFox.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> WENDIGO_BREATH = register("entity.wendigo.breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> WENDIGO_CLICK = register("entity.wendigo.click");
    public static final DeferredHolder<SoundEvent, SoundEvent> WENDIGO_STEP = register("entity.wendigo.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> WENDIGO_SCREAM = register("entity.wendigo.scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> WENDIGO_VANISH = register("entity.wendigo.vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> WENDIGO_HURT = register("entity.wendigo.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WENDIGO_DEATH = register("entity.wendigo.death");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ArcticFox.id(name)));
    }
}
