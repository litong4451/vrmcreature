package com.example.vrmcreature.entity;

import com.example.vrmcreature.config.VrmCreatureConfig;
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

    public VrmMob(EntityType<? extends VrmMob> type, Level level) {
        super(type, level);
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
