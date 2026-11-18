@echo off
REM SmartWardrobe 本地构建环境配置脚本
REM 用法：在需要编译的终端里先执行  env.bat  ，再运行 gradlew 命令。
set "TOOLCHAIN=%~dp0.toolchain"
set "JAVA_HOME=%TOOLCHAIN%\jdk\jdk-17.0.21+9"
set "ANDROID_HOME=%TOOLCHAIN%\android-sdk"
set "ANDROID_SDK_ROOT=%TOOLCHAIN%\android-sdk"
set "GRADLE_HOME=%TOOLCHAIN%\gradle\gradle-8.9"
set "PATH=%JAVA_HOME%\bin;%GRADLE_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"
echo [SmartWardrobe] JAVA_HOME=%JAVA_HOME%
echo [SmartWardrobe] ANDROID_HOME=%ANDROID_HOME%
echo [SmartWardrobe] Gradle 8.9 and JDK 17 are ready.
