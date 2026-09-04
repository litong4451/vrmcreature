package com.example.vrmcreature.entity;

import com.example.vrmcreature.config.VrmCreatureConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * VRM 自定义生物实体。
 * 阵营与行为均由配置文件决定（创建时设置，锁定后不可改）。
 */
public class VrmMob extends Mob {

    /** 模型名（对应 assets/vrmcreature/vrm/<name>.vrm 或 .glb），默认 model */
    private static final EntityDataAccessor<String> DATA_MODEL_NAME =
            SynchedEntityData.defineId(VrmMob.class, EntityDataSerializers.STRING);

    public VrmMob(EntityType<? extends VrmMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODEL_NAME, "model");
    }

    public String getModelName() {
        return this.entityData.get(DATA_MODEL_NAME);
    }

    public void setModelName(String name) {
        if (name != null && !name.isEmpty()) {
            this.entityData.set(DATA_MODEL_NAME, name);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("ModelName", getModelName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ModelName")) {
            setModelName(tag.getString("ModelName"));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, VrmCreatureConfig.MAX_HEALTH.get())
                .add(Attributes.ATTACK_DAMAGE, VrmCreatureConfig.ATTACK_DAMAGE.get())
                .add(Attributes.MOVEMENT_SPEED, VrmCreatureConfig.MOVEMENT_SPEED.get())
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        int behavior = VrmCreatureConfig.BEHAVIOR.get();
        boolean hostile = VrmCreatureConfig.getFaction() == 0;

        switch (behavior) {
            case 0 -> {
                // 静止：原地不动
            }
            case 2 -> {
                // 跟随玩家
                this.goalSelector.addGoal(1, new FollowPlayerGoal(this, 1.0D, 3.0F, 16.0F));
            }
            case 3 -> {
                // 主动攻击
                if (hostile) {
                    this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
                    this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
                } else {
                    // 中立/我方被攻击后反击
                    this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
                    this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
                }
            }
            default -> {
                // 巡逻（行为1）
                this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0D));
            }
        }

        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }
}
