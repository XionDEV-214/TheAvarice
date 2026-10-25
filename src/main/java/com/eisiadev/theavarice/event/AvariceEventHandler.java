package com.eisiadev.theavarice.event;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.enchantment.AvariceEnchantment;
import com.eisiadev.theavarice.registry.EnchantmentRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = TheAvarice.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AvariceEventHandler {

    private static final Map<UUID, List<ItemStack>> GLM_PROCESSED_ITEMS = new ConcurrentHashMap<>();

    public static void markGlmProcessed(UUID entityId, List<ItemStack> processedItems) {
        GLM_PROCESSED_ITEMS.put(entityId, new ArrayList<>(processedItems));
    }

    private static boolean consumeGlmProcessed(UUID entityId, ItemStack stack) {
        List<ItemStack> processed = GLM_PROCESSED_ITEMS.get(entityId);
        if (processed == null) return false;
        for (int i = 0; i < processed.size(); i++) {
            if (ItemStack.isSameItemSameTags(processed.get(i), stack)) {
                processed.remove(i);
                if (processed.isEmpty()) GLM_PROCESSED_ITEMS.remove(entityId);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        Player player = resolvePlayer(event.getSource());
        if (player == null) return;

        ItemStack weapon = player.getMainHandItem();
        int avariceLevel = EnchantmentHelper.getItemEnchantmentLevel(
                EnchantmentRegistry.AVARICE.get(), weapon);
        if (avariceLevel <= 0) return;

        UUID entityId = event.getEntity().getUUID();
        double multiplier = AvariceEnchantment.getMultiplier(avariceLevel);
        var random = event.getEntity().getRandom();
        boolean debug = TheAvarice.LOGGER.isDebugEnabled();

        if (debug) TheAvarice.LOGGER.debug("[Avarice] LivingDrops: entity={}, level={}, x{}",
                event.getEntity().getDisplayName().getString(), avariceLevel, multiplier);

        List<ItemEntity> toAdd = new ArrayList<>();

        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            if (consumeGlmProcessed(entityId, stack)) {
                if (debug) TheAvarice.LOGGER.debug("[Avarice] Skip {} (GLM handled)",
                        stack.getDisplayName().getString());
                continue;
            }

            // max stack 1 (장비 등)은 배수 적용 제외
            if (!AvariceEnchantment.isAffected(stack)) {
                if (debug) TheAvarice.LOGGER.debug("[Avarice] Skip {} (max stack 1)",
                        stack.getDisplayName().getString());
                continue;
            }

            int orig = stack.getCount();
            int chunk = AvariceEnchantment.dropChunk(stack.getMaxStackSize());
            long newCount = AvariceEnchantment.capDrop(
                    AvariceEnchantment.multipliedCount(orig, multiplier, random.nextDouble()), chunk);

            if (debug) TheAvarice.LOGGER.debug("[Avarice] {} x{} -> x{}",
                    stack.getDisplayName().getString(), orig, newCount);

            stack.setCount((int) Math.min(newCount, chunk));
            long remaining = newCount - chunk;
            while (remaining > 0) {
                ItemStack copy = stack.copy();
                copy.setCount((int) Math.min(remaining, chunk));
                ItemEntity extra = new ItemEntity(
                        itemEntity.level(),
                        itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), copy);
                extra.setPickUpDelay(10);
                toAdd.add(extra);
                remaining -= chunk;
            }
        }

        event.getDrops().addAll(toAdd);
    }

    private static Player resolvePlayer(DamageSource source) {
        if (source.getEntity() instanceof Player p) return p;
        if (source.getDirectEntity() instanceof Player p) return p;
        if (source.getDirectEntity() instanceof Projectile proj
                && proj.getOwner() instanceof Player p) return p;
        return null;
    }
}
