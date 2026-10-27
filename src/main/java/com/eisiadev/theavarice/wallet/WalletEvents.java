package com.eisiadev.theavarice.wallet;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.config.AvariceConfig;
import com.eisiadev.theavarice.currency.CurrencyGroup;
import com.eisiadev.theavarice.currency.CurrencyGroups;
import com.eisiadev.theavarice.currency.CurrencyTiers;
import com.eisiadev.theavarice.util.SoundHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = TheAvarice.MOD_ID)
public final class WalletEvents {

    private WalletEvents() {}

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(WalletCapability.KEY, new WalletProvider());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(WalletCapability.WALLET).ifPresent(old ->
                event.getEntity().getCapability(WalletCapability.WALLET).ifPresent(fresh -> fresh.copyFrom(old)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemEntity itemEntity = event.getItem();
        if (!itemEntity.isAlive() || itemEntity.hasPickUpDelay()) return;

        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty() || stack.hasTag()) return;
        if (!isAllowedToTake(itemEntity, player)) return;

        CurrencyGroup group = CurrencyGroups.findByItem(stack.getItem());
        if (group == null) return;
        CurrencyTiers.Tier matched = group.find(stack.getItem());

        Wallet wallet = WalletCapability.get(player);
        wallet.add(matched.id(), stack.getCount());
        wallet.normalize(group.tiers());

        stack.setCount(0);
        event.setResult(Event.Result.ALLOW);

        for (String raw : AvariceConfig.PICKUP_SOUNDS.get()) {
            SoundHelper.play(player, raw);
        }

        // The wallet screen tracks its balances through ModularUI sync values; only the enchant screen needs a push.
        if (player.containerMenu instanceof com.eisiadev.theavarice.enchant.EnchantMenu) {
            WalletService.sync(player);
        }
    }

    private static boolean isAllowedToTake(ItemEntity itemEntity, Player player) {
        CompoundTag tag = itemEntity.saveWithoutId(new CompoundTag());
        return !tag.hasUUID("Owner") || tag.getUUID("Owner").equals(player.getUUID());
    }
}
