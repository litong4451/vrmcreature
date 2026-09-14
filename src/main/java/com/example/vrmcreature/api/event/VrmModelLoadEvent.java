package com.example.vrmcreature.api.event;

import com.example.vrmcreature.entity.client.VrmCreatureel;
import net.neoforged.bus.api.Event;

/**
 * VRM 模型加载钩子（客户端）。
 *
 * 在某模型从磁盘 .vrm/.glb 解析完成之后触发。
 * {@link #getModel()} 返回的 {@link VrmCreatureel} 为可变对象（网格/节点/动画/贴图均为公开字段），
 * 监听者可直接修改：替换网格、增删动画片段、改节点层级等，修改结果立即影响后续渲染。
 *
 * 典型用法：为模型动态注入额外动画片段、或对特定模型做渲染前修正。
 */
public class VrmModelLoadEvent extends Event {

    private final String modelName;
    private final VrmCreatureel model;

    public VrmModelLoadEvent(String modelName, VrmCreatureel model) {
        this.modelName = modelName;
        this.model = model;
    }

    /** 被加载的模型名 */
    public String getModelName() {
        return modelName;
    }

    /** 已解析完成的模型数据（可直接修改，影响后续渲染） */
    public VrmCreatureel getModel() {
        return model;
    }
}
