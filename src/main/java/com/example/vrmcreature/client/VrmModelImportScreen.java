package com.example.vrmcreature.client;

import com.example.vrmcreature.config.VrmModelConfig;
import com.example.vrmcreature.entity.client.VrmCreatureelLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 「导入模型」界面：文件管理器已选定源模型文件后，
 * 要求玩家填写英文模型名（必填，模组内唯一标识，仅字母/数字/下划线）
 * 与中文显示名（可选，留空则界面显示英文名）。
 * 确认后把源文件复制到模型目录，并把中文显示名写入该模型配置。
 */
public class VrmModelImportScreen extends Screen {

    private final Screen parent;
    private final VrmDraft draft;
    private final Path srcFile;

    private EditBox nameBox;
    private EditBox displayBox;
    private String error = "";

    public VrmModelImportScreen(Screen parent, VrmDraft draft, Path srcFile) {
        super(Component.literal("导入模型"));
        this.parent = parent;
        this.draft = draft;
        this.srcFile = srcFile;
    }

    @Override
    protected void init() {
        int w = 220;
        int x = (this.width - w) / 2;
        int y = 50;

        addRenderableWidget(Button.builder(Component.literal("源文件：" + srcFile.getFileName()), b -> {})
                .bounds(x, y, w, 20).build());
        y += 30;

        this.nameBox = new EditBox(this.font, x, y, w, 20, Component.literal("英文模型名"));
        this.nameBox.setMaxLength(64);
        addRenderableWidget(this.nameBox);
        y += 30;

        this.displayBox = new EditBox(this.font, x, y, w, 20, Component.literal("中文显示名（可选）"));
        this.displayBox.setMaxLength(64);
        addRenderableWidget(this.displayBox);
        y += 36;

        addRenderableWidget(Button.builder(Component.literal("确定导入"), b -> doImport())
                .bounds(x, y, w, 20).build());
        y += 26;

        addRenderableWidget(Button.builder(Component.literal("取消"), b ->
                        Minecraft.getInstance().setScreen(parent))
                .bounds(x, y, w, 20).build());
    }

    private void doImport() {
        String eng = this.nameBox.getValue().trim();
        if (eng.isEmpty() || !eng.matches("[A-Za-z0-9_]+")) {
            this.error = "英文名不能为空，且只能包含字母、数字、下划线";
            return;
        }
        String cn = this.displayBox.getValue().trim();

        String fn = srcFile.getFileName().toString();
        String ext = "";
        int dot = fn.lastIndexOf('.');
        if (dot >= 0) {
            ext = fn.substring(dot);
        }
        if (!ext.equalsIgnoreCase(".vrm") && !ext.equalsIgnoreCase(".glb")) {
            this.error = "仅支持 .vrm / .glb 模型文件";
            return;
        }

        VrmCreatureelLoader.ensureVrmDir();
        Path dst = VrmCreatureelLoader.vrmDir().resolve(eng + ext.toLowerCase());
        try {
            Files.copy(srcFile, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            this.error = "复制模型文件失败：" + e.getMessage();
            return;
        }

        // 中文显示名：非空写入配置；为空且已有配置则清空显示名
        VrmModelConfig.Data d = VrmModelConfig.load(eng);
        if (!cn.isEmpty()) {
            d.displayName = cn;
            VrmModelConfig.save(eng, d);
        } else if (VrmModelConfig.isConfigured(eng)) {
            d.displayName = "";
            VrmModelConfig.save(eng, d);
        }

        draft.modelName = eng;
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.nameBox != null) {
            guiGraphics.drawString(this.font, "英文模型名（必填）", this.nameBox.getX(), this.nameBox.getY() - 12, 0xFFFFFF);
        }
        if (this.displayBox != null) {
            guiGraphics.drawString(this.font, "中文显示名（可选，留空显示英文名）", this.displayBox.getX(), this.displayBox.getY() - 12, 0xFFFFFF);
        }
        if (!this.error.isEmpty() && this.displayBox != null) {
            guiGraphics.drawString(this.font, this.error, this.displayBox.getX(), this.displayBox.getY() + 26, 0xFF5555);
        }
    }
}
