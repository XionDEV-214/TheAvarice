package com.eisiadev.theavarice.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Accessor("xpos")
    void theavarice$setXpos(double value);

    @Accessor("ypos")
    void theavarice$setYpos(double value);
}
