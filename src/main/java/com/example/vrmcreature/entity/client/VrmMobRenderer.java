package com.example.vrmcreature.entity.client;

import com.example.vrmcreature.VrmCreature;
import com.example.vrmcreature.entity.VrmMob;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * VRM 生物渲染器：播放骨骼动画并渲染蒙皮网格。
 * 支持多模型：按实体的 modelName 从 assets/vrmcreature/vrm/ 目录懒加载对应模型，
 * 每个模型独立缓存（模型数据 + 动画器 + 内嵌贴图），首次使用时才加载。
 * 动画自动选择：移动→walk/run，攻击→attack，否则→idle（或动作偏好）。
 * 纹理优先使用模型内嵌贴图（VRM 0.x/2.0），无内嵌贴图时回退默认 model.png。
 */
public class VrmMobRenderer extends EntityRenderer<VrmMob> {
    private static final Logger LOGGER = LoggerFactory.getLogger("VRMCreature");
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VrmCreature.MODID, "textures/vrm/model.png");

    /** 单个模型的一份缓存：模型数据 + 动画器 + 纹理 */
    private static class ModelEntry {
        final VrmCreatureel model;
        final VrmAnimator animator;
        final ResourceLocation textureLocation;
        final DynamicTexture dynamicTexture; // 持有引用避免被 GC

        ModelEntry(VrmCreatureel model, VrmAnimator animator,
                   ResourceLocation textureLocation, DynamicTexture dynamicTexture) {
            this.model = model;
            this.animator = animator;
            this.textureLocation = textureLocation;
            this.dynamicTexture = dynamicTexture;
        }
    }

    private final Map<String, ModelEntry> cache = new HashMap<>();

    public VrmMobRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** 获取某模型名的缓存条目，未加载时惰性加载 */
    private ModelEntry entry(String modelName) {
        String name = (modelName == null || modelName.isEmpty()) ? "model" : modelName;
        return cache.computeIfAbsent(name, this::loadEntry);
    }

    private ModelEntry loadEntry(String name) {
        VrmCreatureel model = VrmCreatureelLoader.loadByName(name);
        VrmAnimator animator = new VrmAnimator(model);

        // 优先注册模型内嵌贴图
        DynamicTexture dyn = null;
        ResourceLocation loc = DEFAULT_TEXTURE;
        if (model != null && model.textureBytes != null && model.textureBytes.length > 0) {
            try (InputStream in = new ByteArrayInputStream(model.textureBytes)) {
                NativeImage img = NativeImage.read(in);
                if (img != null && img.getWidth() > 0) {
                    dyn = new DynamicTexture(img); // 所有权移交 DynamicTexture
                    String safe = sanitize(name);
                    loc = ResourceLocation.fromNamespaceAndPath(VrmCreature.MODID,
                            "textures/vrm/" + safe + "_embedded");
                    Minecraft.getInstance().getTextureManager().registerTexture(loc, dyn);
                    LOGGER.info("Registered embedded texture for model '{}' {}x{}", name, img.getWidth(), img.getHeight());
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Failed to decode embedded texture for model '{}', fallback to default: {}",
                        name, e.getMessage());
            }
        }
        return new ModelEntry(model, animator, loc, dyn);
    }

    /** 模型名仅允许英文字母/数字/下划线/点/连字符，其余字符替换为下划线（避免资源路径非法） */
    private static String sanitize(String name) {
        String s = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return s.isEmpty() ? "model" : s;
    }

    @Override
    public void render(VrmMob entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        ModelEntry e = entry(entity.getModelName());
        if (e.model == null || e.model.isEmpty()) {
            return;
        }

        // 1) 根据实体状态选择动画并更新时间
        VrmCreatureel.AnimationClip clip = selectClip(e.model, entity);
        float time = (entity.tickCount + partialTicks) / 20.0F;
        if (clip != null && clip.duration > 0f) {
            time = time % clip.duration;
        }
        e.animator.update(time, clip);

        // 2) 渲染网格
        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(e.textureLocation));

        for (VrmCreatureel.MeshData mesh : e.model.meshes) {
            if (mesh.indices == null) continue;
            for (int i = 0; i < mesh.indices.length; i += 3) {
                int i0 = mesh.indices[i];
                int i1 = mesh.indices[i + 1];
                int i2 = mesh.indices[i + 2];
                vertex(consumer, pose, e.animator, mesh, i0, packedLight);
                vertex(consumer, pose, e.animator, mesh, i1, packedLight);
                vertex(consumer, pose, e.animator, mesh, i2, packedLight);
            }
        }

        poseStack.popPose();
    }

    /** 选择当前应播放的动画片段 */
    private VrmCreatureel.AnimationClip selectClip(VrmCreatureel model, VrmMob entity) {
        if (model.animations.isEmpty()) return null;

        boolean moving = entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5D;
        boolean attacking = entity.swinging && entity.attackAnim > 0.0F;

        String pref = switch (com.example.vrmcreature.config.VrmCreatureConfig.ANIM_PREF.get()) {
            case 1 -> "walk";
            case 2 -> "attack";
            default -> "idle";
        };

        // 按状态优先级找匹配动画
        String want = attacking ? "attack" : (moving ? "walk" : pref);
        VrmCreatureel.AnimationClip best = null;
        for (VrmCreatureel.AnimationClip c : model.animations) {
            String name = c.name.toLowerCase();
            if (name.contains(want)) return c;
            if (best == null && !name.isEmpty()) best = c;
        }
        return best;
    }

    private void vertex(VertexConsumer consumer, Matrix4f pose, VrmAnimator animator,
                        VrmCreatureel.MeshData mesh, int index, int packedLight) {
        float x = mesh.positions[index * 3];
        float y = mesh.positions[index * 3 + 1];
        float z = mesh.positions[index * 3 + 2];

        // 蒙皮变换
        Vector3f skinned = animator.applySkin(mesh, index, x, y, z);
        x = skinned.x();
        y = skinned.y();
        z = skinned.z();

        float nx = 0.0F, ny = 1.0F, nz = 0.0F;
        if (mesh.normals != null) {
            nx = mesh.normals[index * 3];
            ny = mesh.normals[index * 3 + 1];
            nz = mesh.normals[index * 3 + 2];
        }

        // UV：glTF 的 V 原点在顶部，Minecraft 在底部，需翻转 V
        float u = 0.0F, v = 0.0F;
        if (mesh.uvs != null && index * 2 + 1 < mesh.uvs.length) {
            u = mesh.uvs[index * 2];
            v = 1.0F - mesh.uvs[index * 2 + 1];
        }

        consumer.addVertex(pose, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, v)
                .setOverlay(packedLight)
                .setLight(packedLight)
                .setNormal(nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(VrmMob entity) {
        return entry(entity.getModelName()).textureLocation;
    }
}
