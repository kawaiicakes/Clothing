package io.github.kawaiicakes.clothing.common.network;

import com.google.common.collect.ImmutableList;
import io.github.kawaiicakes.clothing.common.resources.BakedClothingResourceLoader;
import io.github.kawaiicakes.clothing.common.resources.ClothingResourceLoader;
import io.github.kawaiicakes.clothing.common.resources.GenericClothingResourceLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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

        net.messageBuilder(S2CClothingPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CClothingPacket::new)
                .encoder(S2CClothingPacket::toBytes)
                .consumerMainThread(ClothingPackets::handleOnClient)
                .add();
    }

    protected static void handleOnClient(S2CClothingPacket msg, Supplier<NetworkEvent.Context> event) {
        event.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> msg.handle(event)));
        event.get().setPacketHandled(true);
    }

    public static <MSG> void sendToPlayer(MSG msg, @Nullable ServerPlayer player) {
        if (player == null) {
            INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
            return;
        }
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static class S2CClothingPacket {
        protected final String loaderClass;
        protected final ImmutableList<CompoundTag> clothingEntries;

        public S2CClothingPacket(ClothingResourceLoader<?> clothingResourceLoader) {
            this.loaderClass = clothingResourceLoader.getName();
            this.clothingEntries = clothingResourceLoader.getStacks();
        }

        public S2CClothingPacket(FriendlyByteBuf buf) {
            this.loaderClass = buf.readUtf();
            this.clothingEntries = ImmutableList.copyOf(
                    buf.readList(FriendlyByteBuf::readAnySizeNbt)
            );
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(this.loaderClass);
            buf.writeCollection(
                    this.clothingEntries,
                    FriendlyByteBuf::writeNbt
            );
        }

        @SuppressWarnings("unused")
        public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            if (!contextSupplier.get().getDirection().equals(NetworkDirection.PLAY_TO_CLIENT)) return;

            // TODO: redo this, damn this is scuffed lmao
            switch (this.loaderClass) {
                case "generic": GenericClothingResourceLoader.getInstance().addStacks(this.clothingEntries);
                case "baked": BakedClothingResourceLoader.getInstance().addStacks(this.clothingEntries);
                default: throw new IllegalArgumentException("Unrecognized loader '" + this.loaderClass + "'!");
            }
        }
    }
}
