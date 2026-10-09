#!/bin/sh
#
# 由 `gradle wrapper` 生成；此处提供最小可用版本。
# 若缺失 gradle/wrapper/gradle-wrapper.jar，本地 `gradle wrapper` 可补齐（不提交二进制到仓库）。
#
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)

# 定位 java
JAVA="java"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
fi

# 解析 wrapper jar
CLASSPATH="gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$APP_HOME/$CLASSPATH" ]; then
  echo "缺少 gradle/wrapper/gradle-wrapper.jar。"
  echo "请在本机执行：gradle wrapper --gradle-version 8.10"
  echo "或用 Android Studio 导入本目录，它会自动补齐 wrapper。"
  exit 1
fi

exec "$JAVA" -classpath "$APP_HOME/$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
