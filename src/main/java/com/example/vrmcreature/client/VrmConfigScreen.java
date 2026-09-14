package com.example.vrmcreature.client;

import com.example.vrmcreature.entity.client.VrmCreatureelLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * VRM 生物入口界面（快捷键打开）。
 * 打开方式：按快捷键（默认未绑定，可在设置-控制中指定）打开，无需输入命令。
 * 入口：
 *  - 「打开模型目录」：在系统中打开模型目录，方便直接放入 .vrm/.glb 文件
 *  - 「新建生物」：进入完整创建流程（选模型/属性/刷新群系/掉落物/生成）
 */
public class VrmConfigScreen extends Screen {

    public VrmConfigScreen() {
        super(Component.literal("VRM 生物设置"));
    }

    @Override
    protected void init() {
        int w = 200;
        int x = (this.width - w) / 2;
        int y = 40;

        addRenderableWidget(Button.builder(Component.literal("打开模型目录"), b -> openModelDir())
                .bounds(x, y, w, 20).build());
        y += 30;

        addRenderableWidget(Button.builder(Component.literal("新建生物"), b ->
                        Minecraft.getInstance().setScreen(new VrmCreateScreen(this)))
                .bounds(x, y, w, 20).build());
        y += 30;

        addRenderableWidget(Button.builder(Component.literal("关闭"), b -> this.onClose())
                .bounds(x, y, w, 20).build());
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
}
