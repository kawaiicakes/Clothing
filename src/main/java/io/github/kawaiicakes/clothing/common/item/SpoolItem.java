package io.github.kawaiicakes.clothing.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class SpoolItem extends Item implements DyeableLeatherItem {
    public static final String SPOOL_COLOR_KEY = "spool_color";

    public SpoolItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public int getColor(ItemStack pStack) {
        CompoundTag spoolTag = pStack.getTag();
        return spoolTag != null && spoolTag.contains(SPOOL_COLOR_KEY, Tag.TAG_ANY_NUMERIC)
                ? (int) spoolTag.getLong(SPOOL_COLOR_KEY)
                : ClothingItem.FALLBACK_COLOR;
    }

    @Override
    public void setColor(ItemStack pStack, int pColor) {
        pStack.getOrCreateTag().putLong(SPOOL_COLOR_KEY, pColor);
    }

    @Override
    public boolean hasCustomColor(ItemStack pStack) {
        return this.getColor(pStack) != ClothingItem.FALLBACK_COLOR;
    }

    @Override
    public void clearColor(ItemStack pStack) {
        CompoundTag spoolTag = pStack.getTag();
        if (spoolTag != null && spoolTag.contains(SPOOL_COLOR_KEY, Tag.TAG_ANY_NUMERIC))
            spoolTag.remove(SPOOL_COLOR_KEY);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        pTooltipComponents.add(Component.empty());
        pTooltipComponents.add(
                Component.translatable("item.modifiers.clothing.spool_color").withStyle(ChatFormatting.GRAY)
        );
        pTooltipComponents.add(
                Component.literal(
                        "#" + Integer.toHexString(this.getColor(pStack)).toUpperCase()
                ).withStyle(ChatFormatting.BLUE)
        );
    }
}
