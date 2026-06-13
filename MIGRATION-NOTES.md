# JDK 迁移说明 — 白云会议管理系统

## 背景

项目 `pom.xml` 原配置 `source/target 1.7`（JDK 7），但 Spring 5.0.0.RELEASE 官方要求 JDK 8 最低版本。在新 CI 机器（JDK 8+）上编译时频繁失败。

## 升级方案

### 默认：JDK 1.8（推荐）

**兼容性评估：**

| 依赖 | 版本 | JDK 8 兼容性 |
|------|------|-------------|
| Spring Framework | 5.0.0.RELEASE | **官方支持**（baseline 即 JDK 8） |
| MyBatis-Plus | 2.3 | 兼容（构建于 JDK 7/8） |
| Druid | 1.1.0 | 兼容 |
| MySQL Connector | 5.1.38 | 兼容 |
| mssql-jdbc | 6.2.0.jre8 | 兼容（JRE 8 专用版） |
| FastJson | 1.2.8 | 兼容 |
| Jackson | 2.10.1 | 兼容 |
| Hutool | 4.0.12 | 兼容 |
| Apache POI | 3.11 / 3.9 | 兼容 |
| Baidu AI SDK | 4.4.1 | 兼容 |
| commons-fileupload | 1.3.1 | 兼容 |
| AspectJWeaver | 1.8.8 | 兼容 |

**结论：所有依赖均兼容 JDK 8，无需修改 Java 源代码。**

### 可选：JDK 11

激活 `jdk11` profile 即可编译：

```bash
mvn clean package -Pjdk11
```

**额外依赖（已在 pom.xml jdk11 profile 中配置）：**
- `javax.xml.bind:jaxb-api:2.3.1` — JDK 11 移除了 JAXB 模块
- `org.glassfish.jaxb:jaxb-runtime:2.3.1` — JAXB 运行时
- `javax.annotation:javax.annotation-api:1.3.2` — JDK 11 移除了 `@PostConstruct` 等注解

**注意事项：**
- Spring 5.0 的 CGLIB 3.2.x 在 JDK 11 模块系统下可能有警告（`--add-opens`），不影响功能
- 如需完全消除警告，建议后续升级到 Spring 5.1+ 或 5.3.x
- MyBatis-Plus 2.3 未在 JDK 11 上官方测试，但实测可用

## 构建命令参考

```bash
# JDK 8 构建（默认）
mvn clean package

# JDK 8 + SQL Server
mvn clean package -Psqlserver

# JDK 11 构建
mvn clean package -Pjdk11

# 跳过前端构建
mvn clean package -Dskip.frontend=true

# 完整构建（含前端）
mvn clean package
# Maven 会自动执行 Node 安装 → npm install → npm run build
```

## 推荐的安全升级（可选，非必须）

以下依赖有已知安全漏洞，建议后续升级：

| 依赖 | 当前版本 | 建议版本 | 原因 |
|------|----------|----------|------|
| fastjson | 1.2.8 | **1.2.83** | 反序列化 CVE，API 兼容 |
| mysql-connector-java | 5.1.38 | 5.1.49 | 5.1.x 最后版本，bug 修复 |
| druid | 1.1.0 | 1.2.21 | 安全修复 |
| commons-fileupload | 1.3.1 | 1.5 | CVE-2023-24998 |
| jackson-databind | 2.10.1 | 2.13.5 | 反序列化 CVE |
| log4j 1.x | 1.2.17 | 迁移至 log4j2 或 logback | log4j 1.x 已 EOL |

## 回滚方式

如 JDK 8 出现不可预见问题，可临时回退：

```xml
<!-- pom.xml properties -->
<java.version>1.7</java.version>
```

> **注意**：回退到 1.7 仅适用于旧 CI 环境。Spring 5.0 在 JDK 7 上存在字节码兼容风险，不推荐长期使用。

## 其他改动说明

| 改动 | 原因 |
|------|------|
| `sqljdbc4` scope `4.0` → `runtime` | 原配置非法（scope 值错误），修正为标准 Maven scope |
| `tomcat-embed-core` 添加 `provided` scope | 避免 Tomcat JAR 打包进 WAR 与外部 Tomcat 冲突 |
| `maven-compiler-plugin` 指定版本 3.11.0 | 避免使用 Maven 默认的过旧插件版本 |
