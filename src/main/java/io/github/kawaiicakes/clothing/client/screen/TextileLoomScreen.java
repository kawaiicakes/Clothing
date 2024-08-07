package io.github.kawaiicakes.clothing.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.clothing.common.menu.TextileLoomMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class TextileLoomScreen extends AbstractContainerScreen<TextileLoomMenu> {
    public TextileLoomScreen(TextileLoomMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        pMenu.registerUpdateListener(this::containerChanged);
    }

    @Override
    protected void renderBg(@NotNull PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {

    }

    protected void containerChanged() {
        // TODO
    }
}
