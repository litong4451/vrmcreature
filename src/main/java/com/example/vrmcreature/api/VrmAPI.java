package com.example.vrmcreature.api;

import com.example.vrmcreature.config.VrmModelConfig;
import com.example.vrmcreature.entity.ModEntities;
import com.example.vrmcreature.entity.VrmMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * VrmCreature 对外 API 门面。
 *
 * 其他模组可像使用普通库一样集成本模组能力：
 *  - 构建期：依赖本模组 jar（implementation / compileOnly）；
 *  - 运行期：mods 目录放入 vrmcreature jar（或作为库 jar 引入）；
 *  - 可选：在自身 neoforge.mods.toml 的 [[dependencies.modid]] 声明依赖 vrmcreature，保证加载顺序。
 *
 * 扩展点（钩子）总览：
 *  1. {@link VrmAPI}：以代码注册/读取/删除模型配置、以代码生成指定模型的生物；
 *  2. {@link com.example.vrmcreature.api.event.VrmModelPickEvent}：自然刷新选中模型后，可替换模型或取消本次刷新；
 *  3. {@link com.example.vrmcreature.api.event.VrmMobSpawnEvent}：实体应用配置后，可调整属性/行为/附加状态；
 *  4. {@link com.example.vrmcreature.api.event.VrmLootDropEvent}：实体死亡掉落前，可修改掉落表或取消默认掉落；
 *  5. {@link com.example.vrmcreature.api.event.VrmConfigLoadEvent}：配置读取生效前，可修改本次生效的配置；
 *  6. {@link com.example.vrmcreature.api.event.VrmModelLoadEvent}：模型文件解析完成后，可修改网格/节点/动画；
 *  7. {@link com.example.vrmcreature.api.event.VrmBehaviorEvent}：AI 注册前，可取消默认 AI 并完全自定义行为；
 *  8. {@link com.example.vrmcreature.api.event.VrmAnimEvent}：每帧选择动作前，可指定/覆盖实体播放的动画片段。
 *
 * 事件监听均使用 NeoForge 游戏总线（NeoForge.EVENT_BUS），
 * 在你的模组中用 @SubscribeEvent 注册，或使用 @EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)。
 */
public final class VrmAPI {

    private VrmAPI() {
    }

    /** 是否存在某模型的配置 */
    public static boolean isConfigured(String modelName) {
        return VrmModelConfig.listConfigured().contains(modelName);
    }

    /** 读取某模型的配置（不存在时返回默认值，不会写盘） */
    public static VrmModelConfig.Data getModel(String modelName) {
        return VrmModelConfig.load(modelName);
    }

    /** 已配置的模型名列表（按字典序） */
    public static List<String> listModels() {
        return VrmModelConfig.listConfigured();
    }

    /** 以代码注册/覆盖一个模型的配置（立即写入配置文件） */
    public static void registerModel(String modelName, VrmModelConfig.Data data) {
        VrmModelConfig.save(modelName, data);
    }

    /** 删除一个模型的配置文件（不存在则无操作） */
    public static void removeModel(String modelName) {
        VrmModelConfig.delete(modelName);
    }

    /**
     * 在指定位置生成一个指定模型的 VRM 生物，并应用该模型配置。
     * 返回 true 表示生成成功（实体已加入世界）。
     */
    public static boolean spawnVrm(Level level, BlockPos pos, String modelName) {
        VrmMob mob = ModEntities.VRM_MOB.get().create(level);
        if (mob == null) {
            return false;
        }
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        mob.applyModelConfig(modelName);
        return level.addFreshEntity(mob);
    }
}
