package io.github.kawaiicakes.clothing.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

public class ClothingModel<T extends LivingEntity> extends HumanoidModel<T> {
    public ClothingModel(ModelPart pRoot) {
        super(pRoot);
    }
}
