package com.example.vrmcreature.client;

import com.example.vrmcreature.entity.client.VrmCreatureelLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 「选择模型文件」界面：列出模型目录下全部 .vrm/.glb 模型，单选，写入草稿 modelName。
 * 模型来源：<游戏目录>/versions/<版本名>/vrmcreature/vrm/ 目录下所有 .vrm/.glb 文件（自动扫描）。
 */
public class VrmModelSelectScreen extends Screen {

    private static final int PAGE_SIZE = 8;

    private final Screen parent;
    private final VrmDraft draft;
    private int page = 0;
    private final List<String> allModels = new ArrayList<>();

    public VrmModelSelectScreen(Screen parent, VrmDraft draft) {
        super(Component.literal("选择模型文件"));
        this.parent = parent;
        this.draft = draft;
        this.allModels.addAll(VrmCreatureelLoader.listModelNames());
        if (allModels.isEmpty()) {
            allModels.add("model");
        }
    }

    @Override
    protected void init() {
        int w = 200;
        int x = (this.width - w) / 2;
        int y = 30;

        // 模型单选列表（分页）
        int totalPages = Math.max(1, (allModels.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        int start = page * PAGE_SIZE;
        int end = Math.min(allModels.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            String name = allModels.get(i);
            boolean selected = draft.modelName.equals(name);
            String label = (selected ? "✔ " : "    ") + name;
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                draft.modelName = name;
                rebuild();
            }).bounds(x, y, w, 20).build());
            y += 22;
        }

        // 翻页
        addRenderableWidget(Button.builder(Component.literal("上一页"), b -> {
            page = Math.max(0, page - 1);
            rebuild();
        }).bounds(x, y, (w - 10) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("下一页"), b -> {
            page = Math.min(totalPages - 1, page + 1);
            rebuild();
        }).bounds(x + (w - 10) / 2 + 10, y, (w - 10) / 2, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(Component.literal("当前模型：" + draft.modelName), b -> {})
                .bounds(x, y, w, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(Component.literal("确定"), b ->
                        Minecraft.getInstance().setScreen(parent))
                .bounds(x, y, w, 20).build());
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }
}
