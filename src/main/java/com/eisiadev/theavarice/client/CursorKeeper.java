package com.eisiadev.theavarice.client;

import com.eisiadev.theavarice.TheAvarice;
import com.eisiadev.theavarice.mixin.client.MouseHandlerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TheAvarice.MOD_ID, value = Dist.CLIENT)
public final class CursorKeeper {

    private static final long TIMEOUT_MILLIS = 3000L;

    private static double x;
    private static double y;
    private static long deadline;

    private CursorKeeper() {}

    public static void remember() {
        Minecraft minecraft = Minecraft.getInstance();
        x = minecraft.mouseHandler.xpos();
        y = minecraft.mouseHandler.ypos();
        deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (deadline == 0L) return;
        if (System.currentTimeMillis() > deadline) {
            deadline = 0L;
            return;
        }
        if (!(event.getScreen() instanceof WalletScreen) && !(event.getScreen() instanceof EnchantScreen)) return;
        deadline = 0L;

        Minecraft minecraft = Minecraft.getInstance();
        GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), x, y);
        MouseHandlerAccessor accessor = (MouseHandlerAccessor) minecraft.mouseHandler;
        accessor.theavarice$setXpos(x);
        accessor.theavarice$setYpos(y);
    }
}
