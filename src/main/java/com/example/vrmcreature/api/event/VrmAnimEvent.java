package com.example.vrmcreature.api.event;

import com.example.vrmcreature.entity.VrmMob;
import com.example.vrmcreature.entity.client.VrmCreatureel;
import net.neoforged.bus.api.Cancelable;
import net.neoforged.bus.api.Event;

/**
 * VRM 生物动作（动画）选择钩子（客户端）。
 *
 * 在每次为实体选择「本帧应播放的动画片段」时触发（即 {@code VrmMobRenderer.selectClip} 内部）。
 * 监听者可：
 *  - 直接 {@link #setClip(VrmCreatureel.AnimationClip)} 或 {@link #setClipByName(String)} 覆盖要播放的动画；
 *  - {@link #setCanceled(boolean)}：取消默认动画选择——此时若未另行指定片段，本帧将不播放动画，
 *    可配合自行驱动动画（如通过 {@link VrmModelLoadEvent} 修改模型）实现完全自定义的动作逻辑。
 *
 * 不取消且不指定时，维持默认的自动选择（移动→walk / 攻击→attack / 静止→idle 或按动作偏好）。
 */
@Cancelable
public class VrmAnimEvent extends Event {

    private final VrmMob mob;
    private final VrmCreatureel model;
    private final VrmCreatureel.AnimationClip defaultClip;
    private VrmCreatureel.AnimationClip clip;

    public VrmAnimEvent(VrmMob mob, VrmCreatureel model, VrmCreatureel.AnimationClip defaultClip) {
        this.mob = mob;
        this.model = model;
        this.defaultClip = defaultClip;
        this.clip = defaultClip;
    }

    /** 本帧要渲染的实体 */
    public VrmMob getMob() {
        return mob;
    }

    /** 实体当前使用的模型数据（含全部动画片段列表 {@link VrmCreatureel#animations}） */
    public VrmCreatureel getModel() {
        return model;
    }

    /** 默认自动选择出的动画片段（可能为 null） */
    public VrmCreatureel.AnimationClip getDefaultClip() {
        return defaultClip;
    }

    /** 最终确定要播放的动画片段（null 表示不播放） */
    public VrmCreatureel.AnimationClip getClip() {
        return clip;
    }

    /** 强制指定本帧播放的动画片段 */
    public void setClip(VrmCreatureel.AnimationClip clip) {
        this.clip = clip;
    }

    /** 按片段名从模型的动画列表中指定要播放的片段（不区分大小写，找不到则保持原值） */
    public void setClipByName(String name) {
        if (name == null || name.isEmpty() || model == null) {
            return;
        }
        for (VrmCreatureel.AnimationClip c : model.animations) {
            if (name.equalsIgnoreCase(c.name)) {
                this.clip = c;
                return;
            }
        }
    }
}
