# SmartWardrobe 本地构建说明

本项目所有软件环境依赖均内置在项目目录下，不依赖系统全局安装的 JDK / Gradle / Android Studio。

## 工具链位置（全部在本目录内）

| 组件 | 版本 | 路径（相对项目根） |
| --- | --- | --- |
| JDK | Temurin 17.0.21 | `.toolchain/jdk/jdk-17.0.21+9` |
| Gradle | 8.9 | `.toolchain/gradle/gradle-8.9` |
| Android SDK | platform 34、35 / build-tools 34.0.0、35.0.0 / platform-tools | `.toolchain/android-sdk` |
| Gradle Wrapper | 8.9（指向本地 zip） | `gradle/wrapper/gradle-8.9-bin.zip` |

## 一键配置环境

在终端进入项目根目录后执行：

```bat
env.bat
```

该脚本会设置 `JAVA_HOME`、`ANDROID_HOME`、`GRADLE_HOME` 并把 JDK/Gradle/platform-tools 加入 PATH。

## 编译命令

```bat
env.bat
gradle :app:compileDebugKotlin --no-daemon
```

或直接使用 wrapper（已指向本地 Gradle zip，无需外网下载 Gradle）：

```bat
gradlew :app:compileDebugKotlin --no-daemon
```

> 首次构建需要联网下载 Maven/Google 依赖（Compose、Room、Hilt 等），这些依赖会缓存到 `%USERPROFILE%\.gradle\caches`。

## 关键依赖版本

- Kotlin 2.0.21 / KSP 2.0.21-1.0.28
- AGP 8.7.3 / Gradle 8.9
- Room 2.7.2（支持 Kotlin 2.0 + KSP2）
- Hilt 2.51.1 / Coil 3.0.0 / ML Kit Subject Segmentation 16.0.0-beta1
