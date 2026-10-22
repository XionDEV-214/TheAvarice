package com.eisiadev.theavarice.registry;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.loot.AvariceGlobalLootModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class LootModifierRegistry {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> GLM =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TheAvarice.MOD_ID);

    public static final RegistryObject<Codec<AvariceGlobalLootModifier>> AVARICE_MODIFIER =
            GLM.register("avarice_modifier", () -> AvariceGlobalLootModifier.CODEC);
}
