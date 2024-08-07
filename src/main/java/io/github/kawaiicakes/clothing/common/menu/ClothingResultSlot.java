package io.github.kawaiicakes.clothing.common.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

public class ClothingResultSlot extends ResultSlot {
    protected final Container dyeContainer;

    public ClothingResultSlot(
            Player pPlayer,
            Container dyeContainer, CraftingContainer pCraftSlots, Container pContainer,
            int pSlot,
            int pXPosition, int pYPosition
    ) {
        super(pPlayer, pCraftSlots, pContainer, pSlot, pXPosition, pYPosition);
        this.dyeContainer = dyeContainer;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void onTake(Player pPlayer, ItemStack pStack) {
        // TODO
        this.checkTakeAchievements(pStack);
        net.minecraftforge.common.ForgeHooks.setCraftingPlayer(pPlayer);
        NonNullList<ItemStack> nonnulllist = pPlayer.level.getRecipeManager()
                .getRemainingItemsFor(TEXTILE_LOOM_CRAFTING, this.craftSlots, pPlayer.level);
        net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
        for(int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = this.craftSlots.getItem(i);
            ItemStack itemstack1 = nonnulllist.get(i);
            if (!itemstack.isEmpty()) {
                this.craftSlots.removeItem(i, 1);
                itemstack = this.craftSlots.getItem(i);
            }

            if (!itemstack1.isEmpty()) {
                if (itemstack.isEmpty()) {
                    this.craftSlots.setItem(i, itemstack1);
                } else if (ItemStack.isSame(itemstack, itemstack1) && ItemStack.tagMatches(itemstack, itemstack1)) {
                    itemstack1.grow(itemstack.getCount());
                    this.craftSlots.setItem(i, itemstack1);
                } else if (!this.player.getInventory().add(itemstack1)) {
                    this.player.drop(itemstack1, false);
                }
            }
        }
    }
}
