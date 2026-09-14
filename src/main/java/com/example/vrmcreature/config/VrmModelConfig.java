package com.example.vrmcreature.config;

import com.example.vrmcreature.VrmCreature;
import com.example.vrmcreature.api.event.VrmConfigLoadEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * 每模型独立配置：一个模型对应一个 JSON 配置文件。
 * 配置目录：<游戏根目录>/versions/<版本名>/vrmcreature/config/<模型名>.json
 * 例如：.minecraft/versions/1.21.1/vrmcreature/config/alice.json
 *
 * 每个模型可独立设置：锁定状态、可自然刷新、阵营、基础属性、刷新权重、行为、动作偏好、刷新群系、掉落物。
 * 自然刷新按各模型自己的群系 + 权重进行（权重越大越容易被刷出）。
 */
public class VrmModelConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("VRMCreature");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** 配置根目录：<游戏根目录>/versions/<版本名>/vrmcreature/config */
    public static Path configDir() {
        Path gameDir = FMLPaths.GAMEDIR.get();
        String version = FMLLoader.getGameVersion();
        return gameDir.resolve("versions").resolve(version)
                .resolve(VrmCreature.MODID).resolve("config");
    }

    /** 确保配置目录存在（不存在则自动创建） */
    public static void ensureConfigDir() {
        Path dir = configDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.warn("Failed to create config dir {}", dir, e);
        }
    }

    /** 列出所有已有配置的模型名（config 目录下 *.json，去扩展名） */
    public static List<String> listConfigured() {
        ensureConfigDir();
        List<String> names = new ArrayList<>();
        Path dir = configDir();
        if (!Files.isDirectory(dir)) {
            return names;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> {
                        String fn = p.getFileName().toString().toLowerCase();
                        return fn.endsWith(".json");
                    })
                    .forEach(p -> {
                        String fn = p.getFileName().toString();
                        String name = fn.substring(0, fn.lastIndexOf('.'));
                        if (!name.isEmpty()) names.add(name);
                    });
        } catch (IOException e) {
            LOGGER.warn("Failed to list model configs in {}", dir, e);
        }
        names.sort(Comparator.naturalOrder());
        return names;
    }

    private static Path fileOf(String modelName) {
        return configDir().resolve(modelName + ".json");
    }

    /** 读取指定模型的配置；文件不存在或解析失败时返回默认配置。 */
    public static Data load(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            modelName = "model";
        }
        Path file = fileOf(modelName);
        if (Files.isRegularFile(file)) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                Data d = GSON.fromJson(json, Data.class);
                if (d != null) {
                    if (d.spawnBiomes == null) d.spawnBiomes = new ArrayList<>();
                    if (d.loot == null) d.loot = new ArrayList<>();
                    return d;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to load model config {}: {}", file, e.getMessage());
            }
        }
        return new Data();
    }

    /**
     * 读取指定模型配置，并在生效前触发 {@link VrmConfigLoadEvent} 钩子。
     * 监听者可修改返回的 {@link Data} 字段，修改结果作为本次生效的配置。
     * 仅用于「配置将实际作用于实体」的路径（实体生成 / 应用配置），
     * 渲染等高频读取路径请使用 {@link #load(String)}（不触发钩子）。
     */
    public static Data loadWithHooks(String modelName) {
        Data d = load(modelName);
        NeoForge.EVENT_BUS.post(new VrmConfigLoadEvent(modelName, d));
        return d;
    }

    /** 保存指定模型的配置到 JSON 文件。 */
    public static void save(String modelName, Data data) {
        ensureConfigDir();
        try {
            Files.writeString(fileOf(modelName), GSON.toJson(data), StandardCharsets.UTF_8);
            LOGGER.info("Saved model config {}", fileOf(modelName));
        } catch (IOException e) {
            LOGGER.error("Failed to save model config {}", fileOf(modelName), e);
        }
    }

    /** 删除指定模型的配置文件（不存在则无操作）。 */
    public static void delete(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return;
        }
        try {
            Files.deleteIfExists(fileOf(modelName));
            LOGGER.info("Deleted model config {}", fileOf(modelName));
        } catch (IOException e) {
            LOGGER.error("Failed to delete model config {}", fileOf(modelName), e);
        }
    }

    /** 当前群系是否允许某模型的配置（空列表 = 全部群系） */
    public static boolean isBiomeAllowed(Data d, String biomeId) {
        if (d == null || d.spawnBiomes == null || d.spawnBiomes.isEmpty()) {
            return true;
        }
        return d.spawnBiomes.contains(biomeId);
    }

    /** 是否存在任意已配置模型：允许自然刷新 且 允许当前群系（自然刷新谓词用） */
    public static boolean isAnySpawnAllowed(String biomeId) {
        for (String name : listConfigured()) {
            Data d = load(name);
            if (d.canSpawn && isBiomeAllowed(d, biomeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 自然刷新选模型：从「允许当前群系」的已配置模型中按 spawnWeight 权重随机选一个。
     * 权重越大越容易被选中；无任何可用模型时返回 null。
     */
    public static String pickModelForBiome(String biomeId, Random random) {
        List<String> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (String name : listConfigured()) {
            Data d = load(name);
            if (d.canSpawn && isBiomeAllowed(d, biomeId)) {
                candidates.add(name);
                int w = Math.max(1, d.spawnWeight);
                weights.add(w);
                total += w;
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int r = random.nextInt(total);
        for (int i = 0; i < candidates.size(); i++) {
            r -= weights.get(i);
            if (r < 0) {
                return candidates.get(i);
            }
        }
        return candidates.get(0);
    }

    /** 单个模型的完整配置数据。 */
    public static class Data {
        /** 是否已创建并锁定属性（锁定后 GUI 不可再改） */
        public boolean locked = false;
        /** 是否允许自然刷新 */
        public boolean canSpawn = true;
        /** 阵营：0 敌方 1 中立 2 我方 */
        public int faction = 0;
        /** 最大生命值 */
        public double health = 20.0D;
        /** 攻击伤害 */
        public double damage = 3.0D;
        /** 移动速度 */
        public double speed = 0.25D;
        /** 自然刷新权重（越大越容易刷出） */
        public int spawnWeight = 10;
        /** 行为模式：0 静止 1 巡逻 2 跟随玩家 3 主动攻击 */
        public int behavior = 1;
        /** 动作偏好：0 待机 1 走路 2 攻击 */
        public int animPref = 0;
        /** 允许自然刷新的群系 ID 列表（空 = 全部群系） */
        public List<String> spawnBiomes = new ArrayList<>();
        /** 掉落物配置行：物品ID;数量;概率（如 minecraft:diamond;1;0.3） */
        public List<String> loot = new ArrayList<>();
    }
}
