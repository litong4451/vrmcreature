package com.example.vrmcreature.entity;

import com.example.vrmcreature.VrmCreature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, VrmCreature.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<VrmMob>> VRM_MOB =
            ENTITIES.register("vrm_mob",
                    () -> EntityType.Builder.of(VrmMob::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .build("vrm_mob"));
}
