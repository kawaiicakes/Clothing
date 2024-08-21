package io.github.kawaiicakes.clothing.common.data;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static io.github.kawaiicakes.clothing.common.item.ClothingItem.*;
import static io.github.kawaiicakes.clothing.common.item.impl.BakedModelClothingItem.MODEL_PARENTS_KEY;
import static io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem.*;

public class ClothingProperties {
    public static final ClothingProperty<ResourceLocation> NAME = new ClothingProperty<>(CLOTHING_NAME_KEY) {
        @Override
        public ResourceLocation getDefaultValue() {
            return new ResourceLocation(MOD_ID, "default");
        }

        @Override
        public StringTag writeToTag(ResourceLocation property) {
            return StringTag.valueOf(property.toString());
        }

        @Override
        public JsonPrimitive writeToJson(ResourceLocation property) {
            return new JsonPrimitive(property.toString());
        }

        @Override
        public ResourceLocation readFromTag(Tag property) {
            ResourceLocation toReturn = this.getDefaultValue();

            try {
                toReturn = property instanceof StringTag stringTag
                        ? new ResourceLocation(stringTag.getAsString())
                        : this.getDefaultValue();

                return toReturn;
            } catch (Exception e) {
                LOGGER.error("Error reading ResourceLocation from Tag '{}'!", property, e);
                return toReturn;
            }
        }

        @Override
        public ResourceLocation readFromJson(JsonElement property) {
            ResourceLocation toReturn = this.getDefaultValue();

            try {
                toReturn = property.isJsonPrimitive()
                        ? new ResourceLocation(property.getAsJsonPrimitive().getAsString())
                        : this.getDefaultValue();

                return toReturn;
            } catch (Exception e) {
                LOGGER.error("Error reading ResourceLocation from Json '{}'!", property, e);
                return toReturn;
            }
        }
    };

    public static final ClothingProperty<EquipmentSlot> SLOT = new ClothingProperty<>(CLOTHING_SLOT_NBT_KEY) {
        @Override
        public EquipmentSlot getDefaultValue() {
            return EquipmentSlot.CHEST;
        }

        @Override
        public StringTag writeToTag(EquipmentSlot property) {
            return StringTag.valueOf(property.getName());
        }

        @Override
        public JsonPrimitive writeToJson(EquipmentSlot property) {
            return new JsonPrimitive(property.getName());
        }

        @Override
        public EquipmentSlot readFromTag(Tag property) {
            try {
                return property instanceof StringTag stringTag
                        ? EquipmentSlot.byName(stringTag.getAsString())
                        : EquipmentSlot.CHEST;
            } catch (IllegalArgumentException e) {
                LOGGER.error("No such equipment slot '{}' exists!", property, e);
                return EquipmentSlot.CHEST;
            } catch (Exception e) {
                LOGGER.error("Error reading EquipmentSlot from tag '{}'!", property, e);
                return EquipmentSlot.CHEST;
            }
        }

        @Override
        public EquipmentSlot readFromJson(JsonElement property) {
            try {
                return property.isJsonPrimitive()
                        ? EquipmentSlot.byName(property.getAsJsonPrimitive().getAsString())
                        : this.getDefaultValue();
            } catch (IllegalArgumentException e) {
                LOGGER.error("No such equipment slot '{}' exists!", property, e);
                return EquipmentSlot.CHEST;
            } catch (Exception e) {
                LOGGER.error("Error reading EquipmentSlot from JSON element '{}'!", property, e);
                return EquipmentSlot.CHEST;
            }
        }
    };

    public static final ClothingProperty<Integer> COLOR = new ClothingProperty<>(TAG_COLOR) {
        @Override
        public Integer getDefaultValue() {
            return 0xFFFFFF;
        }

        @Override
        public IntTag writeToTag(Integer property) {
            return IntTag.valueOf(property);
        }

        @Override
        public JsonPrimitive writeToJson(Integer property) {
            return new JsonPrimitive(property);
        }

        @Override
        public Integer readFromTag(Tag property) {
            if (!(property instanceof IntTag intTag)) {
                LOGGER.error("Tag is not an IntTag!");
                return this.getDefaultValue();
            }

            return intTag.getAsInt();
        }

        @Override
        public Integer readFromJson(JsonElement property) {
            if (!property.isJsonPrimitive() || property.getAsJsonPrimitive().isNumber()) {
                LOGGER.error("Json is not a number!");
                return this.getDefaultValue();
            }

            return property.getAsJsonPrimitive().getAsInt();
        }
    };

    public static final ClothingProperty<List<Component>> LORE = new ClothingProperty<>(CLOTHING_LORE_NBT_KEY) {
        @Override
        public List<Component> getDefaultValue() {
            return List.of(Component.translatable("item.lore.clothing.error"));
        }

        @Override
        public ListTag writeToTag(List<Component> property) {
            ListTag toReturn = new ListTag();

            try {
                for (Component component : property) {
                    toReturn.add(StringTag.valueOf(Component.Serializer.toJson(component)));
                }
            } catch (Exception e) {
                LOGGER.error("Unable to parse clothing lore!", e);
                return new ListTag();
            }

            return toReturn;
        }

        @Override
        public JsonArray writeToJson(List<Component> property) {
            JsonArray toReturn = new JsonArray(property.size());

            try {
                for (Component component : property) {
                    toReturn.add(Component.Serializer.toJson(component));
                }
            } catch (Exception e) {
                LOGGER.error("Unable to parse clothing lore!", e);
                return new JsonArray();
            }

            return toReturn;
        }

        @Override
        public List<Component> readFromTag(Tag property) {
            if (!(property instanceof ListTag loreTag)) {
                LOGGER.error("Passed tag '{}' is not a ListTag!", property);
                return this.getDefaultValue();
            }

            List<Component> toReturn = new ArrayList<>(loreTag.size());

            try {
                for (Tag componentTag : loreTag) {
                    toReturn.add(Component.Serializer.fromJson(componentTag.getAsString()));
                }
            } catch (Exception e) {
                LOGGER.error("Unable to parse clothing lore!", e);
                toReturn = this.getDefaultValue();
            }

            return toReturn;
        }

        @Override
        public List<Component> readFromJson(JsonElement property) {
            if (!property.isJsonArray()) {
                LOGGER.error("Passed JsonElement '{}' is not a JsonArray!", property);
                return this.getDefaultValue();
            }

            JsonArray loreJson = property.getAsJsonArray();

            List<Component> toReturn = new ArrayList<>(loreJson.size());

            try {
                for (JsonElement element : loreJson) {
                    toReturn.add(Component.Serializer.fromJson(element.getAsJsonPrimitive().getAsString()));
                }
            } catch (Exception e) {
                LOGGER.error("Unable to parse clothing lore!", e);
                toReturn = this.getDefaultValue();
            }

            return toReturn;
        }
    };

    public static final ClothingProperty<Multimap<Attribute, AttributeModifier>> ATTRIBUTES
            = new ClothingProperty<>(ATTRIBUTES_KEY) {
        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultValue() {
            return ImmutableMultimap.of();
        }

        @Override
        public CompoundTag writeToTag(Multimap<Attribute, AttributeModifier> property) {
            CompoundTag toReturn = new CompoundTag();

            for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : property.asMap().entrySet()) {
                ListTag modifierEntries = new ListTag();

                for (AttributeModifier modifier : entry.getValue()) {
                    modifierEntries.add(modifier.save());
                }

                ResourceLocation attributeLocation = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());

                if (attributeLocation == null) {
                    LOGGER.error("Unable to obtain ResourceLocation of Attribute {}!", entry.getKey());
                    continue;
                }

                toReturn.put(attributeLocation.toString(), modifierEntries);
            }

            return toReturn;
        }

        @Override
        public JsonObject writeToJson(Multimap<Attribute, AttributeModifier> property) {
            try {
                CompoundTag tagForSerialization = this.writeToTag(property);

                Set<String> emptyModifierListKeys = new HashSet<>();
                for (String attributeKey : tagForSerialization.getAllKeys()) {
                    ListTag modifierList = tagForSerialization.getList(attributeKey, Tag.TAG_COMPOUND);

                    if (modifierList.isEmpty()) {
                        emptyModifierListKeys.add(attributeKey);
                        continue;
                    }

                    for (Tag tagInList : modifierList) {
                        if (!(tagInList instanceof CompoundTag modifierTag))
                            throw new IllegalStateException("Modifier is not a CompoundTag!");

                        modifierTag.remove("Name");
                        modifierTag.remove("UUID");
                    }

                    tagForSerialization.put(attributeKey, modifierList);
                }
                // this is to avoid ConcurrentModificationExceptions
                emptyModifierListKeys.forEach(tagForSerialization::remove);

                return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tagForSerialization).getAsJsonObject();
            } catch (Exception e) {
                LOGGER.error("Unable to write attributes to JSON!", e);
                return new JsonObject();
            }
        }

        @Override
        public Multimap<Attribute, AttributeModifier> readFromTag(Tag property) {
            try {
                ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

                if (!(property instanceof CompoundTag clothingAttributesTag))
                    throw new IllegalArgumentException("Passed Tag is not a CompoundTag!");

                for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
                    ResourceLocation attributeLocation = ForgeRegistries.ATTRIBUTES.getKey(attribute);
                    if (attributeLocation == null) {
                        LOGGER.error("Unable to obtain ResourceLocation of Attribute {}!", attribute);
                        continue;
                    }
                    if (!clothingAttributesTag.contains(attributeLocation.toString())) continue;

                    ListTag modifierList = clothingAttributesTag.getList(
                            attributeLocation.toString(),
                            Tag.TAG_COMPOUND
                    );

                    List<AttributeModifier> attributeModifiers = new ArrayList<>(modifierList.size());
                    for (Tag tag : modifierList) {
                        if (!(tag instanceof CompoundTag compoundTag)) continue;

                        AttributeModifier modifier = AttributeModifier.load(compoundTag);

                        if (modifier == null) continue;

                        attributeModifiers.add(modifier);
                    }

                    builder.putAll(
                            attribute,
                            attributeModifiers.toArray(AttributeModifier[]::new)
                    );
                }

                return builder.build();
            } catch (Exception e) {
                LOGGER.error("Unable to read attributes from stack!", e);
                return this.getDefaultValue();
            }
        }

        @Override
        public Multimap<Attribute, AttributeModifier> readFromJson(JsonElement property) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            try {
                if (!property.isJsonObject()) throw new IllegalArgumentException("JsonElement is not a JsonObject!");

                JsonObject jsonData = property.getAsJsonObject();

                for (String key : jsonData.keySet()) {
                    Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(key));
                    if (attribute == null) throw new IllegalArgumentException(
                            "Passed JSON contains unknown attribute '" + key + "'!"
                    );

                    if (!(jsonData.get(key) instanceof JsonArray jsonArray))
                        throw new IllegalArgumentException(
                                "Passed JSON does not contain an array for attribute '" + key + "'!"
                        );

                    List<AttributeModifier> forKey = new ArrayList<>(jsonArray.size());

                    int i = 0;
                    for (JsonElement element : jsonArray) {
                        if (!element.isJsonObject()) throw new IllegalArgumentException(
                                "Passed JSON has non-object in attribute array for '" + key + "'!"
                        );

                        CompoundTag attributeModifierTag
                                = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, element);

                        String attributeName = key + "." + i;

                        UUID slotUUID = Mth.createInsecureUUID(RandomSource.createNewThreadLocalInstance());

                        attributeModifierTag.putUUID(
                                "UUID",
                                slotUUID
                        );

                        attributeModifierTag.putString("Name", attributeName);

                        AttributeModifier modifier = AttributeModifier.load(attributeModifierTag);

                        if (modifier == null) throw new IllegalStateException(
                                "Unable to load AttributeModifier from tag '" + attributeModifierTag + "'!"
                        );

                        forKey.add(modifier);
                        i++;
                    }

                    builder.putAll(attribute, forKey.toArray(AttributeModifier[]::new));
                }
            } catch (Exception e) {
                LOGGER.error("Error deserializing clothing attributes!", e);
                return this.getDefaultValue();
            }

            return builder.build();
        }
    };

    public static final ClothingProperty<SoundEvent> EQUIP_SOUND = new ClothingProperty<>(EQUIP_SOUND_KEY) {
        @Override
        public SoundEvent getDefaultValue() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public StringTag writeToTag(SoundEvent property) {
            return StringTag.valueOf(property.getLocation().toString());
        }

        @Override
        public JsonPrimitive writeToJson(SoundEvent property) {
            return new JsonPrimitive(property.getLocation().toString());
        }

        @Override
        public SoundEvent readFromTag(Tag property) {
            if (!(property instanceof StringTag stringTag)) {
                LOGGER.error("Tag '{}' is not a StringTag!", property);
                return this.getDefaultValue();
            }

            SoundEvent toReturn = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(stringTag.getAsString()));

            return toReturn != null
                    ? toReturn
                    : this.getDefaultValue();
        }

        @Override
        public SoundEvent readFromJson(JsonElement property) {
            if (!property.isJsonPrimitive()) {
                LOGGER.error("JsonElement '{}' is not a JsonPrimitive!", property);
                return this.getDefaultValue();
            }

            SoundEvent toReturn = ForgeRegistries.SOUND_EVENTS.getValue(
                    new ResourceLocation(property.getAsJsonPrimitive().getAsString())
            );

            return toReturn != null
                    ? toReturn
                    : this.getDefaultValue();
        }
    };

    public static final ClothingProperty<Integer> MAX_DAMAGE = new ClothingProperty<>(MAX_DAMAGE_KEY) {
        @Override
        public Integer getDefaultValue() {
            return 100;
        }

        @Override
        public IntTag writeToTag(Integer property) {
            return IntTag.valueOf(property);
        }

        @Override
        public JsonPrimitive writeToJson(Integer property) {
            return new JsonPrimitive(property);
        }

        @Override
        public Integer readFromTag(Tag property) {
            if (!(property instanceof IntTag intTag)) {
                LOGGER.error("Tag is not an IntTag!");
                return this.getDefaultValue();
            }

            return intTag.getAsInt();
        }

        @Override
        public Integer readFromJson(JsonElement property) {
            if (!property.isJsonPrimitive() || property.getAsJsonPrimitive().isNumber()) {
                LOGGER.error("Json is not a number!");
                return this.getDefaultValue();
            }

            return property.getAsJsonPrimitive().getAsInt();
        }
    };

    public static final ClothingProperty<ModelStrata> MODEL_LAYER = new ClothingProperty<>(MODEL_LAYER_NBT_KEY) {
        @Override
        public ModelStrata getDefaultValue() {
            return ModelStrata.BASE;
        }

        @Override
        public StringTag writeToTag(ModelStrata property) {
            return StringTag.valueOf(property.getSerializedName());
        }

        @Override
        public JsonPrimitive writeToJson(ModelStrata property) {
            return new JsonPrimitive(property.getSerializedName());
        }

        @Override
        public ModelStrata readFromTag(Tag property) {
            if (!(property instanceof StringTag stringTag)) {
                LOGGER.error("Passed Tag '{}' is not a StringTag!", property);
                return this.getDefaultValue();
            }

            ModelStrata strata;
            try {
                strata = ModelStrata.byName(stringTag.getAsString());
            } catch (Exception e) {
                LOGGER.error("No such ModelStrata as '{}'!", stringTag.getAsString(), e);
                strata = this.getDefaultValue();
            }
            return strata;
        }

        @Override
        public ModelStrata readFromJson(JsonElement property) {
            if (!property.isJsonPrimitive()) {
                LOGGER.error("Passed Json '{}' is not a JsonPrimitive!", property);
                return this.getDefaultValue();
            }

            ModelStrata strata;
            try {
                strata = ModelStrata.byName(property.getAsJsonPrimitive().getAsString());
            } catch (Exception e) {
                LOGGER.error("No such ModelStrata as '{}'!", property.getAsJsonPrimitive().getAsString(), e);
                strata = this.getDefaultValue();
            }
            return strata;
        }
    };

    public static final ClothingProperty<ResourceLocation> TEXTURE_LOCATION
            = new ClothingProperty<>(TEXTURE_LOCATION_NBT_KEY) {
        @Override
        public ResourceLocation getDefaultValue() {
            return new ResourceLocation(MOD_ID, "default");
        }

        @Override
        public StringTag writeToTag(ResourceLocation property) {
            return StringTag.valueOf(property.toString());
        }

        @Override
        public JsonPrimitive writeToJson(ResourceLocation property) {
            return new JsonPrimitive(property.toString());
        }

        @Override
        public ResourceLocation readFromTag(Tag property) {
            ResourceLocation toReturn = this.getDefaultValue();

            try {
                toReturn = property instanceof StringTag stringTag
                        ? new ResourceLocation(stringTag.getAsString())
                        : this.getDefaultValue();

                if (toReturn.getPath().isEmpty())
                    toReturn = new ResourceLocation(MOD_ID, "default");

                return toReturn;
            } catch (Exception e) {
                LOGGER.error("Error reading ResourceLocation from Tag '{}'!", property, e);
                return toReturn;
            }
        }

        @Override
        public ResourceLocation readFromJson(JsonElement property) {
            ResourceLocation toReturn = this.getDefaultValue();

            try {
                toReturn = property.isJsonPrimitive()
                        ? new ResourceLocation(property.getAsJsonPrimitive().getAsString())
                        : this.getDefaultValue();

                if (toReturn.getPath().isEmpty())
                    toReturn = new ResourceLocation(MOD_ID, "default");

                return toReturn;
            } catch (Exception e) {
                LOGGER.error("Error reading ResourceLocation from Json '{}'!", property, e);
                return toReturn;
            }
        }
    };

    public static final ClothingProperty<ResourceLocation[]> OVERLAYS = new ClothingProperty<>(OVERLAY_NBT_KEY) {
        @Override
        public ResourceLocation[] getDefaultValue() {
            return new ResourceLocation[0];
        }

        @Override
        public ListTag writeToTag(ResourceLocation[] property) {
            ListTag toReturn = new ListTag();

            for (ResourceLocation overlay : property) {
                toReturn.add(StringTag.valueOf(overlay.toString()));
            }

            return toReturn;
        }

        @Override
        public JsonArray writeToJson(ResourceLocation[] property) {
            JsonArray toReturn = new JsonArray(property.length);

            for (ResourceLocation overlay : property) {
                toReturn.add(overlay.toString());
            }

            return toReturn;
        }

        @Override
        public ResourceLocation[] readFromTag(Tag property) {
            if (!(property instanceof ListTag listTag)) {
                LOGGER.error("Passed Tag is not a ListTag!");
                return this.getDefaultValue();
            }

            try {
                ResourceLocation[] toReturn = new ResourceLocation[listTag.size()];

                for (int i = 0; i < listTag.size(); i++) {
                    toReturn[i] = new ResourceLocation(listTag.getString(i));
                }

                return toReturn;
            } catch (Exception e) {
                LOGGER.error("Unable to read ResourceLocation[] from Tag '{}'!", property, e);
                return this.getDefaultValue();
            }
        }

        @Override
        public ResourceLocation[] readFromJson(JsonElement property) {
            if (!property.isJsonArray()) {
                LOGGER.error("Passed Json is not a JsonArray!");
                return this.getDefaultValue();
            }

            try {
                ResourceLocation[] toReturn = new ResourceLocation[property.getAsJsonArray().size()];

                for (int i = 0; i < property.getAsJsonArray().size(); i++) {
                    toReturn[i] = new ResourceLocation(
                            property.getAsJsonArray().get(i).getAsJsonPrimitive().getAsString()
                    );
                }

                return toReturn;
            } catch (Exception e) {
                LOGGER.error("Unable to read ResourceLocation[] from Tag '{}'!", property, e);
                return this.getDefaultValue();
            }
        }
    };

    public static final ClothingProperty<ModelPartReference[]> VISIBLE_PARTS
            = new ClothingProperty<>(PART_VISIBILITY_KEY) {
        @Override
        public ModelPartReference[] getDefaultValue() {
            return new ModelPartReference[0];
        }

        @Override
        public ListTag writeToTag(ModelPartReference[] property) {
            ListTag toReturn = new ListTag();

            for (ModelPartReference part : property) {
                toReturn.add(StringTag.valueOf(part.getSerializedName()));
            }

            return toReturn;
        }

        @Override
        public JsonArray writeToJson(ModelPartReference[] property) {
            JsonArray toReturn = new JsonArray(property.length);

            for (ModelPartReference part : property) {
                toReturn.add(part.getSerializedName());
            }

            return toReturn;
        }

        @Override
        public ModelPartReference[] readFromTag(Tag property) {
            if (!(property instanceof ListTag listTag)) {
                LOGGER.error("Passed Tag '{}' is not a StringTag!", property);
                return this.getDefaultValue();
            }

            ModelPartReference[] toReturn = new ModelPartReference[listTag.size()];
            try {
                for (int i = 0; i < listTag.size(); i++) {
                    toReturn[i] = ModelPartReference.byName(listTag.getString(i));
                }
            } catch (Exception e) {
                LOGGER.error("Error deserializing ModelPartReference[] from Tag '{}'!", listTag, e);
                toReturn = this.getDefaultValue();
            }
            return toReturn;
        }

        @Override
        public ModelPartReference[] readFromJson(JsonElement property) {
            if (!property.isJsonArray()) {
                LOGGER.error("Passed Json '{}' is not a JsonArray!", property);
                return this.getDefaultValue();
            }

            JsonArray array = property.getAsJsonArray();

            ModelPartReference[] toReturn = new ModelPartReference[array.size()];
            try {
                for (int i = 0; i < array.size(); i++) {
                    toReturn[i] = ModelPartReference.byName(array.get(i).getAsJsonPrimitive().getAsString());
                }
            } catch (Exception e) {
                LOGGER.error("Error deserializing ModelPartReference[] from Tag '{}'!", array, e);
                toReturn = this.getDefaultValue();
            }
            return toReturn;
        }
    };

    public static final ClothingProperty<Map<ModelPartReference, ResourceLocation>> MODEL_PARENTS
            = new ClothingProperty<>(MODEL_PARENTS_KEY) {
        @Override
        public Map<ModelPartReference, ResourceLocation> getDefaultValue() {
            return Map.of();
        }

        @Override
        public CompoundTag writeToTag(Map<ModelPartReference, ResourceLocation> property) {
            CompoundTag toReturn = new CompoundTag();

            for (Map.Entry<ModelPartReference, ResourceLocation> entry : property.entrySet()) {
                toReturn.putString(entry.getKey().getSerializedName(), entry.getValue().toString());
            }

            return toReturn;
        }

        @Override
        public JsonObject writeToJson(Map<ModelPartReference, ResourceLocation> property) {
            JsonObject toReturn = new JsonObject();

            for (Map.Entry<ModelPartReference, ResourceLocation> entry : property.entrySet()) {
                toReturn.add(entry.getKey().getSerializedName(), new JsonPrimitive(entry.getValue().toString()));
            }

            return toReturn;
        }

        @Override
        public Map<ModelPartReference, ResourceLocation> readFromTag(Tag property) {
            try {
                if (!(property instanceof CompoundTag modelPartTag))
                    throw new IllegalArgumentException("Passed Tag is not a CompoundTag!");

                Map<ModelPartReference, ResourceLocation> toReturn = new HashMap<>(modelPartTag.size());

                for (String part : modelPartTag.getAllKeys()) {
                    if (!(modelPartTag.get(part) instanceof StringTag modelLocation))
                        throw new IllegalArgumentException();
                    toReturn.put(ModelPartReference.byName(part), new ResourceLocation(modelLocation.toString()));
                }

                return toReturn;
            } catch (Exception e) {
                LOGGER.error("Unable to read model parents from Tag '{}'!", property, e);
                return this.getDefaultValue();
            }
        }

        @Override
        public Map<ModelPartReference, ResourceLocation> readFromJson(JsonElement property) {
            try {
                JsonObject modelObject = property.getAsJsonObject();

                Map<ModelPartReference, ResourceLocation> toReturn = new HashMap<>();

                for (String key : modelObject.keySet()) {
                    ModelPartReference byName = ModelPartReference.byName(key);
                    ResourceLocation modelLocation
                            = new ResourceLocation(modelObject.getAsJsonPrimitive(key).getAsString());

                    toReturn.put(byName, modelLocation);
                }

                return toReturn;
            } catch (Exception e) {
                LOGGER.error("Unable to read model parents from Tag '{}'!", property, e);
                return this.getDefaultValue();
            }
        }
    };

    public static void writeClothingStackToJson(JsonObject json, ItemStack clothing) {
        if (!(clothing.getItem() instanceof ClothingItem<?> clothingItem))
            throw new IllegalArgumentException("Passed ItemStack '{}' is not a piece of clothing!");

        ClothingProperties.NAME.writePropertyToJson(
                json, ClothingProperties.NAME.readPropertyFromStack(clothing)
        );

        ClothingProperties.SLOT.writePropertyToJson(
                json, ClothingProperties.SLOT.readPropertyFromStack(clothing)
        );

        ClothingProperties.COLOR.writePropertyToJson(
                json, ClothingProperties.COLOR.readPropertyFromStack(clothing)
        );

        ClothingProperties.LORE.writePropertyToJson(
                json, ClothingProperties.LORE.readPropertyFromStack(clothing)
        );

        ClothingProperties.ATTRIBUTES.writePropertyToJson(
                json, ClothingProperties.ATTRIBUTES.readPropertyFromStack(clothing)
        );

        ClothingProperties.EQUIP_SOUND.writePropertyToJson(
                json, ClothingProperties.EQUIP_SOUND.readPropertyFromStack(clothing)
        );

        ClothingProperties.MAX_DAMAGE.writePropertyToJson(
                json, ClothingProperties.MAX_DAMAGE.readPropertyFromStack(clothing)
        );

        if (clothingItem instanceof GenericClothingItem) {
            ClothingProperties.MODEL_LAYER.writePropertyToJson(
                    json, ClothingProperties.MODEL_LAYER.readPropertyFromStack(clothing)
            );

            ClothingProperties.TEXTURE_LOCATION.writePropertyToJson(
                    json, ClothingProperties.TEXTURE_LOCATION.readPropertyFromStack(clothing)
            );

            ClothingProperties.OVERLAYS.writePropertyToJson(
                    json, ClothingProperties.OVERLAYS.readPropertyFromStack(clothing)
            );

            ClothingProperties.VISIBLE_PARTS.writePropertyToJson(
                    json, ClothingProperties.VISIBLE_PARTS.readPropertyFromStack(clothing)
            );
        } else {
            ClothingProperties.MODEL_PARENTS.writePropertyToJson(
                    json, ClothingProperties.MODEL_PARENTS.readPropertyFromStack(clothing)
            );
        }
    }
}
