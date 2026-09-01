package com.example.vrmcreature.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * 跟随玩家：保持在 [minDist, maxDist] 之间，过远则靠近。
 */
public class FollowPlayerGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private final float minDist;
    private final float maxDist;
    private int delay = 0;

    public FollowPlayerGoal(Mob mob, double speedModifier, float minDist, float maxDist) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.minDist = minDist;
        this.maxDist = maxDist;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.delay-- > 0) return false;
        this.delay = 20;
        Player player = this.mob.level().getNearestPlayer(this.mob, this.maxDist);
        if (player == null) return false;
        double d = this.mob.distanceToSqr(player);
        return d > this.minDist * this.minDist && d < this.maxDist * this.maxDist;
    }

    @Override
    public boolean canContinueToUse() {
        Player player = this.mob.level().getNearestPlayer(this.mob, this.maxDist + 2.0F);
        if (player == null) return false;
        return this.mob.distanceToSqr(player) > this.minDist * this.minDist * 0.8F;
    }

    @Override
    public void tick() {
        Player player = this.mob.level().getNearestPlayer(this.mob, this.maxDist);
        if (player == null) return;
        if (this.mob.distanceToSqr(player) > this.minDist * this.minDist) {
            this.mob.getNavigation().moveTo(player, this.speedModifier);
        }
        this.mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
    }
}
