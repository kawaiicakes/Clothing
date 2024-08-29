package io.github.kawaiicakes.clothing.common.item;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClothingItemRenderer;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

// TODO: add non-creative behaviour in loom menu, add builder in recipe generator
public class OverlayPatternItem extends BannerPatternItem {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String OVERLAY_KEY = "overlay";
    public static final String CREATIVE_KEY = "isCreative";

    public OverlayPatternItem(Properties pProperties) {
        super(BannerPatternTags.PATTERN_ITEM_SKULL, pProperties);
    }

    @Override
    @NotNull
    public ItemStack getDefaultInstance() {
        ItemStack toReturn = super.getDefaultInstance();

        this.setOverlay(toReturn, new ResourceLocation("clothing:none"));
        this.setIsCreative(toReturn, false);

        return toReturn;
    }

    public void setIsCreative(ItemStack stack, boolean isCreative) {
        stack.getOrCreateTag().putBoolean(CREATIVE_KEY, isCreative);
    }

    public boolean getIsCreative(ItemStack stack) {
        if (!stack.getOrCreateTag().contains(CREATIVE_KEY, Tag.TAG_BYTE))
            stack.getOrCreateTag().putBoolean(CREATIVE_KEY, false);
        return stack.getOrCreateTag().getBoolean(CREATIVE_KEY);
    }

    public void setOverlay(ItemStack stack, ResourceLocation location) {
        stack.getOrCreateTag().putString(OVERLAY_KEY, location.toString());
    }

    public ResourceLocation getOverlay(ItemStack stack) {
        if (!stack.getOrCreateTag().contains(OVERLAY_KEY, Tag.TAG_STRING))
            stack.getOrCreateTag().putString(OVERLAY_KEY, "clothing:none");

        return new ResourceLocation(stack.getOrCreateTag().getString(OVERLAY_KEY));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        pTooltip.add(Component.literal(this.getOverlay(pStack).toString()).withStyle(ChatFormatting.GRAY));
        if (this.getIsCreative(pStack))
            pTooltip.add(
                    Component.translatable("item.modifiers.clothing.is_creative_pattern")
                            .withStyle(ChatFormatting.DARK_GRAY)
            );
    }

    @Override
    @ParametersAreNonnullByDefault
    public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems) {
        if (!this.allowedIn(pCategory)) return;

        try {
            final OverlayDefinitionLoader loader = OverlayDefinitionLoader.getInstance();

            for (OverlayDefinitionLoader.OverlayDefinition definition : loader.getOverlays()) {
                ItemStack toAdd = this.getDefaultInstance();

                this.setOverlay(toAdd, definition.name());
                this.setIsCreative(toAdd, true);

                pItems.add(toAdd);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to generate clothing entries!", e);
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(
                new IClientItemExtensions() {
                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return ClothingItemRenderer.getInstance();
                    }
                }
        );
    }
}
