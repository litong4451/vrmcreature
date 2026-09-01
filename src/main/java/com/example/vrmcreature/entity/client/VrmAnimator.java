package com.example.vrmcreature.entity.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

/**
 * VRM 骨骼动画计算器：
 * 给定模型、当前播放的动画片段与时间，计算每个关节的蒙皮矩阵。
 * 支持 glTF 标准的 translation/rotation/scale 通道，插值方式 LINEAR/STEP。
 */
public class VrmAnimator {
    private final VrmCreatureel model;
    private final Matrix4f[] local;
    private final Matrix4f[] global;
    private final Matrix4f[] skinMatrices;

    public VrmAnimator(VrmCreatureel model) {
        this.model = model;
        int n = model.nodes.size();
        this.local = new Matrix4f[n];
        this.global = new Matrix4f[n];
        for (int i = 0; i < n; i++) {
            local[i] = new Matrix4f();
            global[i] = new Matrix4f();
        }
        int jc = model.skinJoints.size();
        this.skinMatrices = new Matrix4f[jc];
        for (int i = 0; i < jc; i++) {
            skinMatrices[i] = new Matrix4f();
        }
    }

    /** 更新所有蒙皮矩阵 */
    public void update(float time, VrmCreatureel.AnimationClip clip) {
        int n = model.nodes.size();
        // 1) 初始化各节点默认局部矩阵（T * R * S）
        for (int i = 0; i < n; i++) {
            VrmCreatureel.Node node = model.nodes.get(i);
            local[i].identity()
                    .translate(node.translation[0], node.translation[1], node.translation[2])
                    .rotate(new Quaternionf(node.rotation[0], node.rotation[1], node.rotation[2], node.rotation[3]))
                    .scale(node.scale[0], node.scale[1], node.scale[2]);
        }

        // 2) 应用动画通道覆盖局部矩阵
        if (clip != null) {
            for (VrmCreatureel.AnimationChannel ch : clip.channels) {
                if (ch.nodeIndex < 0 || ch.nodeIndex >= n) continue;
                float[] trs = sample(ch, time);
                if (trs == null) continue;
                switch (ch.path) {
                    case "translation" -> {
                        local[ch.nodeIndex].m30(trs[0]);
                        local[ch.nodeIndex].m31(trs[1]);
                        local[ch.nodeIndex].m32(trs[2]);
                    }
                    case "rotation" -> {
                        Quaternionf q = new Quaternionf(trs[0], trs[1], trs[2], trs[3]);
                        Matrix4f r = new Matrix4f().rotate(q);
                        Matrix4f s = new Matrix4f()
                                .scale(local[ch.nodeIndex].m00(), local[ch.nodeIndex].m11(), local[ch.nodeIndex].m22());
                        local[ch.nodeIndex].identity()
                                .translate(local[ch.nodeIndex].m30(), local[ch.nodeIndex].m31(), local[ch.nodeIndex].m32())
                                .mul(r).mul(s);
                    }
                    case "scale" -> {
                        local[ch.nodeIndex].m00(trs[0]);
                        local[ch.nodeIndex].m11(trs[1]);
                        local[ch.nodeIndex].m22(trs[2]);
                    }
                }
            }
        }

        // 3) 合成全局矩阵
        for (int i = 0; i < n; i++) {
            int parent = model.nodes.get(i).parent;
            if (parent < 0) {
                global[i].set(local[i]);
            } else {
                global[i].set(global[parent]).mul(local[i]);
            }
        }

        // 4) 蒙皮矩阵 = 全局关节矩阵 * 逆绑定矩阵
        for (int i = 0; i < model.skinJoints.size(); i++) {
            int nodeIdx = model.skinJoints.get(i);
            Matrix4f ib = new Matrix4f();
            ib.set(model.inverseBindMatrices, i * 16);
            skinMatrices[i].set(global[nodeIdx]).mul(ib);
        }
    }

    /** 采样通道在 time 处的分量值 */
    private float[] sample(VrmCreatureel.AnimationChannel ch, float time) {
        int comp = ch.path.equals("rotation") ? 4 : 3;
        float[] times = ch.times;
        float[] values = ch.values;
        if (times.length == 0) return null;
        float[] out = new float[comp];

        if (time <= times[0]) {
            System.arraycopy(values, 0, out, 0, comp);
            return out;
        }
        if (time >= times[times.length - 1]) {
            System.arraycopy(values, (times.length - 1) * comp, out, 0, comp);
            return out;
        }
        int k = 0;
        while (k < times.length - 1 && times[k + 1] < time) k++;
        float t0 = times[k], t1 = times[k + 1];
        float alpha = (time - t0) / (t1 - t0);
        if ("STEP".equalsIgnoreCase(ch.interpolation)) alpha = 0f;

        for (int c = 0; c < comp; c++) {
            float a = values[k * comp + c];
            float b = values[(k + 1) * comp + c];
            if ("rotation".equals(ch.path)) {
                // 用 nlerp 近似球面插值
                float[] qa = new float[]{values[k * 4], values[k * 4 + 1], values[k * 4 + 2], values[k * 4 + 3]};
                float[] qb = new float[]{values[(k + 1) * 4], values[(k + 1) * 4 + 1], values[(k + 1) * 4 + 2], values[(k + 1) * 4 + 3]};
                float dot = qa[0] * qb[0] + qa[1] * qb[1] + qa[2] * qb[2] + qa[3] * qb[3];
                if (dot < 0) {
                    qb[0] = -qb[0]; qb[1] = -qb[1]; qb[2] = -qb[2]; qb[3] = -qb[3];
                }
                out[c] = qa[c] * (1 - alpha) + qb[c] * alpha;
            } else {
                out[c] = a * (1 - alpha) + b * alpha;
            }
        }
        if ("rotation".equals(ch.path)) {
            float len = (float) Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2] + out[3] * out[3]);
            if (len > 1e-6f) {
                out[0] /= len; out[1] /= len; out[2] /= len; out[3] /= len;
            }
        }
        return out;
    }

    /** 对单个顶点做线性混合蒙皮 */
    public Vector3f applySkin(VrmCreatureel.MeshData mesh, int vertexIndex, float x, float y, float z) {
        if (mesh.joints == null || mesh.weights == null || skinMatrices.length == 0) {
            return new Vector3f(x, y, z);
        }
        float ox = 0, oy = 0, oz = 0;
        for (int k = 0; k < 4; k++) {
            int ji = mesh.joints[vertexIndex * 4 + k];
            float w = mesh.weights[vertexIndex * 4 + k];
            if (ji < 0 || ji >= skinMatrices.length || w == 0f) continue;
            Vector4f tmp = new Vector4f(x, y, z, 1f);
            skinMatrices[ji].transform(tmp);
            ox += w * tmp.x;
            oy += w * tmp.y;
            oz += w * tmp.z;
        }
        return new Vector3f(ox, oy, oz);
    }
}
