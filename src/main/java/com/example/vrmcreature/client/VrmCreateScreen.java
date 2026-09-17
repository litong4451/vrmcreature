package com.example.vrmcreature.client;

import com.example.vrmcreature.config.VrmModelConfig;
import com.example.vrmcreature.entity.client.VrmCreatureelLoader;
import com.example.vrmcreature.network.Network;
import com.example.vrmcreature.network.VrmBiomePacket;
import com.example.vrmcreature.network.VrmLootPacket;
import com.example.vrmcreature.network.VrmSavePacket;
import com.example.vrmcreature.network.VrmSpawnPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 「新建生物」主界面：整合完整创建流程（单模型）。
 *  - 选择模型文件（单选，进入 VrmModelSelectScreen）
 *  - 设置属性（阵营/生命/攻击/速度/行为/动作，进入 VrmAttrScreen）
 *  - 刷新群系（多选，进入 VrmBiomeSelectScreen）
 *  - 掉落物设置（进入 VrmLootScreen）
 *  - 调整生成数量后点「生成生物」统一发送
 * 每个模型拥有独立配置（versions/<版本名>/vrmcreature/config/<模型名>.json）。
 */
public class VrmCreateScreen extends Screen {

    private final Screen parent;
    private final VrmDraft draft = new VrmDraft();

    public VrmCreateScreen(Screen parent) {
        super(Component.literal("新建生物"));
        this.parent = parent;
        // 默认选择第一个模型
        List<String> models = VrmCreatureelLoader.listModelNames();
        if (models.isEmpty()) {
            models.add("model");
        }
        draft.modelName = models.get(0);
        draft.ensureInit();
    }

    @Override
    protected void init() {
        int w = 200;
        int x = (this.width - w) / 2;
        int y = 30;

        addRenderableWidget(Button.builder(Component.literal("选择模型文件：" + VrmCreatureelLoader.displayName(draft.modelName)), b ->
                        Minecraft.getInstance().setScreen(new VrmModelSelectScreen(this, draft)))
                .bounds(x, y, w, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(Component.literal("设置属性"), b ->
                        Minecraft.getInstance().setScreen(new VrmAttrScreen(this, draft)))
                .bounds(x, y, w, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(Component.literal("刷新群系（" + biomeSummary() + "）"), b ->
                        Minecraft.getInstance().setScreen(new VrmBiomeSelectScreen(this, draft)))
                .bounds(x, y, w, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(Component.literal("掉落物设置（" + draft.lootLines.size() + " 条）"), b ->
                        Minecraft.getInstance().setScreen(new VrmLootScreen(this, draft)))
                .bounds(x, y, w, 20).build());
        y += 34;

        // 生成数量
        addRenderableWidget(Button.builder(Component.literal("生成数量：" + draft.count), b -> {})
                .bounds(x + 30, y, w - 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            draft.count = Math.min(100, draft.count + 1);
            rebuild();
        }).bounds(x, y, 26, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            draft.count = Math.max(1, draft.count - 1);
            rebuild();
        }).bounds(x + w - 26, y, 26, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(Component.literal("生成生物"), b -> createAndSpawn())
                .bounds(x, y, w, 20).build());
        y += 26;

        addRenderableWidget(Button.builder(Component.literal("返回"), b ->
                        Minecraft.getInstance().setScreen(parent))
                .bounds(x, y, w, 20).build());
    }

    /** 统一打包：属性(若未锁定) + 群系 + 掉落物 + 生成（单模型） */
    private void createAndSpawn() {
        VrmModelConfig.Data d = VrmModelConfig.load(draft.modelName);
        if (!d.locked) {
            Network.sendToServer(new VrmSavePacket(
                    draft.modelName, draft.faction, draft.health, draft.damage, draft.speed,
                    draft.behavior, draft.animPref));
        }
        // 空列表 = 全部群系
        Network.sendToServer(new VrmBiomePacket(draft.modelName, new ArrayList<>(draft.selectedBiomes)));
        Network.sendToServer(new VrmLootPacket(draft.modelName, new ArrayList<>(draft.lootLines)));

        Network.sendToServer(new VrmSpawnPacket(draft.count, draft.modelName));
        Minecraft.getInstance().setScreen(parent);
    }

    private String biomeSummary() {
        if (draft.selectedBiomes.isEmpty()) {
            return "全部";
        }
        return "已选 " + draft.selectedBiomes.size() + " 个";
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }
}
