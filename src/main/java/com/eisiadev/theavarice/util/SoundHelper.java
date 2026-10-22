package com.eisiadev.theavarice.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class SoundHelper {

    private SoundHelper() {}

    public static void play(Player player, String raw) {
        if (raw == null || raw.isBlank()) return;

        String[] parts = raw.split(",");
        ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
        if (id == null || !ForgeRegistries.SOUND_EVENTS.containsKey(id)) return;

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (sound == null) return;

        float volume;
        float pitch;
        try {
            volume = parts.length > 1 ? Float.parseFloat(parts[1].trim()) : 1.0F;
            pitch = parts.length > 2 ? Float.parseFloat(parts[2].trim()) : 1.0F;
        } catch (NumberFormatException e) {
            return;
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }
}
