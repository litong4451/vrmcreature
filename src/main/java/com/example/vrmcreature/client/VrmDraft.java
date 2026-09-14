package com.example.vrmcreature.client;

import com.example.vrmcreature.config.VrmModelConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 「新建生物」流程的临时草稿数据，在 VrmCreateScreen 及其子界面（选模型/属性/群系/掉落物）间共享。
 * 单模型流程：每次新建只针对一个模型（modelName），其独立配置写入
 * versions/<版本名>/vrmcreature/config/<modelName>.json。
 */
public class VrmDraft {

    /** 当前操作的模型名（不含扩展名） */
    public String modelName = "model";

    public int faction = 0;        // 0=敌方 1=中立 2=我方
    public double health = 20.0D;
    public double damage = 3.0D;
    public double speed = 0.25D;
    public int behavior = 1;       // 0 静止 1 巡逻 2 跟随 3 主动攻击
    public int animPref = 0;       // 0 待机 1 走路 2 攻击
    public int count = 1;          // 生成数量

    /** 已勾选群系 ID（空 = 全部群系） */
    public final Set<String> selectedBiomes = new LinkedHashSet<>();
    /** 掉落物配置行，格式：物品ID;数量;概率（如 minecraft:diamond;1;0.3） */
    public final List<String> lootLines = new ArrayList<>();

    private boolean initialized = false;

    /** 从该模型的 JSON 配置回显默认值（仅在首次创建时执行一次） */
    public void ensureInit() {
        if (initialized) {
            return;
        }
        initialized = true;
        VrmModelConfig.Data d = VrmModelConfig.load(modelName);
        faction = d.faction;
        health = d.health;
        damage = d.damage;
        speed = d.speed;
        behavior = d.behavior;
        animPref = d.animPref;
        selectedBiomes.addAll(d.spawnBiomes);
        lootLines.addAll(d.loot);
    }
}
