package io.github.kawaiicakes.clothing.common.data;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClothingOverlayGenerator implements DataProvider {
    protected static final Logger LOGGER = LogUtils.getLogger();

    public final DataGenerator dataGenerator;
    public final ExistingFileHelper fileHelper;
    public final String modId;
    public final DataGenerator.PathProvider overlayPath;
    public ImmutableList<OverlayDefinitionLoader.OverlayDefinition> overlays;

    public ClothingOverlayGenerator(DataGenerator dataGenerator, ExistingFileHelper fileHelper, String modId) {
        this.dataGenerator = dataGenerator;
        this.fileHelper = fileHelper;
        this.modId = modId;
        this.overlayPath = this.dataGenerator.createPathProvider(
                DataGenerator.Target.DATA_PACK, "clothing/generic/overlays"
        );
    }

    public ImmutableList<OverlayDefinitionLoader.OverlayDefinition> getOverlays() {
        return ImmutableList.copyOf(this.overlays);
    }

    public void registerOverlays() {
        this.addOverlays(
                OverlayDefinitionLoader.OverlayDefinitionBuilder.of("ouch")
                        .addSlot(EquipmentSlot.CHEST)
                        .build(),
                OverlayDefinitionLoader.OverlayDefinitionBuilder.of("oppai")
                        .addSlot(EquipmentSlot.CHEST)
                        .build()
        );
    }

    @Override
    public void run(@NotNull CachedOutput pOutput) {
        if (this.overlays == null || this.overlays.isEmpty()) this.registerOverlays();

        Set<String> overlayIdSet = new HashSet<>();

        for (OverlayDefinitionLoader.OverlayDefinition overlay : this.overlays) {
            try {
                if (!overlayIdSet.add(overlay.name()))
                    throw new IllegalStateException("Duplicate overlay " + overlay.name());

                Path overlayPath = this.overlayPath.json(new ResourceLocation(this.modId, overlay.name()));

                DataProvider.saveStable(pOutput, overlay.serializeToJson(), overlayPath);
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Couldn't save overlay {}!", overlay.name(), e);
            }
        }
    }

    public void addOverlay(OverlayDefinitionLoader.OverlayDefinition overlay) {
        ImmutableList.Builder<OverlayDefinitionLoader.OverlayDefinition> builder
                = ImmutableList.builder();

        if (this.overlays == null || this.overlays.isEmpty()) {
            this.overlays = ImmutableList.of(overlay);
        } else {
            builder.addAll(this.overlays);
            builder.add(overlay);

            this.overlays = builder.build();
        }
    }

    public void addOverlays(OverlayDefinitionLoader.OverlayDefinition... overlays) {
        ImmutableList.Builder<OverlayDefinitionLoader.OverlayDefinition> builder
                = ImmutableList.builder();

        if (this.overlays == null || this.overlays.isEmpty()) {
            this.overlays = ImmutableList.copyOf(overlays);
        } else {
            builder.addAll(this.overlays);
            builder.addAll(Arrays.asList(overlays));

            this.overlays = builder.build();
        }
    }

    @Override
    public @NotNull String getName() {
        return "Clothing Overlays: " + " mod id - " + this.modId;
    }
}
