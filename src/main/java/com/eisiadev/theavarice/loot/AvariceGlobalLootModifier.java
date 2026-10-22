package com.eisiadev.theavarice.loot;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.enchantment.AvariceEnchantment;
import com.eisiadev.theavarice.event.AvariceEventHandler;
import com.eisiadev.theavarice.registry.EnchantmentRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class AvariceGlobalLootModifier extends LootModifier {

    public static final Codec<AvariceGlobalLootModifier> CODEC =
            RecordCodecBuilder.create(instance -> codecStart(instance)
                    .apply(instance, AvariceGlobalLootModifier::new));

    public AvariceGlobalLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Nonnull
    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot, LootContext context) {

        Player player = resolvePlayer(context);
        if (player == null) return generatedLoot;

        int avariceLevel = EnchantmentHelper.getItemEnchantmentLevel(
                EnchantmentRegistry.AVARICE.get(), player.getMainHandItem());
        if (avariceLevel <= 0) return generatedLoot;

        double multiplier = AvariceEnchantment.getMultiplier(avariceLevel);
        var random = context.getRandom();
        boolean debug = TheAvarice.LOGGER.isDebugEnabled();

        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);

        if (debug) {
            String name = entity != null ? entity.getDisplayName().getString() : "unknown";
            TheAvarice.LOGGER.debug("[Avarice] GLM Entity: entity={}, level={}, x{}", name, avariceLevel, multiplier);
        }

        ObjectArrayList<ItemStack> result = new ObjectArrayList<>();

        for (ItemStack stack : generatedLoot) {
            if (stack.isEmpty()) continue;

            int orig = stack.getCount();
            double raw = orig * multiplier;
            int newCount = (int) raw;
            if (random.nextDouble() < (raw - newCount)) newCount++;

            if (debug) TheAvarice.LOGGER.debug("[Avarice] GLM: {} x{} -> x{}",
                    stack.getDisplayName().getString(), orig, newCount);

            int max = stack.getMaxStackSize();
            while (newCount > 0) {
                ItemStack copy = stack.copy();
                copy.setCount(Math.min(newCount, max));
                result.add(copy);
                newCount -= max;
            }
        }

        if (entity != null) {
            AvariceEventHandler.markGlmProcessed(entity.getUUID(), new ArrayList<>(result));
        }

        return result;
    }

    private static Player resolvePlayer(LootContext context) {
        Entity killer = context.getParamOrNull(LootContextParams.KILLER_ENTITY);
        Entity directKiller = context.getParamOrNull(LootContextParams.DIRECT_KILLER_ENTITY);
        if (killer instanceof Player p) return p;
        if (directKiller instanceof Player p) return p;
        if (directKiller instanceof Projectile proj && proj.getOwner() instanceof Player p) return p;
        return null;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
