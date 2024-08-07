package io.github.kawaiicakes.clothing.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {
    /*
        TODO

        #containerChanged either needs to be injected into heavily or a new method is supplied in the constructor using
            LoomMenu#registerUpdateListener.

        #init or #renderBg should be ready to render the player's model à la the inventory but with the clothing

        #renderBg needs to support overlay selection

        #renderOverlayPattern, @Unique method mirroring #renderPattern

        More changes as the need for them arises
     */

    private LoomScreenMixin(LoomMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }
}
