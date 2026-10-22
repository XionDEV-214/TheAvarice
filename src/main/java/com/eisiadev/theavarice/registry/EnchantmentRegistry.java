package com.eisiadev.theavarice.registry;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.enchantment.AvariceEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EnchantmentRegistry {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, TheAvarice.MOD_ID);

    public static final RegistryObject<AvariceEnchantment> AVARICE =
            ENCHANTMENTS.register("avarice", AvariceEnchantment::new);
}
