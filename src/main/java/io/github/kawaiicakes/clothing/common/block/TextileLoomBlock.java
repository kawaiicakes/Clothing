package io.github.kawaiicakes.clothing.common.block;

import io.github.kawaiicakes.clothing.common.menu.TextileLoomMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class TextileLoomBlock extends HorizontalDirectionalBlock {
    protected TextileLoomBlock() {
        super(Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD));
    }

    @Override
    @ParametersAreNonnullByDefault
    @SuppressWarnings("deprecation")
    public MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new TextileLoomMenu(
                        containerId, inventory, ContainerLevelAccess.create(pLevel, pPos)
                ),
                Component.translatable("block.clothing.textile_loom")
        );
    }

    @Override
    @ParametersAreNonnullByDefault
    @SuppressWarnings("deprecation")
    public @NotNull InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult result
    ) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, state.getMenuProvider(level, pos));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }
}
