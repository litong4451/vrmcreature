package com.example.vrmcreature.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 「掉落物设置」界面：配置生物死亡时的物品掉落。
 * 每条掉落：物品ID（如 minecraft:diamond）+ 数量 + 概率（0~1，1=必定掉落）。
 * 数据以 "物品ID;数量;概率" 字符串行存入草稿，最终随生成包写入配置。
 */
public class VrmLootScreen extends Screen {

    private static final int MAX_SHOW = 6;

    private final Screen parent;
    private final VrmDraft draft;
    private EditBox itemBox;
    private EditBox countBox;
    private EditBox chanceBox;
    private final List<Runnable> onRebuild = new ArrayList<>();

    public VrmLootScreen(Screen parent, VrmDraft draft) {
        super(Component.literal("掉落物设置"));
        this.parent = parent;
        this.draft = draft;
    }

    @Override
    protected void init() {
        int w = 200;
        int x = (this.width - w) / 2;
        int y = 30;

        // 输入行：物品ID(92) + 数量(34) + 概率(42) + 添加(32)
        itemBox = new EditBox(this.font, x, y, 92, 20, Component.literal("物品ID"));
        countBox = new EditBox(this.font, x + 96, y, 34, 20, Component.literal("数量"));
        chanceBox = new EditBox(this.font, x + 134, y, 34, 20, Component.literal("概率"));
        itemBox.setMaxLength(64);
        countBox.setMaxLength(4);
        chanceBox.setMaxLength(6);
        countBox.setFilter(s -> s.matches("\\d*"));
        chanceBox.setFilter(s -> s.matches("\\d*(\\.\\d*)?"));

        addRenderableWidget(itemBox);
        addRenderableWidget(countBox);
        addRenderableWidget(chanceBox);
        addRenderableWidget(Button.builder(Component.literal("添加"), b -> addEntry())
                .bounds(x + 172, y, 28, 20).build());
        y += 28;

        // 现有掉落列表
        int shown = 0;
        for (int i = 0; i < draft.lootLines.size() && shown < MAX_SHOW; i++, shown++) {
            String line = draft.lootLines.get(i);
            String[] p = line.split(";");
            String id = p.length > 0 ? p[0] : "";
            String cnt = p.length > 1 ? p[1] : "1";
            String ch = p.length > 2 ? p[2] : "1.0";
            String label = (i + 1) + ". " + id + "  x" + cnt + "  " + ch;
            addRenderableWidget(Button.builder(Component.literal(label), b -> {})
                    .bounds(x, y, w - 40, 20).build());
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal("删除"), b -> {
                if (idx >= 0 && idx < draft.lootLines.size()) {
                    draft.lootLines.remove(idx);
                }
                rebuild();
            }).bounds(x + w - 40, y, 40, 20).build());
            y += 22;
        }
        if (draft.lootLines.size() > MAX_SHOW) {
            addRenderableWidget(Button.builder(Component.literal("…共 " + draft.lootLines.size() + " 条，多余条目请到配置文件查看"), b -> {})
                    .bounds(x, y, w, 14).build());
            y += 18;
        } else if (draft.lootLines.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("（暂无掉落物配置）"), b -> {})
                    .bounds(x, y, w, 14).build());
            y += 18;
        }

        y += 4;
        addRenderableWidget(Button.builder(Component.literal("保存并返回"), b ->
                        Minecraft.getInstance().setScreen(parent))
                .bounds(x, y, w, 20).build());
    }

    private void addEntry() {
        String id = itemBox.getValue().trim();
        String cnt = countBox.getValue().trim();
        String ch = chanceBox.getValue().trim();
        if (id.isEmpty()) {
            return;
        }
        int count;
        double chance;
        try {
            count = cnt.isEmpty() ? 1 : Integer.parseInt(cnt);
            chance = ch.isEmpty() ? 1.0 : Double.parseDouble(ch);
        } catch (NumberFormatException e) {
            return;
        }
        draft.lootLines.add(id + ";" + Math.max(1, count) + ";" + Math.max(0.0, Math.min(1.0, chance)));
        itemBox.setValue("");
        countBox.setValue("");
        chanceBox.setValue("");
        rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }
}
