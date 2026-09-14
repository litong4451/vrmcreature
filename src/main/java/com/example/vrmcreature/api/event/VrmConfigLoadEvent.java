package com.example.vrmcreature.api.event;

import com.example.vrmcreature.config.VrmModelConfig;
import net.neoforged.bus.api.Event;

/**
 * VRM 模型配置读取钩子（服务端 + 客户端通用）。
 *
 * 在某模型配置从磁盘 JSON 读入之后、正式作用于实体之前触发。
 * 监听者可直接修改 {@link #getData()} 返回的 {@link VrmModelConfig.Data} 字段
 * （血量/伤害/速度/权重/行为/群系/掉落等），修改结果即作为本次生效的配置。
 *
 * 注意：本钩子只在「配置将实际作用于实体」的路径触发（如实体生成、应用配置），
 * 不会在渲染等高频读取路径触发，避免性能损耗与递归。
 */
public class VrmConfigLoadEvent extends Event {

    private final String modelName;
    private final VrmModelConfig.Data data;

    public VrmConfigLoadEvent(String modelName, VrmModelConfig.Data data) {
        this.modelName = modelName;
        this.data = data;
    }

    /** 被读取的模型名 */
    public String getModelName() {
        return modelName;
    }

    /** 即将生效的配置数据（字段 public，可直接修改） */
    public VrmModelConfig.Data getData() {
        return data;
    }
}
