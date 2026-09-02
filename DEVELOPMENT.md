# 开发者文档

本文件面向模组开发与打包发布；玩家请阅读 [README.md](README.md)。

## 环境要求

- JDK 21（64 位）
- IntelliJ IDEA（Community 版即可）

## 本地构建

```bash
./gradlew build
```

产物在 `build/libs/` 下，主 jar 为 `vrmcreature-1.0.0.jar`。

## 打包发布（GitHub Actions）

推送 tag 即可自动触发构建并发布到 Releases：

```bash
git tag v1.0.0
git push origin v1.0.0
```

也可在仓库 Actions 页面手动触发 `Build & Release`（workflow_dispatch）。

## 放置默认模型（可选）

如需把默认模型打进 jar，将 `.vrm` 重命名为 `model.vrm` 放到：

```
src/main/resources/assets/vrmcreature/vrm/model.vrm
```

（不内置时，玩家通过资源包覆盖同一路径即可替换模型。）
