package io.github.kawaiicakes.clothing.common.menu;

import io.github.kawaiicakes.clothing.common.block.ClothingBlockRegistry;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class TextileLoomMenu extends AbstractContainerMenu {
    protected final ContainerLevelAccess access;
    protected final Player player;
    protected Runnable slotUpdateListener = () -> {};
    protected final DataSlot selectedOverlayIndex = DataSlot.standalone();
    protected final CraftingContainer craftContainer = new CraftingContainer(this, 3, 3);
    protected final Container dyeContainer = new SimpleContainer(1) {
        public void setChanged() {
            super.setChanged();
            TextileLoomMenu.this.slotsChanged(this);
            TextileLoomMenu.this.slotUpdateListener.run();
        }
    };
    protected final ResultContainer resultContainer = new ResultContainer();

    public TextileLoomMenu(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pContainerLevelAccess) {
        super(ClothingMenuRegistry.TEXTILE_LOOM_MENU.get(), pContainerId);
        this.access = pContainerLevelAccess;
        this.player = pPlayerInventory.player;

        this.addDyeSlot();
        this.addCraftingSlots();
        this.addResultSlot(pPlayerInventory);
        this.addPlayerInventorySlots(pPlayerInventory);
        this.addDataSlot(this.selectedOverlayIndex);
    }

    public TextileLoomMenu(int pContainerId, Inventory inventory) {
        this(pContainerId, inventory, ContainerLevelAccess.NULL);
    }

    public void addDyeSlot() {
        this.addSlot(new Slot(this.dyeContainer, 1, 33, 26) {
            public boolean mayPlace(@NotNull ItemStack pStack) {
                return pStack.getItem() instanceof DyeItem;
            }
        });
    }

    public void addCraftingSlots() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(this.craftContainer, j + i * 3, 30 + j * 18, 17 + i * 18));
            }
        }
    }

    public void addResultSlot(Inventory inventory) {
        this.addSlot(
                new ClothingResultSlot(
                        inventory.player,
                        this.dyeContainer,
                        this.craftContainer,
                        this.resultContainer,
                        0,
                        124,
                        35
                )
        );
    }

    public void addPlayerInventorySlots(Inventory pPlayerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(pPlayerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(pPlayerInventory, k, 8 + k * 18, 142));
        }
    }

    public void registerUpdateListener(Runnable pListener) {
        this.slotUpdateListener = pListener;
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        // TODO
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return AbstractContainerMenu.stillValid(this.access, pPlayer, ClothingBlockRegistry.TEXTILE_LOOM_BLOCK.get());
    }

    @Override
    public void removed(@NotNull Player pPlayer) {
        super.removed(pPlayer);
        this.access.execute((pLevel, pPos) -> {
            this.clearContainer(pPlayer, this.dyeContainer);
            this.clearContainer(pPlayer, this.craftContainer);
        });
    }

    @Override
    public void slotsChanged(@NotNull Container pContainer) {
        super.slotsChanged(pContainer);
        this.access.execute((pLevel, pPos) ->
                slotChangedCraftingGrid(
                        this, pLevel, this.player,
                        this.craftContainer, this.resultContainer, this.dyeContainer
                )
        );
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack pStack, Slot pSlot) {
        return pSlot.container != this.resultContainer && super.canTakeItemForPickAll(pStack, pSlot);
    }

    // TODO
    protected static void slotChangedCraftingGrid(
            AbstractContainerMenu pMenu,
            Level pLevel,
            Player pPlayer,
            CraftingContainer pContainer,
            ResultContainer pResult,
            Container dyeContainer
    ) {
        if (pLevel.isClientSide) return;

        ServerPlayer serverPlayer = (ServerPlayer) pPlayer;
        ItemStack itemstack = ItemStack.EMPTY;

        Optional<CraftingRecipe> potentialRecipe = Objects.requireNonNull(pLevel.getServer())
                .getRecipeManager()
                .getRecipeFor(TEXTILE_LOOM_CRAFTING, pContainer, pLevel);
        if (potentialRecipe.isPresent()) {
            CraftingRecipe craftingrecipe = potentialRecipe.get();
            if (pResult.setRecipeUsed(pLevel, serverPlayer, craftingrecipe)) {
                itemstack = craftingrecipe.assemble(pContainer);
            }
        }

        int resultSlotIndex = 0;

        pResult.setItem(resultSlotIndex, itemstack);
        pMenu.setRemoteSlot(resultSlotIndex, itemstack);
        serverPlayer.connection.send(
                new ClientboundContainerSetSlotPacket(
                        pMenu.containerId, pMenu.incrementStateId(), resultSlotIndex, itemstack
                )
        );
    }
}
