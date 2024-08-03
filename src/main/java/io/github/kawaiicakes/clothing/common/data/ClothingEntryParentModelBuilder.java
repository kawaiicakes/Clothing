package io.github.kawaiicakes.clothing.common.data;

import com.google.gson.JsonObject;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.client.model.generators.loaders.CompositeModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ClothingEntryParentModelBuilder<T extends ModelBuilder<T>> extends CompositeModelBuilder<T> {
    public static <T extends ModelBuilder<T>> ClothingEntryParentModelBuilder<T>
    begin(T parent, ExistingFileHelper existingFileHelper)
    {
        return new ClothingEntryParentModelBuilder<>(parent, existingFileHelper);
    }

    protected ClothingEntryParentModelBuilder(T parent, ExistingFileHelper existingFileHelper) {
        super(parent, existingFileHelper);
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        JsonObject toReturn = super.toJson(json);

        if (toReturn.getAsJsonObject("children").keySet().isEmpty()) {
            toReturn.remove("children");
            toReturn.remove("loader");
        }
        if (toReturn.getAsJsonArray("item_render_order").isEmpty())
            toReturn.remove("item_render_order");

        return toReturn;
    }
}
