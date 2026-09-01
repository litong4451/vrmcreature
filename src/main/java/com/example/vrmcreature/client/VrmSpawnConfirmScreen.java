package com.example.vrmcreature.client;

import com.example.vrmcreature.network.Network;
import com.example.vrmcreature.network.VrmSpawnPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 「是否现在生成」确认弹窗。
 * 可调整生成数量（默认 1），确认后发送生成请求到服务端。
 */
public class VrmSpawnConfirmScreen extends Screen {

    private final Screen parent;
    private int count = 1;

    public VrmSpawnConfirmScreen(Screen parent) {
        super(Component.literal("是否现在生成？"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 200;
        int x = (this.width - w) / 2;
        int y = 60;

        addRenderableWidget(Button.builder(Component.literal("生成数量：" + count), b -> {})
                .bounds(x + 30, y, w - 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            count = Math.min(100, count + 1);
            rebuildLabel();
        }).bounds(x, y, 26, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            count = Math.max(1, count - 1);
            rebuildLabel();
        }).bounds(x + w - 26, y, 26, 20).build());
        y += 30;

        addRenderableWidget(Button.builder(Component.literal("确认生成"), b -> {
            Network.sendToServer(new VrmSpawnPacket(count));
            Minecraft.getInstance().setScreen(parent);
        }).bounds(x, y, w, 20).build());
        y += 26;

        addRenderableWidget(Button.builder(Component.literal("取消"), b ->
                        Minecraft.getInstance().setScreen(parent))
                .bounds(x, y, w, 20).build());
    }

    private void rebuildLabel() {
        // 重建整个界面以刷新数量标签
        this.clearWidgets();
        this.init();
    }
}
