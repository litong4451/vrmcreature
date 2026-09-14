package com.example.vrmcreature.entity;

import com.example.vrmcreature.api.event.VrmBehaviorEvent;
import com.example.vrmcreature.api.event.VrmLootDropEvent;
import com.example.vrmcreature.config.VrmModelConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

/**
 * VRM 自定义生物实体。
 * 每个模型对应一份独立配置（versions/<版本名>/vrmcreature/config/<模型名>.json），
 * 实体按其 modelName 读取对应配置：阵营、基础属性、行为 AI、掉落物。
 */
public class VrmMob extends Mob {

    /** 模型名（对应 <游戏目录>/versions/<版本名>/vrmcreature/vrm/<name>.vrm 或 .glb），默认 model */
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

    /** 读取当前模型对应的配置（文件缺失时返回默认配置） */
    private VrmModelConfig.Data config() {
        return VrmModelConfig.load(getModelName());
    }

    /**
     * 应用某模型的完整配置：设置属性并重注册行为 AI。
     * 在生成实体（手动生成 / 自然刷新）时调用，确保属性与行为按该模型独立配置生效。
     */
    public void applyModelConfig(String modelName) {
        setModelName(modelName);
        VrmModelConfig.Data d = VrmModelConfig.load(modelName);
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(d.health);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(d.damage);
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(d.speed);
        }
        this.setHealth((float) d.health);
        // 重新注册行为 AI（按该模型配置）
        this.goalSelector.removeAllGoals();
        this.targetSelector.removeAllGoals();
        this.registerGoals();
    }

    /** 死亡掉落：按该模型配置的概率掉落对应物品（每次独立判定） */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);
        VrmModelConfig.Data d = config();
        // 掉落钩子：允许依赖方修改掉落表或取消默认掉落
        List<String> lootLines = new ArrayList<>(d.loot);
        VrmLootDropEvent event = new VrmLootDropEvent(this, lootLines);
        if (NeoForge.EVENT_BUS.post(event)) {
            return;
        }
        for (String line : lootLines) {
            String[] p = line.split(";");
            if (p.length < 3) {
                continue;
            }
            String id = p[0].trim();
            int count;
            double chance;
            try {
                count = Integer.parseInt(p[1].trim());
                chance = Double.parseDouble(p[2].trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (this.random.nextDouble() > chance) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
            if (item == null || item == Items.AIR) {
                continue;
            }
            ItemStack stack = new ItemStack(item, count);
            if (stack.isEmpty()) {
                continue;
            }
            float f = this.random.nextFloat() * 0.5F + 0.25F;
            ItemEntity drop = new ItemEntity(this.level(), this.getX() + f, this.getY() + 0.5D, this.getZ() + f, stack);
            drop.setDefaultPickUpDelay();
            this.level().addFreshEntity(drop);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        VrmModelConfig.Data d = config();
        int behavior = d.behavior;
        boolean hostile = d.faction == 0;

        // 行为钩子：依赖方可取消默认 AI，完全自定义注册 goal / target
        VrmBehaviorEvent behaviorEvent = new VrmBehaviorEvent(this, behavior, hostile);
        if (NeoForge.EVENT_BUS.post(behaviorEvent)) {
            return;
        }

        this.goalSelector.addGoal(0, new FloatGoal(this));

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
