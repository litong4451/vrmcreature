package com.example.vrmcreature.client;

import com.example.vrmcreature.config.VrmCreatureConfig;
import com.example.vrmcreature.network.Network;
import com.example.vrmcreature.network.VrmSavePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * VRM 生物设置界面。
 * 打开方式：按快捷键 V（默认，可在设置-控制中改键）打开，无需输入命令。
 * 流程：
 *  - 未锁定：可设置阵营/属性/行为/动作，点「保存设置」→ 锁定并弹出生成确认
 *  - 已锁定：只读展示，只能点「现在生成」
 */
public class VrmConfigScreen extends Screen {

    private final List<Supplier<String>> labelProviders = new ArrayList<>();
    private final List<Button> labels = new ArrayList<>();

    // 创建模式字段（未锁定时可调）
    private int faction = 0;      // 0=敌方 1=中立 2=我方
    private double health = 20.0D;
    private double damage = 3.0D;
    private double speed = 0.25D;
    private int behavior = 1;     // 0 静止 1 巡逻 2 跟随 3 主动攻击
    private int animPref = 0;     // 0 待机 1 走路 2 攻击

    public VrmConfigScreen() {
        super(Component.literal("VRM 生物设置"));
        if (VrmCreatureConfig.isLocked()) {
            faction = VrmCreatureConfig.getFaction();
            health = VrmCreatureConfig.MAX_HEALTH.get();
            damage = VrmCreatureConfig.ATTACK_DAMAGE.get();
            speed = VrmCreatureConfig.MOVEMENT_SPEED.get();
            behavior = VrmCreatureConfig.BEHAVIOR.get();
            animPref = VrmCreatureConfig.ANIM_PREF.get();
        }
    }

    @Override
    protected void init() {
        labels.clear();
        labelProviders.clear();

        int w = 200;
        int x = (this.width - w) / 2;
        int y = 30;

        if (!VrmCreatureConfig.isLocked()) {
            y = buildCreateMode(x, y, w);
        } else {
            y = buildReadOnly(x, y, w);
        }

        addRenderableWidget(Button.builder(Component.literal("关闭"), b -> this.onClose())
                .bounds(x, y, w, 20).build());
    }

    /** 创建模式：全部可设置 */
    private int buildCreateMode(int x, int y, int w) {
        addLabel(x, y, w, () -> "当前阵营：" + factionName());
        addRenderableWidget(Button.builder(Component.literal("切换阵营"), b -> {
            faction = (faction + 1) % 3;
            refreshLabels();
        }).bounds(x + 60, y + 22, w - 120, 20).build());
        y += 52;

        y = addAdjustRow(x, y, w, () -> "生命值：" + fmt(health),
                () -> health = Math.min(1024, health + 1),
                () -> health = Math.max(1, health - 1));
        y = addAdjustRow(x, y, w, () -> "攻击力：" + fmt(damage),
                () -> damage = Math.min(1024, damage + 1),
                () -> damage = Math.max(0, damage - 1));
        y = addAdjustRow(x, y, w, () -> "速度：" + fmt(speed),
                () -> speed = Math.min(10.0, speed + 0.05),
                () -> speed = Math.max(0.05, speed - 0.05));

        addLabel(x, y, w, () -> "行为模式：" + behaviorName());
        addRenderableWidget(Button.builder(Component.literal("切换"), b -> {
            behavior = (behavior + 1) % 4;
            refreshLabels();
        }).bounds(x + 110, y + 22, w - 110, 20).build());
        y += 52;

        addLabel(x, y, w, () -> "动作偏好：" + animName());
        addRenderableWidget(Button.builder(Component.literal("切换"), b -> {
            animPref = (animPref + 1) % 3;
            refreshLabels();
        }).bounds(x + 110, y + 22, w - 110, 20).build());
        y += 52;

        addRenderableWidget(Button.builder(Component.literal("保存设置"), b -> saveAndSpawn())
                .bounds(x, y, w, 20).build());
        return y + 26;
    }

    /** 只读模式：展示已锁定设置，只能生成 */
    private int buildReadOnly(int x, int y, int w) {
        addLabel(x, y, w, () -> "阵营（已锁定）：" + factionName());
        y += 24;
        addLabel(x, y, w, () -> "生命值：" + fmt(health) + "  攻击力：" + fmt(damage));
        y += 24;
        addLabel(x, y, w, () -> "速度：" + fmt(speed));
        y += 24;
        addLabel(x, y, w, () -> "行为模式：" + behaviorName());
        y += 24;
        addLabel(x, y, w, () -> "动作偏好：" + animName());
        y += 30;

        addRenderableWidget(Button.builder(Component.literal("现在生成"), b ->
                        Minecraft.getInstance().setScreen(new VrmSpawnConfirmScreen(this)))
                .bounds(x, y, w, 20).build());
        return y + 26;
    }

    private void saveAndSpawn() {
        // 发送创建设置到服务端并锁定
        Network.sendToServer(new VrmSavePacket(faction, health, damage, speed, behavior, animPref));
        // 立即打开生成确认弹窗（数量默认1）
        Minecraft.getInstance().setScreen(new VrmSpawnConfirmScreen(this));
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
        return switch (faction) {
            case 1 -> "中立";
            case 2 -> "我方";
            default -> "敌方";
        };
    }

    private String behaviorName() {
        return switch (behavior) {
            case 0 -> "静止";
            case 2 -> "跟随玩家";
            case 3 -> "主动攻击";
            default -> "巡逻";
        };
    }

    private String animName() {
        return switch (animPref) {
            case 1 -> "走路";
            case 2 -> "攻击";
            default -> "待机";
        };
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format("%.2f", v);
    }
}
