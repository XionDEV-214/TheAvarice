package com.eisiadev.theavarice.currency;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.config.AvariceConfig;
import com.eisiadev.theavarice.enchantment.AvariceEnchantment;
import com.eisiadev.theavarice.registry.EnchantmentRegistry;
import com.eisiadev.theavarice.util.SoundHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = TheAvarice.MOD_ID)
public final class CurrencyHandler {

    private static final int MAX_STACK = 2_000_000_000;

    private CurrencyHandler() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide) return;

        List<CurrencyTiers.Tier> tiers = CurrencyTiers.load();
        if (tiers.isEmpty()) return;

        ItemStack weapon = player.getMainHandItem();

        int lootingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, weapon);
        int avariceLevel = EnchantmentHelper.getItemEnchantmentLevel(EnchantmentRegistry.AVARICE.get(), weapon);
        double avariceMultiplier = avariceLevel > 0 ? AvariceEnchantment.getMultiplier(avariceLevel) : 1.0;

        double maxHealth = entity.getMaxHealth();
        double baseCurrency = Math.floor(maxHealth * 6.5 * (1.0 + 0.01 * Math.sqrt(maxHealth)));
        long remaining = (long) Math.floor(baseCurrency * (1.0 + lootingLevel) * avariceMultiplier);
        if (remaining <= 0) return;

        for (CurrencyTiers.Tier tier : tiers) {
            if (remaining <= 0) break;

            long count = remaining / tier.value();
            if (count <= 0) continue;
            remaining %= tier.value();

            while (count > 0) {
                int amount = (int) Math.min(count, MAX_STACK);
                ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(),
                        new ItemStack(tier.item(), amount));
                drop.setPickUpDelay(0);
                level.addFreshEntity(drop);
                count -= amount;
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) return;

        Item picked = event.getStack().getItem();
        List<CurrencyTiers.Tier> tiers = CurrencyTiers.load();
        if (tiers.stream().noneMatch(tier -> tier.item() == picked)) return;

        for (String raw : AvariceConfig.PICKUP_SOUNDS.get()) {
            SoundHelper.play(player, raw);
        }
    }
}
