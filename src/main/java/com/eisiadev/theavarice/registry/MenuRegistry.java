package com.eisiadev.theavarice.registry;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.wallet.WalletMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuRegistry {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TheAvarice.MOD_ID);

    public static final RegistryObject<MenuType<WalletMenu>> WALLET =
            MENUS.register("wallet", () -> IForgeMenuType.create((id, inventory, buf) -> new WalletMenu(id, inventory)));
}
