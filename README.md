# SmartWardrobe

智能电子衣橱 — 一款原生 Android 应用。通过拍照/相册导入真实衣物，端侧自动抠图 + 多模态 AI 打标建立数字化衣橱，并结合天气、场合与用户特征由 AI 生成结构化穿搭方案。

## 功能

- **智能录入**：CameraX 拍照 / PhotoPicker 选图 → ML Kit Subject Segmentation 端侧抠图 → DeepSeek 视觉模型打标 → 人工微调入库
- **数字衣橱**：品类 / 季节 / 颜色色卡 / 风格多维筛选，透明背景网格展示，穿着次数统计
- **AI 穿搭造型师**：按季节与温度检索库存，生成 2-3 套搭配方案与推荐理由，杂志风拼贴卡片展示，支持收藏与换一批
- **虚拟试衣间**：ML Kit Selfie Segmentation 提取真人形象，按部位叠加衣物图层，一键套用 AI 搭配或自行换装
- **日历与统计**：穿着记录日历、衣物穿着排行
- **设置**：DeepSeek API Key、体型肤色特征、主题模式

## 技术栈

- Kotlin 2.0.21 + Coroutines / Flow
- Jetpack Compose + Material 3（动态配色 / 深浅色）
- MVVM + Clean Architecture（UI → Domain → Data）
- Hilt 依赖注入
- Room（衣物 / 搭配 / 穿着记录）+ DataStore（偏好 / API Key）
- Retrofit2 + OkHttp3 + kotlinx.serialization
- Coil 3 + CameraX
- ML Kit Subject Segmentation（衣物抠图）+ Selfie Segmentation（人像提取）
- DeepSeek API：`deepseek-v4-flash-vision-exp`（视觉打标）、`deepseek-v4-flash`（穿搭生成）

## 构建

本项目内置了完整工具链（JDK 17 / Gradle 8.9 / Android SDK），不依赖系统全局环境。

```bat
env.bat
gradle :app:compileDebugKotlin --no-daemon
```

或直接使用 wrapper（已指向本地 Gradle zip）：

```bat
gradlew assembleDebug
```

APK 产物：`app/build/outputs/apk/debug/app-debug.apk`

> AI 功能需在应用「设置」页填入 DeepSeek API Key；抠图、试衣、衣橱管理等端侧功能无需联网即可使用。
