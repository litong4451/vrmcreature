package com.example.vrmcreature.client;

import com.example.vrmcreature.entity.client.VrmCreatureelLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.FileDialog;
import java.awt.Frame;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 「管理模型」界面（快捷键打开）。
 * 打开方式：按快捷键（默认未绑定，可在设置-控制中指定）打开，无需输入命令。
 * 功能：
 *  - 模型列表：显示模型目录下已创建的全部模型（优先显示中文名）
 *  - 「导入模型文件」：弹出系统文件管理器选择本机模型，填写英文名与中文显示名后复制进模型目录
 *  - 「新建生物」：进入完整创建流程（选模型/属性/刷新群系/掉落物/生成）
 *  - 「打开模型目录」：在系统中打开模型目录，方便直接放入 .vrm/.glb 文件
 */
public class VrmConfigScreen extends Screen {

    private static final int PAGE_SIZE = 8;

    private int page = 0;
    private final List<String> allModels = new ArrayList<>();

    public VrmConfigScreen() {
        super(Component.literal("管理模型"));
    }

    @Override
    protected void init() {
        allModels.clear();
        allModels.addAll(VrmCreatureelLoader.listModelNames());
        if (allModels.isEmpty()) {
            allModels.add("model");
        }

        int w = 200;
        int x = (this.width - w) / 2;
        int y = 30;

        // 导入模型（弹出文件管理器选择本地模型）
        addRenderableWidget(Button.builder(Component.literal("导入模型文件"), b -> chooseAndImport())
                .bounds(x, y, w, 20).build());
        y += 26;

        // 模型列表（分页显示已创建的模型）
        int totalPages = Math.max(1, (allModels.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        int start = page * PAGE_SIZE;
        int end = Math.min(allModels.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            String name = allModels.get(i);
            addRenderableWidget(Button.builder(Component.literal(VrmCreatureelLoader.displayName(name)), b -> {})
                    .bounds(x, y, w, 20).build());
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

        addRenderableWidget(Button.builder(Component.literal("新建生物"), b ->
                        Minecraft.getInstance().setScreen(new VrmCreateScreen(this)))
                .bounds(x, y, w, 20).build());
        y += 30;

        addRenderableWidget(Button.builder(Component.literal("打开模型目录"), b -> openModelDir())
                .bounds(x, y, w, 20).build());
        y += 30;

        addRenderableWidget(Button.builder(Component.literal("关闭"), b -> this.onClose())
                .bounds(x, y, w, 20).build());
    }

    /** 弹出系统文件管理器选择 .vrm/.glb 模型文件，选中后进入导入界面。 */
    private void chooseAndImport() {
        FileDialog dialog = new FileDialog((Frame) null, "选择 VRM/GLB 模型文件", FileDialog.LOAD);
        dialog.setFilenameFilter((dir, name) -> {
            String n = name.toLowerCase();
            return n.endsWith(".vrm") || n.endsWith(".glb");
        });
        dialog.setVisible(true);
        String dir = dialog.getDirectory();
        String file = dialog.getFile();
        dialog.dispose();
        if (dir == null || file == null) {
            return; // 用户取消选择
        }
        Path src = Paths.get(dir, file);
        Minecraft.getInstance().setScreen(new VrmModelImportScreen(this, null, src));
    }

    /** 用系统文件管理器打开模型目录（自动确保目录存在） */
    private void openModelDir() {
        VrmCreatureelLoader.ensureVrmDir();
        try {
            Util.getPlatform().openFile(VrmCreatureelLoader.vrmDir().toFile());
        } catch (Exception e) {
            // 打开失败（如无图形环境）时静默，目录仍已创建
        }
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }
}
