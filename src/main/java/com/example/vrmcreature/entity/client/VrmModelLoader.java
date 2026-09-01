package com.example.vrmcreature.entity.client;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * VRM 模型加载器：通过 jgltf 解析资源包中的 .vrm/.glb 文件。
 * 提取：网格顶点、节点层级、骨骼蒙皮、动画片段。
 * 模型需放置于：assets/vrmcreature/vrm/ 目录下。
 */
public class VrmCreatureelLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("VRMCreature");

    public static VrmCreatureel load(ResourceLocation location) {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        Optional<Resource> resource = rm.getResource(location);
        if (resource.isEmpty()) {
            LOGGER.warn("VRM model not found: {}", location);
            return new VrmCreatureel(0);
        }

        try (InputStream in = resource.get().open()) {
            GltfModel gltf = new GltfModelReader().read(in);

            // 0) 检测 VRM 版本（1.0 扩展名 "VRM"，2.0 扩展名 "VRMC_vrm"）
            String vrmVersion = "glTF";
            List<String> extUsed = gltf.getExtensionsUsed();
            if (extUsed != null) {
                if (extUsed.contains("VRMC_vrm")) {
                    vrmVersion = "VRM 2.0";
                } else if (extUsed.contains("VRM")) {
                    vrmVersion = "VRM 0.x";
                }
            }

            // 1) 解析皮肤（骨骼关节 + 逆绑定矩阵）
            int jointCount = 0;
            if (!gltf.getSkinModels().isEmpty()) {
                SkinModel skin = gltf.getSkinModels().get(0);
                List<NodeModel> joints = skin.getJoints();
                jointCount = joints.size();
            }
            VrmCreatureel model = new VrmCreatureel(jointCount);
            model.vrmVersion = vrmVersion;

            // 2) 构建节点层级（索引 = 在 gltf nodeModels 中的位置）
            List<NodeModel> allNodes = gltf.getNodeModels();
            Map<NodeModel, Integer> nodeIndex = new HashMap<>();
            for (int i = 0; i < allNodes.size(); i++) {
                nodeIndex.put(allNodes.get(i), i);
                VrmCreatureel.Node n = new VrmCreatureel.Node();
                n.index = i;
                NodeTransform t = allNodes.get(i).getLocalTransform();
                if (t != null) {
                    if (t.hasTranslation()) {
                        n.translation[0] = t.getTranslationX();
                        n.translation[1] = t.getTranslationY();
                        n.translation[2] = t.getTranslationZ();
                    }
                    if (t.hasRotation()) {
                        n.rotation[0] = t.getRotationX();
                        n.rotation[1] = t.getRotationY();
                        n.rotation[2] = t.getRotationZ();
                        n.rotation[3] = t.getRotationW();
                    }
                    if (t.hasScale()) {
                        n.scale[0] = t.getScaleX();
                        n.scale[1] = t.getScaleY();
                        n.scale[2] = t.getScaleZ();
                    }
                }
                model.nodes.add(n);
            }
            for (int i = 0; i < allNodes.size(); i++) {
                NodeModel parent = allNodes.get(i).getParent();
                if (parent != null) {
                    Integer pIdx = nodeIndex.get(parent);
                    if (pIdx != null) {
                        model.nodes.get(i).parent = pIdx;
                    }
                }
            }

            // 3) 解析骨骼关节与逆绑定矩阵
            if (jointCount > 0) {
                SkinModel skin = gltf.getSkinModels().get(0);
                for (NodeModel j : skin.getJoints()) {
                    Integer ji = nodeIndex.get(j);
                    model.skinJoints.add(ji != null ? ji : -1);
                }
                AccessorModel ibm = skin.getInverseBindMatrices();
                if (ibm != null) {
                    FloatBuffer buf = ibm.read();
                    for (int i = 0; i < model.inverseBindMatrices.length && buf.hasRemaining(); i++) {
                        model.inverseBindMatrices[i] = buf.get();
                    }
                }
            }

            // 4) 解析网格
            for (MeshModel mesh : gltf.getMeshModels()) {
                for (MeshPrimitiveModel prim : mesh.getMeshPrimitiveModels()) {
                    VrmCreatureel.MeshData md = new VrmCreatureel.MeshData();
                    AccessorModel pos = prim.getAttributes().get("POSITION");
                    if (pos == null) continue;
                    FloatBuffer posBuf = pos.read();
                    md.positions = new float[posBuf.remaining()];
                    posBuf.get(md.positions);

                    AccessorModel normal = prim.getAttributes().get("NORMAL");
                    if (normal != null) {
                        FloatBuffer nb = normal.read();
                        md.normals = new float[nb.remaining()];
                        nb.get(md.normals);
                    }

                    AccessorModel joints = prim.getAttributes().get("JOINTS_0");
                    if (joints != null) {
                        FloatBuffer jb = joints.read();
                        md.joints = new int[jb.remaining()];
                        for (int i = 0; i < md.joints.length; i++) {
                            md.joints[i] = (int) jb.get();
                        }
                    }
                    AccessorModel weights = prim.getAttributes().get("WEIGHTS_0");
                    if (weights != null) {
                        FloatBuffer wb = weights.read();
                        md.weights = new float[wb.remaining()];
                        wb.get(md.weights);
                    }

                    AccessorModel uv = prim.getAttributes().get("TEXCOORD_0");
                    if (uv != null) {
                        FloatBuffer ub = uv.read();
                        md.uvs = new float[ub.remaining()];
                        ub.get(md.uvs);
                    }

                    AccessorModel idx = prim.getIndices();
                    if (idx != null) {
                        md.indices = toIntArray(idx.read());
                    } else {
                        md.indices = new int[md.positions.length / 3];
                        for (int i = 0; i < md.indices.length; i++) md.indices[i] = i;
                    }
                    md.vertexCount = md.positions.length / 3;
                    model.meshes.add(md);
                }
            }

            // 5) 解析动画
            for (AnimationModel anim : gltf.getAnimationModels()) {
                VrmCreatureel.AnimationClip clip = new VrmCreatureel.AnimationClip();
                clip.name = anim.getName() != null ? anim.getName() : "";
                for (AnimationChannelModel channel : anim.getChannels()) {
                    VrmCreatureel.AnimationChannel c = new VrmCreatureel.AnimationChannel();
                    NodeModel target = channel.getTarget();
                    Integer ti = target != null ? nodeIndex.get(target) : null;
                    c.nodeIndex = ti != null ? ti : -1;
                    c.path = channel.getTargetPath();
                    AnimationSamplerModel sampler = channel.getSampler();
                    if (sampler == null) continue;
                    c.interpolation = sampler.getInterpolation();
                    AccessorModel input = sampler.getInput();
                    AccessorModel output = sampler.getOutput();
                    if (input == null || output == null) continue;
                    FloatBuffer inBuf = input.read();
                    c.times = new float[inBuf.remaining()];
                    inBuf.get(c.times);
                    FloatBuffer outBuf = output.read();
                    c.values = new float[outBuf.remaining()];
                    outBuf.get(c.values);
                    if (c.times.length > 0) {
                        clip.duration = Math.max(clip.duration, c.times[c.times.length - 1]);
                    }
                    clip.channels.add(c);
                }
                model.animations.add(clip);
            }

            // 6) 从模型内提取基础色贴图（VRM 1.0/2.0 的材质/贴图引用方式不同，这里做兼容提取）
            model.textureBytes = extractTextureBytes(gltf);

            LOGGER.info("Loaded VRM '{}' version={} meshes={} nodes={} joints={} anims={} tex={}",
                    location, model.vrmVersion, model.meshes.size(), model.nodes.size(), model.skinJoints.size(), model.animations.size(),
                    model.textureBytes != null ? model.textureBytes.length + "B" : "none");
            return model;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to load VRM model '{}'", location, e);
            return new VrmCreatureel(0);
        }
    }

    /**
     * 从模型内提取基础色贴图字节。
     * 兼容处理：
     *  - VRM 2.0：标准 pbrMetallicRoughness.baseColorTexture
     *  - VRM 0.x：MToon 材质定义在 VRM.materialProperties 扩展中，
     *             其 textures[0] 为全局图片索引（base color）
     *  - glTF 1.0 兼容：diffuseTexture
     * 若贴图为内嵌数据（GLB buffer 或 data URI）则返回其字节，否则返回 null。
     */
    private static byte[] extractTextureBytes(GltfModel gltf) {
        // 1) 标准材质路径（VRM 2.0 / glTF）
        for (MaterialModel mat : gltf.getMaterialModels()) {
            TextureModel tex = mat.getBaseColorTexture();
            if (tex == null) {
                tex = mat.getDiffuseTexture(); // glTF 1.0 兼容
            }
            if (tex == null) continue;
            byte[] data = imageBytes(tex.getImage());
            if (data != null) return data;
        }

        // 2) VRM 0.x 扩展材质路径（MToon：materialProperties[].textures[]）
        try {
            Map<String, Object> ext = gltf.getExtensions();
            if (ext != null) {
                Object vrmExt = ext.get("VRM");
                if (vrmExt instanceof Map<?, ?> vrm) {
                    Object matProps = vrm.get("materialProperties");
                    if (matProps instanceof List<?> list && !list.isEmpty()) {
                        Object first = list.get(0);
                        if (first instanceof Map<?, ?> mp) {
                            Object textures = mp.get("textures");
                            if (textures instanceof List<?> tList && !tList.isEmpty()) {
                                int imgIdx = ((Number) tList.get(0)).intValue();
                                List<ImageModel> images = gltf.getImageModels();
                                if (imgIdx >= 0 && imgIdx < images.size()) {
                                    byte[] data = imageBytes(images.get(imgIdx));
                                    if (data != null) return data;
                                }
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to extract VRM 0.x MToon texture: {}", e.getMessage());
        }
        return null;
    }

    /** 取图片字节（GLB buffer 内嵌优先，其次 data URI） */
    private static byte[] imageBytes(ImageModel img) {
        if (img == null) return null;
        byte[] data = img.getImageData();
        if (data != null && data.length > 0) return data;
        String uri = img.getUri();
        if (uri != null && uri.startsWith("data:")) {
            return decodeDataUri(uri);
        }
        return null;
    }

    /** 解析 data URI（data:image/png;base64,xxxx）为字节 */
    private static byte[] decodeDataUri(String uri) {
        int comma = uri.indexOf(',');
        if (comma < 0) return null;
        String meta = uri.substring(0, comma);
        String payload = uri.substring(comma + 1);
        try {
            if (meta.contains(";base64")) {
                return java.util.Base64.getDecoder().decode(payload);
            } else {
                return java.net.URLDecoder.decode(payload, java.nio.charset.StandardCharsets.UTF_8)
                        .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to decode data URI texture: {}", e.getMessage());
            return null;
        }
    }

    private static int[] toIntArray(java.nio.Buffer buffer) {
        if (buffer instanceof IntBuffer ib) {
            IntBuffer dup = ib.duplicate();
            int[] out = new int[dup.remaining()];
            dup.get(out);
            return out;
        } else if (buffer instanceof ShortBuffer sb) {
            ShortBuffer dup = sb.duplicate();
            int[] out = new int[dup.remaining()];
            for (int i = 0; i < out.length; i++) out[i] = dup.get() & 0xFFFF;
            return out;
        } else if (buffer instanceof ByteBuffer bb) {
            ByteBuffer dup = bb.duplicate();
            int[] out = new int[dup.remaining()];
            for (int i = 0; i < out.length; i++) out[i] = dup.get() & 0xFF;
            return out;
        } else if (buffer instanceof FloatBuffer fb) {
            FloatBuffer dup = fb.duplicate();
            int[] out = new int[dup.remaining()];
            for (int i = 0; i < out.length; i++) out[i] = (int) dup.get();
            return out;
        }
        return new int[0];
    }
}
