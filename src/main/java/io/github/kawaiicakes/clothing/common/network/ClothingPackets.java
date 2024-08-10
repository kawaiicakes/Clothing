package io.github.kawaiicakes.clothing.common.network;

import com.google.common.collect.ImmutableList;
import io.github.kawaiicakes.clothing.common.resources.ClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class ClothingPackets {
    private static SimpleChannel INSTANCE;
    private static int PACKET_ID = 0;
    private static int id() {
        return PACKET_ID++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(S2CClothingEntryPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CClothingEntryPacket::new)
                .encoder(S2CClothingEntryPacket::toBytes)
                .consumerMainThread(S2CClothingEntryPacket::handle)
                .add();

        net.messageBuilder(S2COverlayPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2COverlayPacket::new)
                .encoder(S2COverlayPacket::toBytes)
                .consumerMainThread(S2COverlayPacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG msg, @Nullable ServerPlayer player) {
        if (player == null) {
            INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
            return;
        }
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static class S2CClothingEntryPacket {
        protected final String loaderClass;
        protected final NonNullList<ItemStack> clothingEntries;

        public S2CClothingEntryPacket(ClothingEntryLoader<?> clothingEntryLoader) {
            this.loaderClass = clothingEntryLoader.getName();
            this.clothingEntries = clothingEntryLoader.generateStacks();
        }

        public S2CClothingEntryPacket(FriendlyByteBuf buf) {
            this.loaderClass = buf.readUtf();
            NonNullList<ItemStack> stacks = NonNullList.create();
            stacks.addAll(buf.readList(FriendlyByteBuf::readItem));
            this.clothingEntries = stacks;
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(this.loaderClass);
            buf.writeCollection(
                    this.clothingEntries,
                    FriendlyByteBuf::writeItem
            );
        }

        public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            if (!contextSupplier.get().getDirection().equals(NetworkDirection.PLAY_TO_CLIENT)) return;

            contextSupplier.get().enqueueWork(
                    () -> DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> {
                                ClothingEntryLoader<?> clothingEntryLoader;
                                clothingEntryLoader = ClothingEntryLoader.getLoader(this.loaderClass);
                                if (clothingEntryLoader == null) return;

                                clothingEntryLoader.addStacks(this.clothingEntries);
                            }
                    )
            );

            contextSupplier.get().setPacketHandled(true);
        }
    }

    public static class S2COverlayPacket {
        protected final ImmutableList<OverlayDefinitionLoader.OverlayDefinition> overlayDefinitions;

        public S2COverlayPacket(OverlayDefinitionLoader loader) {
            this.overlayDefinitions = loader.getOverlays();
        }

        public S2COverlayPacket(FriendlyByteBuf buf) {
            this.overlayDefinitions = ImmutableList.copyOf(
                    buf.readList(OverlayDefinitionLoader.OverlayDefinition::deserializeFromNetwork)
            );
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeCollection(this.overlayDefinitions, OverlayDefinitionLoader.OverlayDefinition::serializeToNetwork);
        }

        public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            if (!contextSupplier.get().getDirection().equals(NetworkDirection.PLAY_TO_CLIENT)) return;

            contextSupplier.get().enqueueWork(
                    () -> DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> new OverlayDefinitionLoader().addOverlays(this.overlayDefinitions)
                    )
            );

            contextSupplier.get().setPacketHandled(true);
        }
    }
}
