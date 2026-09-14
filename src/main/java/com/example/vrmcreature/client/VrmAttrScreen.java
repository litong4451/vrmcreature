package com.example.vrmcreature.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 「设置属性」界面：编辑草稿中的阵营/生命/攻击/速度/行为/动作。
 * 修改直接写入 VrmDraft，点「保存并返回」回到新建生物界面。
 */
public class VrmAttrScreen extends Screen {

    private final Screen parent;
    private final VrmDraft draft;
    private final List<Supplier<String>> labelProviders = new ArrayList<>();
    private final List<Button> labels = new ArrayList<>();

    public VrmAttrScreen(Screen parent, VrmDraft draft) {
        super(Component.literal("设置属性"));
        this.parent = parent;
        this.draft = draft;
    }

    @Override
    protected void init() {
        labels.clear();
        labelProviders.clear();

        int w = 200;
        int x = (this.width - w) / 2;
        int y = 30;

        // 阵营：标签 + 切换按钮同行
        addLabel(x, y, w - 70, () -> "阵营：" + factionName());
        addRenderableWidget(Button.builder(Component.literal("切换"), b -> {
            draft.faction = (draft.faction + 1) % 3;
            refreshLabels();
        }).bounds(x + w - 70, y, 70, 20).build());
        y += 26;

        y = addAdjustRow(x, y, w, () -> "生命值：" + fmt(draft.health),
                () -> draft.health = Math.min(1024, draft.health + 1),
                () -> draft.health = Math.max(1, draft.health - 1));
        y = addAdjustRow(x, y, w, () -> "攻击力：" + fmt(draft.damage),
                () -> draft.damage = Math.min(1024, draft.damage + 1),
                () -> draft.damage = Math.max(0, draft.damage - 1));
        y = addAdjustRow(x, y, w, () -> "速度：" + fmt(draft.speed),
                () -> draft.speed = Math.min(10.0, draft.speed + 0.05),
                () -> draft.speed = Math.max(0.05, draft.speed - 0.05));

        // 行为模式：标签 + 切换按钮同行
        addLabel(x, y, w - 70, () -> "行为模式：" + behaviorName());
        addRenderableWidget(Button.builder(Component.literal("切换"), b -> {
            draft.behavior = (draft.behavior + 1) % 4;
            refreshLabels();
        }).bounds(x + w - 70, y, 70, 20).build());
        y += 26;

        // 动作偏好：标签 + 切换按钮同行
        addLabel(x, y, w - 70, () -> "动作偏好：" + animName());
        addRenderableWidget(Button.builder(Component.literal("切换"), b -> {
            draft.animPref = (draft.animPref + 1) % 3;
            refreshLabels();
        }).bounds(x + w - 70, y, 70, 20).build());
        y += 34;

        addRenderableWidget(Button.builder(Component.literal("保存并返回"), b ->
                        Minecraft.getInstance().setScreen(parent))
                .bounds(x, y, w, 20).build());
    }

    private int addAdjustRow(int x, int y, int w, Supplier<String> provider, Runnable plus, Runnable minus) {
        addLabel(x + 30, y, w - 60, provider);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            plus.run();
            refreshLabels();
        }).bounds(x, y, 26, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            minus.run();
            refreshLabels();
        }).bounds(x + w - 26, y, 26, 20).build());
        return y + 26;
    }

    private void addLabel(int x, int y, int w, Supplier<String> provider) {
        Button label = Button.builder(Component.literal(provider.get()), b -> {})
                .bounds(x, y, w, 20).build();
        label.active = false;
        addRenderableWidget(label);
        labels.add(label);
        labelProviders.add(provider);
    }

    private void refreshLabels() {
        for (int i = 0; i < labels.size(); i++) {
            labels.get(i).setMessage(Component.literal(labelProviders.get(i).get()));
        }
    }

    private String factionName() {
        return switch (draft.faction) {
            case 1 -> "中立";
            case 2 -> "我方";
            default -> "敌方";
        };
    }

    private String behaviorName() {
        return switch (draft.behavior) {
            case 0 -> "静止";
            case 2 -> "跟随玩家";
            case 3 -> "主动攻击";
            default -> "巡逻";
        };
    }

    private String animName() {
        return switch (draft.animPref) {
            case 1 -> "走路";
            case 2 -> "攻击";
            default -> "待机";
        };
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format("%.2f", v);
    }
}
