package com.example.vrmcreature.entity.client;

import java.util.ArrayList;
import java.util.List;

/**
 * 从 VRM(.glb) 解析出的完整模型数据：
 * 网格 + 节点层级 + 骨骼蒙皮 + 动画片段。
 */
public class VrmCreatureel {

    /** 网格数据（含蒙皮信息） */
    public static class MeshData {
        public float[] positions; // xyz 连续
        public float[] normals;   // 可选
        public float[] uvs;       // 可选，TEXCOORD_0，每顶点2个float（u,v）
        public int[] indices;     // 三角形索引
        public int[] joints;      // 每顶点4个关节索引（可为 null）
        public float[] weights;   // 每顶点4个蒙皮权重（可为 null）
        public int vertexCount;
    }

    /** 场景节点（含默认 TRS） */
    public static class Node {
        public int index;
        public int parent = -1;
        public float[] translation = {0f, 0f, 0f};
        public float[] rotation = {0f, 0f, 0f, 1f}; // xyzw
        public float[] scale = {1f, 1f, 1f};
    }

    /** 动画通道：针对某节点某路径（translation/rotation/scale）的关键帧 */
    public static class AnimationChannel {
        public int nodeIndex = -1;
        public String path = "translation";
        public String interpolation = "LINEAR";
        public float[] times;
        public float[] values;
    }

    /** 动画片段 */
    public static class AnimationClip {
        public String name = "";
        public float duration = 0f;
        public final List<AnimationChannel> channels = new ArrayList<>();
    }

    public final List<MeshData> meshes = new ArrayList<>();
    public final List<Node> nodes = new ArrayList<>();
    public final List<Integer> skinJoints = new ArrayList<>(); // 关节对应的节点索引
    public final float[] inverseBindMatrices; // 16 * jointCount
    public final List<AnimationClip> animations = new ArrayList<>();

    /** VRM 版本标识："VRM 0.x" / "VRM 2.0" / "glTF(未知)" */
    public String vrmVersion = "unknown";
    /** 从模型内提取的基础色贴图字节（PNG/JPG），可为 null */
    public byte[] textureBytes;

    public VrmCreatureel(int jointCount) {
        this.inverseBindMatrices = jointCount > 0 ? new float[jointCount * 16] : new float[0];
    }

    public boolean isEmpty() {
        return meshes.isEmpty();
    }
}
