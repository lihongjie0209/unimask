# GitHub Actions CI/CD 配置说明

## ✅ 已完成

1. **创建 GitHub Actions 工作流** (`.github/workflows/ci.yml`)
   - ✅ 所有分支提交时运行单元测试
   - ✅ Pull Request 时运行单元测试
   - ✅ v 开头的 tag 触发构建和发布

2. **更新 README.md**
   - ✅ 修正 GitHub 地址为 `lihongjie0209/unimask`
   - ✅ 所有链接已更新

## 📋 待配置

### 1. GitHub Secrets

需要在 GitHub 仓库设置中添加以下 Secrets（Settings → Secrets and variables → Actions）：

- `MAVEN_USERNAME`: Maven 仓库用户名
- `MAVEN_PASSWORD`: Maven 仓库密码/Token

### 2. Maven 仓库配置

**请提供以下信息：**

1. **仓库类型**（选择一个）：
   - Maven Central (Sonatype OSSRH)
   - GitHub Packages
   - 其他私有 Maven 仓库

2. **仓库地址**：
   - 示例（Maven Central）: `https://oss.sonatype.org/service/local/staging/deploy/maven2/`
   - 示例（GitHub Packages）: `https://maven.pkg.github.com/lihongjie0209/unimask`

3. **认证信息**：
   - 用户名
   - 密码/Token

提供信息后，我会更新 `pom.xml` 添加 `<distributionManagement>` 配置。

---

## 🔄 工作流说明

### Test Job (所有分支)
```yaml
触发条件: 任意分支 push 或 PR
步骤:
  1. Checkout 代码
  2. 设置 JDK 8
  3. 运行 Maven 测试
  4. 生成测试报告
```

### Publish Job (仅 v tag)
```yaml
触发条件: 推送 v* tag (例如 v1.0.0, v1.0.1)
依赖: Test job 成功
步骤:
  1. Checkout 代码
  2. 设置 JDK 8
  3. 从 tag 提取版本号
  4. 更新 pom.xml 版本
  5. 构建 JAR 包
  6. 配置 Maven 认证
  7. 发布到 Maven 仓库
  8. 创建 GitHub Release
```

---

## 🚀 发布流程

### 发布新版本：

```bash
# 1. 确保代码已提交
git add .
git commit -m "Release v1.0.0"

# 2. 创建并推送 tag
git tag v1.0.0
git push origin v1.0.0

# 3. GitHub Actions 自动执行：
#    - 运行测试
#    - 构建 JAR
#    - 发布到 Maven 仓库
#    - 创建 GitHub Release
```

### 查看发布状态：

访问：https://github.com/lihongjie0209/unimask/actions

---

## 📦 POM.xml 待添加配置

```xml
<distributionManagement>
    <repository>
        <id>ossrh</id>
        <name>Maven Central Repository</name>
        <url><!-- 需要提供 --></url>
    </repository>
    <snapshotRepository>
        <id>ossrh</id>
        <name>Maven Central Snapshot Repository</name>
        <url><!-- 需要提供 --></url>
    </snapshotRepository>
</distributionManagement>

<!-- 如果是 Maven Central，还需要 -->
<licenses>
    <license>
        <name>MIT License</name>
        <url>https://opensource.org/licenses/MIT</url>
    </license>
</licenses>

<scm>
    <connection>scm:git:git://github.com/lihongjie0209/unimask.git</connection>
    <developerConnection>scm:git:ssh://github.com:lihongjie0209/unimask.git</developerConnection>
    <url>https://github.com/lihongjie0209/unimask</url>
</scm>

<developers>
    <developer>
        <name>Li Hongjie</name>
        <!-- 其他信息 -->
    </developer>
</developers>
```

---

**准备好 Maven 仓库信息后，请告诉我，我会完成 pom.xml 的配置。**
