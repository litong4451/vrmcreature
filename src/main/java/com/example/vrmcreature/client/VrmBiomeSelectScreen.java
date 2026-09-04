package com.example.vrmcreature.client;

import com.example.vrmcreature.config.VrmCreatureConfig;
import com.example.vrmcreature.network.Network;
import com.example.vrmcreature.network.VrmBiomePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 「选择刷新群系」界面：列出全部生物群系，可多选。
 * - 空选择（等于全选全部）发送空列表 = 全部群系
 * - 选择部分群系时发送对应群系 ID 列表
 * 设置通过 VrmBiomePacket 同步到服务端。
 */
public class VrmBiomeSelectScreen extends Screen {

    private static final int PAGE_SIZE = 8;

    private final Screen parent;
    private int page = 0;
    /** 所有群系 ID（有序，按显示名排序） */
    private final List<String> allBiomes = new ArrayList<>();
    /** 已勾选群系（保持勾选顺序） */
    private final Set<String> selected = new LinkedHashSet<>();

    public VrmBiomeSelectScreen(Screen parent) {
        super(Component.literal("选择刷新群系"));
        this.parent = parent;

        // 收集全部已注册生物群系
        var level = Minecraft.getInstance().level;
        var registryOpt = level != null ? level.registryAccess().registry(Registries.BIOME) : Optional.empty();
        if (registryOpt.isPresent()) {
            var registry = registryOpt.get();
            List<String> ids = new ArrayList<>();
            for (ResourceKey<Biome> key : registry.keySet()) {
                String id = key.location().toString();
                String name = registry.get(key).map(h -> h.value())
                        .map(Biome::getLocalizedName)
                        .orElse("");
                ids.add(id + "\u0000" + name); // 排序用内部拼接
            }
            ids.sort(Comparator.comparing(s -> s.split("\u0000")[1]));
            for (String s : ids) {
                allBiomes.add(s.split("\u0000")[0]);
            }
        }

        // 回显当前配置：空列表 = 全部
        List<? extends String> saved = VrmCreatureConfig.SPAWN_BIOMES.get();
        if (saved == null || saved.isEmpty()) {
            selected.addAll(allBiomes);
        } else {
            for (String id : saved) {
                if (allBiomes.contains(id)) {
                    selected.add(id);
                }
            }
        }
    }

    @Override
    protected void init() {
        int w = 200;
        int x = (this.width - w) / 2;
        int y = 30;

        // 全选 / 清空
        addRenderableWidget(Button.builder(Component.literal("全选"), b -> {
            selected.addAll(allBiomes);
            rebuild();
        }).bounds(x, y, (w - 10) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("清空"), b -> {
            selected.clear();
            rebuild();
        }).bounds(x + (w - 10) / 2 + 10, y, (w - 10) / 2, 20).build());
        y += 28;

        // 群系多选列表（分页）
        int totalPages = Math.max(1, (allBiomes.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= totalPages) page = totalPages - 1;
        int start = page * PAGE_SIZE;
        int end = Math.min(allBiomes.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            String id = allBiomes.get(i);
            String label = (selected.contains(id) ? "✔ " : "    ") + shortName(id);
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                if (!selected.add(id)) {
                    selected.remove(id);
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

        // 状态提示
        String status = selected.isEmpty() ? "（未选 = 全部群系）" : ("已选 " + selected.size() + " 个群系");
        addRenderableWidget(Button.builder(Component.literal(status), b -> {})
                .bounds(x, y, w, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(Component.literal("保存"), b -> save())
                .bounds(x, y, w, 20).build());
        y += 26;

        addRenderableWidget(Button.builder(Component.literal("取消"), b ->
                        Minecraft.getInstance().setScreen(parent))
                .bounds(x, y, w, 20).build());
    }

    private void save() {
        List<String> chosen = new ArrayList<>(selected);
        // 已选等于全部时发送空列表（=全部群系）
        if (chosen.size() == allBiomes.size()) {
            chosen.clear();
        }
        Network.sendToServer(new VrmBiomePacket(chosen));
        Minecraft.getInstance().setScreen(parent);
    }

    /** 去掉命名空间前缀的短名（如 minecraft:plains -> plains） */
    private static String shortName(String id) {
        int idx = id.indexOf(':');
        return idx >= 0 ? id.substring(idx + 1) : id;
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }
}
