package io.github.kawaiicakes.clothing.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.LoomMenuOverlayGetter;
import io.github.kawaiicakes.clothing.common.data.ClothingLayer;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.SpoolItem;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.core.Holder;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: overlay pattern: banner pattern but allows access to otherwise unobtainable overlays (also allows op'd/creative players to force overlays onto clothing that normally shouldn't work)
@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin extends AbstractContainerMenu implements LoomMenuOverlayGetter {
    @Unique
    private static final Logger clothing$LOGGER = LogUtils.getLogger();

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

    @Final
    @Mutable
    @Shadow
    DataSlot selectedBannerPatternIndex;

    @Shadow private List<Holder<BannerPattern>> selectablePatterns;

    @Unique
    protected List<OverlayDefinitionLoader.OverlayDefinition> clothing$selectableOverlays = List.of();

    @Unique
    private boolean clothing$isValidOverlayIndex(int i) {
        return i >= 0 && i < this.clothing$selectableOverlays.size();
    }

    @Override
    public List<OverlayDefinitionLoader.OverlayDefinition> getClothing$selectableOverlays() {
        return this.clothing$selectableOverlays;
    }

    @Override
    public List<OverlayDefinitionLoader.OverlayDefinition> getClothing$selectableOverlays(
            ItemStack clothing, ItemStack spool, ItemStack pattern
    ) {
        try {
            if (!(clothing.getItem() instanceof ClothingItem))
                throw new IllegalArgumentException("Passed stack '" + clothing + "' is not a clothing item!");

            if (clothing.isEmpty()) return List.of();

            if (spool.isEmpty() || !(spool.getItem() instanceof SpoolItem)) return List.of();

            return OverlayDefinitionLoader.getInstance()
                    .getOverlays()
                    .stream()
                    .filter(definition -> definition.isValidEntry(clothing))
                    .toList();
        } catch (Exception e) {
            clothing$LOGGER.error("Exception while trying to obtain valid overlays for '{}'!", clothing, e);
            return List.of();
        }
    }

    /**
     * Intended to prevent selection of banner patterns if a spool item is present in the dye slot.
     */
    @WrapMethod(method = "getSelectablePatterns(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;")
    private List<Holder<BannerPattern>> getSelectablePatternsWrapper(
            ItemStack patternStack, Operation<List<Holder<BannerPattern>>> original
    ) {
        if (this.dyeSlot.getItem().getItem() instanceof SpoolItem) return List.of();
        return original.call(patternStack);
    }

    @Unique
    private void clothing$setupClothingResultSlot(OverlayDefinitionLoader.OverlayDefinition overlay) {
        ItemStack clothingStack = this.bannerSlot.getItem();
        ItemStack dyeStack = this.dyeSlot.getItem();
        ItemStack outputStack = ItemStack.EMPTY;

        if (
                !clothingStack.isEmpty()
                        && clothingStack.getItem() instanceof ClothingItem
                        || dyeStack.isEmpty()
        ) {
            outputStack = clothingStack.copy();
            outputStack.setCount(1);

        if (dyeStack.getItem() instanceof DyeItem dyeItem) {
            List<DyeItem> dyeColor = Collections.singletonList(dyeItem);
            ClothingItem.dyeClothing(outputStack, dyeColor);
        }

        if (
                clothingStack.getItem() instanceof ClothingItem genericClothingItem
                        && overlay != null
                        && dyeStack.getItem() instanceof SpoolItem spoolItem
        ) {
            ClothingLayer layerToAdd = new ClothingLayer(
                    overlay.name(), spoolItem.getColor(dyeStack), null
            );

            ImmutableListMultimap<ClothingItem.MeshStratum, ClothingLayer> existingOverlays
                    = genericClothingItem.getOverlays(clothingStack);

            ImmutableList<ClothingLayer> overlayList = existingOverlays.get(
                    ClothingItem.MeshStratum.forSlot(genericClothingItem.getSlot())
            );

                if (overlayList.isEmpty()) {
                    /* FIXME:
                        The changes made here for this commit are TEMPORARY solely so it can build and be tested.
                        To implement the behaviour fully would mean the introduction of a new DataSlot; the same
                        one that would allow for discriminating which mesh to target when dyeing clothing
                        or its overlays. That would then fall out of the scope of this commit and delay the push.
                     */
                    Multimap<ClothingItem.MeshStratum, ClothingLayer> overlaysForEmpty = ImmutableListMultimap.of(
                                    ClothingItem.MeshStratum.forSlot(genericClothingItem.getSlot()),
                            layerToAdd
                    );

                    genericClothingItem.setOverlays(
                            outputStack,
                            overlaysForEmpty
                    );
                } else if (!overlayList.get(0).equals(layerToAdd)) {
                        genericClothingItem.addOverlay(
                                outputStack,
                                ClothingItem.MeshStratum.forSlot(genericClothingItem.getSlot()),
                                layerToAdd
                        );
                }
            }
        }

        if (!ItemStack.matches(outputStack, this.resultSlot.getItem())) {
            this.resultSlot.set(outputStack);
        }
    }

    /**
     * Allows checking for slot validity for both banners and clothing items.
     * @param obj the object being checked against in the {@code instanceof} call.
     * @param original the original result of the {@code instanceof} call.
     */
    @WrapOperation(
            method = "quickMoveStack",
            constant = @Constant(classValue = BannerItem.class)
    )
    private boolean quickMoveStackInstanceOfBannerItem(Object obj, Operation<Boolean> original) {
        return original.call(obj) || obj instanceof ClothingItem;
    }

    /**
     * Allows checking for slot validity for both dye and spool items.
     * @param obj the object being checked against in the {@code instanceof} call.
     * @param original the original result of the {@code instanceof} call.
     */
    @WrapOperation(
            method = "quickMoveStack",
            constant = @Constant(classValue = DyeItem.class)
    )
    private boolean quickMoveStackInstanceOfDyeItem(Object obj, Operation<Boolean> original) {
        return original.call(obj) || obj instanceof SpoolItem;
    }

    /**
     * This injected block will only execute if there is a clothing item in the banner slot. It's called prior to
     * {@link LoomMenu#broadcastChanges()} so as to "replace" operations that the original Loom logic performed.
     * @param container the {@link Container} in which the slots are changing.
     * @param ci callback info
     */
    @Inject(
            method = "slotsChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/LoomMenu;broadcastChanges()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void slotsChangedDoClothingLogic(Container container, CallbackInfo ci) {
        if (this.bannerSlot.getItem().isEmpty() || !(this.bannerSlot.getItem().getItem() instanceof ClothingItem))
            return;

        this.selectablePatterns = List.of();

        ItemStack clothingStack = this.bannerSlot.getItem();
        ItemStack dyeStack = this.dyeSlot.getItem();
        ItemStack patternStack = this.patternSlot.getItem();

        List<OverlayDefinitionLoader.OverlayDefinition> oldList = this.clothing$selectableOverlays;
        this.clothing$selectableOverlays = this.getClothing$selectableOverlays(
                clothingStack, dyeStack, patternStack
        );

        int i = this.selectedBannerPatternIndex.get();
        boolean validOverlayIndex = this.clothing$isValidOverlayIndex(i);

        OverlayDefinitionLoader.OverlayDefinition overlay;

        if (this.clothing$selectableOverlays.size() == 1) {
            this.selectedBannerPatternIndex.set(0);
            overlay = this.clothing$selectableOverlays.get(0);
        } else if (!validOverlayIndex) {
            this.selectedBannerPatternIndex.set(-1);
            overlay = null;
        } else {
            OverlayDefinitionLoader.OverlayDefinition overlay1 = oldList.get(i);
            int j = this.clothing$selectableOverlays.indexOf(overlay1);
            if (j != -1) {
                overlay = overlay1;
                this.selectedBannerPatternIndex.set(j);
            } else {
                overlay = null;
                this.selectedBannerPatternIndex.set(-1);
            }
        }

        if (overlay != null || this.dyeSlot.getItem().getItem() instanceof DyeItem) {
            this.clothing$setupClothingResultSlot(overlay);
        } else {
            this.resultSlot.set(ItemStack.EMPTY);
        }
    }

    /**
     * FIXME: this should be using an MEV; wait for MixinExtras dev to fix the bug and then change this
     * Adds a check prior to setting up the result slot to see if the dye slot has a dye item in it; not a spool
     */
    @Definition(id = "holder", local = @Local(type = Holder.class, ordinal = 0))
    @Expression("holder != null")
    @WrapOperation(method = "slotsChanged", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean slotsChangedModifySetupResultSlotLogic(Object left, Object right, Operation<Boolean> original) {
        return original.call(left, right) && !(this.dyeSlot.getItem().getItem() instanceof SpoolItem);
    }

    /**
     * Adds a check prior to setting up the result slot to see if the dye slot has a dye item in it; not a spool.
     * Duplicated methods because the itemstack in the dye slot might be out of sync and thus return true
     * in instances where it shouldn't
     */
    @ModifyExpressionValue(
            method = "setupResultSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z",
                    ordinal = 1
            )
    )
    private boolean setupResultSlotModifyLogic(boolean original) {
        return original && !(this.dyeSlot.getItem().getItem() instanceof SpoolItem);
    }

    /**
     * Adds bits handling clothing into the {@code else} bit of the if-block to "reset" the Loom, as in the original
     * method.
     * @param container the {@link Container} in which the slots are changing.
     * @param ci callback info
     */
    @Inject(
            method = "slotsChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;of()Ljava/util/List;",
                    shift = At.Shift.AFTER
            )
    )
    private void slotsChangedWipeClothing(Container container, CallbackInfo ci) {
        this.clothing$selectableOverlays = List.of();
    }

    /**
     * Allows for execution of different logic in {@link #clickMenuButton(Player, int)} if the banner slot has a
     * clothing item in it.
     * @param original the result of calling {@link ItemStack#getItem()}.
     * @return The size of {@link #clothing$selectableOverlays} if the banner slot has a {@link ClothingItem} in it.
     */
    @ModifyExpressionValue(
            method = "clickMenuButton",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;size()I"
            )
    )
    private int clickMenuButtonCheckForOverlays(int original) {
        if (!this.bannerSlot.getItem().isEmpty() && this.bannerSlot.getItem().getItem() instanceof ClothingItem)
            return this.clothing$selectableOverlays.size();

        return original;
    }

    /**
     * This is a bit of a hacky way to stop an {@link ArrayIndexOutOfBoundsException} from being thrown when
     * {@link #clickMenuButtonModifySetupResultSlotLogic(LoomMenu, Holder, Operation, Player, int)} attempts to parse
     * the arguments being passed to the original {@code Operation}. This results in an exception since the banner
     * patterns are expected to be empty when a piece of clothing goes in. Returning null would ordinarily be
     * problematic, but since the original operation should not be called in the event that a null value is passed to
     * it to begin with, this should be fine.
     */
    @WrapOperation(
            method = "clickMenuButton",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/inventory/LoomMenu;selectablePatterns:Ljava/util/List;"
            )
    )
    private List<Holder<BannerPattern>> clickMenuButtonPreventArrayOutOfBoundsException(
            LoomMenu instance, Operation<List<Holder<BannerPattern>>> original
    ) {
        List<Holder<BannerPattern>> toReturn = original.call(instance);

        if (toReturn.isEmpty()) {
            List<Holder<BannerPattern>> nullList = new ArrayList<>();

            for (int i = 0; i < this.clothing$selectableOverlays.size(); i++) {
                nullList.add(null);
            }

            toReturn = nullList;
        }

        return toReturn;
    }

    /**
     * Rather than immediately setting up the result slot, the original method is only called if the item in the banner
     * slot is not a clothing item AND the dye slot does not have a spool in it.
     * @param instance the {@link LoomMenu} instance this is being called in
     * @param dyeColor the argument being passed to {@link LoomMenu#setupResultSlot(Holder)}
     * @param original the reference to the original {@link LoomMenu#setupResultSlot(Holder)} call
     * @param pPlayer the argument passed to {@code instance}
     * @param pId the argument passed to {@code instance}
     */
    @WrapOperation(
            method = "clickMenuButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/LoomMenu;setupResultSlot(Lnet/minecraft/core/Holder;)V"
            )
    )
    private void clickMenuButtonModifySetupResultSlotLogic(
            LoomMenu instance, Holder<BannerPattern> dyeColor, Operation<Void> original, Player pPlayer, int pId
    ) {
        if (!this.bannerSlot.getItem().isEmpty() && this.bannerSlot.getItem().getItem() instanceof ClothingItem) {
            this.clothing$setupClothingResultSlot(this.clothing$selectableOverlays.get(pId));
            return;
        }

        if (this.dyeSlot.getItem().getItem() instanceof SpoolItem) return;

        original.call(instance, dyeColor);
    }

    private LoomMenuMixin(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
    }
}
