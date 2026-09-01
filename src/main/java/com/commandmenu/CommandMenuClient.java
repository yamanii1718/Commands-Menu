package com.commandmenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class CommandMenuClient implements ClientModInitializer {

    public static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        // In 1.21.4+ the InputUtil.Type parameter was removed from the KeyBinding constructor
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.commandmenu.open",
            GLFW.GLFW_KEY_K,
            KeyBinding.Category.MISC   // shows under Miscellaneous in Controls screen
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenuKey.wasPressed()) {
                if (mc.player != null) {
                    mc.setScreen(new CommandMenuScreen());
                }
            }
        });
    }
}
