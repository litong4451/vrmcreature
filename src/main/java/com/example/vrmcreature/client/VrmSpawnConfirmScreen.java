package com.example.vrmcreature.client;

import com.example.vrmcreature.entity.client.VrmCreatureelLoader;
import com.example.vrmcreature.network.Network;
import com.example.vrmcreature.network.VrmSpawnPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 「是否现在生成」确认弹窗。
 * - 可调整生成数量（默认 1）
 * - 支持多选模型：可勾选多个模型，生成 count 只生物时按所选模型循环分配
 * 模型来源：assets/vrmcreature/vrm/ 目录下所有 .vrm/.glb 文件（自动扫描）。
 */
public class VrmSpawnConfirmScreen extends Screen {

    private static final int PAGE_SIZE = 8;

    private final Screen parent;
    private int count = 1;
    private int page = 0;
    /** 所有可用模型名（有序） */
    private final List<String> allModels = new ArrayList<>();
    /** 已勾选模型（保持勾选顺序） */
    private final Set<String> selected = new LinkedHashSet<>();

    public VrmSpawnConfirmScreen(Screen parent) {
        super(Component.literal("是否现在生成？"));
        this.parent = parent;
        this.allModels.addAll(VrmCreatureelLoader.listModelNames());
        if (allModels.isEmpty()) {
            allModels.add("model");
        }
        // 默认全选
        selected.addAll(allModels);
    }

    @Override
    protected void init() {
        int w = 200;
        int x = (this.width - w) / 2;
        int y = 30;

        // 数量行
        addRenderableWidget(Button.builder(Component.literal("生成数量：" + count), b -> {})
                .bounds(x + 30, y, w - 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            count = Math.min(100, count + 1);
            rebuild();
        }).bounds(x, y, 26, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            count = Math.max(1, count - 1);
            rebuild();
        }).bounds(x + w - 26, y, 26, 20).build());
        y += 28;

        // 全选 / 全不选
        addRenderableWidget(Button.builder(Component.literal("全选"), b -> {
            selected.addAll(allModels);
            rebuild();
        }).bounds(x, y, (w - 10) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("清空"), b -> {
            selected.clear();
            rebuild();
        }).bounds(x + (w - 10) / 2 + 10, y, (w - 10) / 2, 20).build());
        y += 28;

        // 模型多选列表（分页）
        int totalPages = Math.max(1, (allModels.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= totalPages) page = totalPages - 1;
        int start = page * PAGE_SIZE;
        int end = Math.min(allModels.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            String name = allModels.get(i);
            String label = (selected.contains(name) ? "✔ " : "    ") + name;
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                if (!selected.add(name)) {
                    selected.remove(name);
                }
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

        // 已选提示
        addRenderableWidget(Button.builder(Component.literal("已选模型：" + selected.size() + " 个"), b -> {})
                .bounds(x, y, w, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(Component.literal("确认生成"), b -> spawn())
                .bounds(x, y, w, 20).build());
        y += 26;

        addRenderableWidget(Button.builder(Component.literal("取消"), b ->
                        Minecraft.getInstance().setScreen(parent))
                .bounds(x, y, w, 20).build());
    }

    private void spawn() {
        // 无勾选时回退为全部模型
        List<String> chosen = new ArrayList<>(selected);
        if (chosen.isEmpty()) {
            chosen.addAll(allModels);
        }
        Network.sendToServer(new VrmSpawnPacket(count, chosen));
        Minecraft.getInstance().setScreen(parent);
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }
}
