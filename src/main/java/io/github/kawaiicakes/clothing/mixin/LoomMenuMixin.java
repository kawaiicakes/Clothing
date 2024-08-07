package io.github.kawaiicakes.clothing.mixin;

import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin extends AbstractContainerMenu {
    @Final
    @Shadow
    private Container inputContainer;

    @Final
    @Mutable
    @Shadow
    Slot bannerSlot;

    @Final
    @Mutable
    @Shadow
    Slot dyeSlot;

    @Final
    @Mutable
    @Shadow
    private Slot patternSlot;

    @Final
    @Mutable
    @Shadow
    private Slot resultSlot;

    @Inject(
            at = @At("TAIL"),
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V"
    )
    private void init(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pAccess, CallbackInfo ci) {
        Slot bannerSlot = new Slot(this.inputContainer, 0, 13, 26) {
            public boolean mayPlace(@NotNull ItemStack stack) {
                Item item = stack.getItem();
                return item instanceof BannerItem || item instanceof ClothingItem<?>;
            }
        };
        this.bannerSlot = bannerSlot;
        this.slots.set(0, bannerSlot);
    }

    // Rather than overwrite the whole thing, I'm just gonna run the logic twice in case another mod modifies the target
    @Inject(
            method = "quickMoveStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void quickMoveStack(Player player, int index, CallbackInfoReturnable<ItemStack> ci) {
        Slot slotFromMixin = this.slots.get(index);

        if (slotFromMixin.hasItem() && index != this.resultSlot.index) {
            ItemStack itemstack1B = slotFromMixin.getItem();

            if (index != this.dyeSlot.index && index != this.bannerSlot.index && index != this.patternSlot.index) {
                if (itemstack1B.getItem() instanceof ClothingItem<?>) {
                    if (
                            !this.moveItemStackTo(
                                    itemstack1B,
                                    this.bannerSlot.index,
                                    this.bannerSlot.index + 1,
                                    false
                            )
                    ) {
                        ci.setReturnValue(ItemStack.EMPTY);
                        ci.cancel();
                    }
                }
            }
        }
    }

    private LoomMenuMixin(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
    }
}
