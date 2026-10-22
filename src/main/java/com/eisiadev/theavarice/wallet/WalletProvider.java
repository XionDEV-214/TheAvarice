package com.eisiadev.theavarice.wallet;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WalletProvider implements ICapabilitySerializable<CompoundTag> {

    private final Wallet wallet = new Wallet();
    private final LazyOptional<Wallet> holder = LazyOptional.of(() -> wallet);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return WalletCapability.WALLET.orEmpty(cap, holder);
    }

    @Override
    public CompoundTag serializeNBT() {
        return wallet.save();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        wallet.load(tag);
    }
}
