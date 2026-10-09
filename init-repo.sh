#!/bin/sh
# init-repo.sh —— git init + 首提交 + 生成 gradle wrapper
# 用法：在本目录（5-纯自研工程方案/）下执行  ./init-repo.sh
# 说明：
#  - 沙箱/CI 里 git 可能不可用，本机执行此脚本即可
#  - gradle-wrapper.jar 不提交（二进制），本机 `gradle wrapper` 补齐
#  - 提交前请确认已填好真实内容源/密钥（见 10-上线合规Checklist）
set -e

cd "$(dirname "$0")"

echo "== 1. git init =="
git init 2>/dev/null || git init -b main

echo "== 2. 生成 gradle wrapper（如本机装了 gradle）=="
if command -v gradle >/dev/null; then
  gradle wrapper --gradle-version 8.10 2>/dev/null || echo "  (gradle wrapper 生成失败，不影响，可稍后手动)"
else
  echo "  本机无 gradle，跳过；Android Studio 导入时会自动补齐 wrapper jar"
fi

echo "== 3. git 身份（若未配置）=="
git config user.name  >/dev/null 2>&1 || git config user.name  "TVBoxRebuild"
git config user.email >/dev/null 2>&1 || git config user.email "dev@example.com"

echo "== 4. 首提交 =="
git add -A
git commit -m "chore: 纯自研 TVBox 类手机影音 App 工程骨架 (M0-M3)

- 多模块 Gradle（app/core-*/feature-*/libs:quickjs）
- M0 脚手架 + M1 源管理 + M2 UI/详情/播放/设置 + M3 安全加密
- QuickJS 真实 C 源码（Bellard quickjs-ng v0.17.0，MIT）
- 0 复用某商业闭源影音App so/资源/端点；内容源走自有/授权
- 合规红线见 10-上线合规Checklist.md"

echo "== 5. 远端（按需）=="
echo "  已本地提交。若要推远端：git remote add origin <url> && git push -u origin main"

echo "[完成] 当前 git log："
git --no-pager log --oneline | head
