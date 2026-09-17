package com.example.vrmcreature.entity.client;

import com.example.vrmcreature.VrmCreature;
import com.example.vrmcreature.api.event.VrmModelLoadEvent;
import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * VRM 模型加载器：通过 jgltf 解析模型目录中的 .vrm/.glb 文件。
 * 提取：网格顶点、节点层级、骨骼蒙皮、动画片段。
 * 模型目录：<游戏根目录>/versions/<版本名>/vrmcreature/vrm/
 * 例如：.minecraft/versions/1.21.1/vrmcreature/vrm/alice.vrm
 * 支持多个模型，每个模型一个文件（文件名使用英文字母，扩展名 .vrm 或 .glb）。
 */
public class VrmCreatureelLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("VRMCreature");
    private static final String DEFAULT_MODEL = "model";

    /** 模型根目录：<游戏根目录>/versions/<版本名>/vrmcreature/vrm */
    public static Path vrmDir() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        String version = Minecraft.getInstance().getLaunchedVersion();
        return gameDir.resolve("versions").resolve(version)
                .resolve(VrmCreature.MODID).resolve("vrm");
    }

    /** 确保模型目录存在（不存在则自动创建），方便玩家直接放入模型文件。 */
    public static void ensureVrmDir() {
        Path dir = vrmDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.warn("Failed to create VRM model dir {}", dir, e);
        }
    }

    /** 列出 vrm 目录下所有可用模型名（去掉扩展名，如 alice.vrm -> alice）。 */
    public static List<String> listModelNames() {
        ensureVrmDir();
        List<String> names = new ArrayList<>();
        Path dir = vrmDir();
        if (!Files.isDirectory(dir)) {
            return names;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> {
                        String fn = p.getFileName().toString().toLowerCase();
                        return fn.endsWith(".vrm") || fn.endsWith(".glb");
                    })
                    .forEach(p -> {
                        String fn = p.getFileName().toString();
                        String name = fn.substring(0, fn.lastIndexOf('.'));
                        if (!name.isEmpty()) names.add(name);
                    });
        } catch (IOException e) {
            LOGGER.warn("Failed to list VRM models in {}", dir, e);
        }
        names.sort(Comparator.naturalOrder());
        return names;
    }

    /** 将模型名解析为模型文件路径：优先 .vrm，其次 .glb。 */
    public static Path modelPath(String name) {
        if (name == null || name.isEmpty()) {
            name = DEFAULT_MODEL;
        }
        Path dir = vrmDir();
        Path vrm = dir.resolve(name + ".vrm");
        if (Files.isRegularFile(vrm)) {
            return vrm;
        }
        return dir.resolve(name + ".glb");
    }

    /** 模型显示名：优先返回配置中的中文显示名，未配置时返回英文模型名。 */
    public static String displayName(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            modelName = DEFAULT_MODEL;
        }
        String cn = com.example.vrmcreature.config.VrmModelConfig.load(modelName).displayName;
        return (cn != null && !cn.isEmpty()) ? cn : modelName;
    }

    /**
     * 按模型名加载模型。指定模型不存在时回退到第一个可用模型；
     * 目录为空时回退默认 "model"（即使缺失也会返回空模型，由渲染端跳过）。
     */
    public static VrmCreatureel loadByName(String name) {
        if (name == null || name.isEmpty()) {
            name = DEFAULT_MODEL;
        }
        List<String> names = listModelNames();
        if (!names.contains(name)) {
            if (!names.isEmpty()) {
                name = names.get(0);
            } else {
                name = DEFAULT_MODEL;
            }
        }
        // 模型加载钩子：依赖方可修改解析后的模型数据（网格/节点/动画）再参与渲染
        VrmCreatureel result = load(modelPath(name));
        NeoForge.EVENT_BUS.post(new VrmModelLoadEvent(name, result));
        return result;
    }

    public static VrmCreatureel load(Path path) {
        if (!Files.isRegularFile(path)) {
            LOGGER.warn("VRM model not found: {}", path);
            return new VrmCreatureel(0);
        }

        try {
            GltfModel gltf = new GltfModelReader().read(path);

            // 0) 检测 VRM 版本（1.0 扩展名 "VRM"，2.0 扩展名 "VRMC_vrm"）
            String vrmVersion = "glTF";
            List<String> extUsed = gltf.getExtensionsModel() != null
                    ? gltf.getExtensionsModel().getExtensionsUsed() : null;
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
                float[] tr = allNodes.get(i).getTranslation();
                if (tr != null && tr.length >= 3) {
                    n.translation[0] = tr[0];
                    n.translation[1] = tr[1];
                    n.translation[2] = tr[2];
                }
                float[] rt = allNodes.get(i).getRotation();
                if (rt != null && rt.length >= 4) {
                    n.rotation[0] = rt[0];
                    n.rotation[1] = rt[1];
                    n.rotation[2] = rt[2];
                    n.rotation[3] = rt[3];
                }
                float[] sc = allNodes.get(i).getScale();
                if (sc != null && sc.length >= 3) {
                    n.scale[0] = sc[0];
                    n.scale[1] = sc[1];
                    n.scale[2] = sc[2];
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
                    FloatBuffer buf = readFloats(ibm);
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
                    FloatBuffer posBuf = readFloats(pos);
                    md.positions = new float[posBuf.remaining()];
                    posBuf.get(md.positions);

                    AccessorModel normal = prim.getAttributes().get("NORMAL");
                    if (normal != null) {
                        FloatBuffer nb = readFloats(normal);
                        md.normals = new float[nb.remaining()];
                        nb.get(md.normals);
                    }

                    AccessorModel joints = prim.getAttributes().get("JOINTS_0");
                    if (joints != null) {
                        md.joints = readInts(joints);
                    }
                    AccessorModel weights = prim.getAttributes().get("WEIGHTS_0");
                    if (weights != null) {
                        FloatBuffer wb = readFloats(weights);
                        md.weights = new float[wb.remaining()];
                        wb.get(md.weights);
                    }

                    AccessorModel uv = prim.getAttributes().get("TEXCOORD_0");
                    if (uv != null) {
                        FloatBuffer ub = readFloats(uv);
                        md.uvs = new float[ub.remaining()];
                        ub.get(md.uvs);
                    }

                    AccessorModel idx = prim.getIndices();
                    if (idx != null) {
                        md.indices = readInts(idx);
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
                for (AnimationModel.Channel channel : anim.getChannels()) {
                    VrmCreatureel.AnimationChannel c = new VrmCreatureel.AnimationChannel();
                    NodeModel target = channel.getNodeModel();
                    Integer ti = target != null ? nodeIndex.get(target) : null;
                    c.nodeIndex = ti != null ? ti : -1;
                    c.path = channel.getPath();
                    AnimationModel.Sampler sampler = channel.getSampler();
                    if (sampler == null) continue;
                    c.interpolation = sampler.getInterpolation().name();
                    AccessorModel input = sampler.getInput();
                    AccessorModel output = sampler.getOutput();
                    if (input == null || output == null) continue;
                    FloatBuffer inBuf = readFloats(input);
                    c.times = new float[inBuf.remaining()];
                    inBuf.get(c.times);
                    FloatBuffer outBuf = readFloats(output);
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
                    path, model.vrmVersion, model.meshes.size(), model.nodes.size(), model.skinJoints.size(), model.animations.size(),
                    model.textureBytes != null ? model.textureBytes.length + "B" : "none");
            return model;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to load VRM model '{}'", path, e);
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
        // jgltf 2.0.4：遍历纹理列表，返回第一张可用的图片字节
        for (TextureModel tex : gltf.getTextureModels()) {
            byte[] data = imageBytes(tex.getImageModel());
            if (data != null) return data;
        }
        return null;
    }

    /** 取图片字节（GLB buffer 内嵌优先，其次 data URI） */
    private static byte[] imageBytes(ImageModel img) {
        if (img == null) return null;
        java.nio.ByteBuffer data = img.getImageData();
        if (data != null && data.hasRemaining()) {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            return bytes;
        }
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

    /** 将访问器数据读为 FloatBuffer（本机字节序）。 */
    private static FloatBuffer readFloats(AccessorModel acc) {
        if (acc == null) return null;
        AccessorFloatData d = AccessorDatas.createFloat(acc);
        return d.createByteBuffer().order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    /** 将访问器数据读为 int[]（兼容 UNSIGNED_BYTE/SHORT/INT 与 FLOAT）。 */
    private static int[] readInts(AccessorModel acc) {
        if (acc == null) return null;
        AccessorData d = AccessorDatas.create(acc);
        java.nio.ByteBuffer bb = d.createByteBuffer().duplicate().order(ByteOrder.nativeOrder());
        if (d instanceof de.javagl.jgltf.model.AccessorIntData) {
            IntBuffer ib = bb.asIntBuffer();
            int[] out = new int[ib.remaining()];
            ib.get(out);
            return out;
        } else if (d instanceof de.javagl.jgltf.model.AccessorShortData) {
            ShortBuffer sb = bb.asShortBuffer();
            int[] out = new int[sb.remaining()];
            for (int i = 0; i < out.length; i++) out[i] = sb.get() & 0xFFFF;
            return out;
        } else if (d instanceof de.javagl.jgltf.model.AccessorByteData) {
            int[] out = new int[bb.remaining()];
            for (int i = 0; i < out.length; i++) out[i] = bb.get() & 0xFF;
            return out;
        } else if (d instanceof AccessorFloatData) {
            FloatBuffer fb = bb.asFloatBuffer();
            int[] out = new int[fb.remaining()];
            for (int i = 0; i < out.length; i++) out[i] = (int) fb.get();
            return out;
        }
        return new int[0];
    }
}
