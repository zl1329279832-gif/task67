# JDK Migration Notes — 1.7 → 8 (with JDK 11 guidance)

## Why Upgrade

The project was configured with `<source>1.7</source><target>1.7</target>` in `maven-compiler-plugin`, but **Spring Framework 5.0 requires JDK 8 as a minimum**. The old setting caused compilation failures on modern CI machines that ship with JDK 11+. Additionally, `mssql-jdbc:6.2.0.jre8` already requires JDK 8 at runtime.

---

## Spring 5.0 Compatibility Matrix

| JDK Version | Spring 5.0.x | Notes |
|-------------|-------------|-------|
| 7           | **Not supported** | Spring 5.0 uses JDK 8 APIs (`java.time`, lambdas, default methods) |
| **8**       | **Supported** | Baseline — all dependencies tested and compatible |
| 9           | Supported   | Module system; no issues for WAR deployment |
| 10          | Supported   | Short-term release |
| **11 (LTS)**| **Supported** | Recommended LTS upgrade target |
| 12–17       | Supported   | Tested through Spring 5.3.x |

**Recommendation**: Use **JDK 8** for the immediate upgrade (zero risk). Plan a follow-up to JDK 11 when the team is ready.

---

## Dependency Compatibility (JDK 8)

All current dependencies are JDK 8 compatible — no version bumps required:

| Dependency          | Version       | JDK 8 | JDK 11 | Notes |
|---------------------|---------------|-------|--------|-------|
| Spring Framework    | 5.0.0.RELEASE | OK    | OK     |       |
| MyBatis-Plus        | 2.3           | OK    | OK     |       |
| Druid               | 1.1.0         | OK    | OK     |       |
| MySQL Connector     | 5.1.38        | OK    | OK     |       |
| mssql-jdbc          | 6.2.0.jre8    | OK    | OK     |       |
| FastJSON            | 1.2.8         | OK    | OK     |       |
| Jackson             | 2.10.1        | OK    | OK     |       |
| Hutool              | 4.0.12        | OK    | OK     |       |
| Apache POI          | 3.11 / 3.9    | OK    | OK     |       |
| Baidu AI SDK        | 4.4.1         | OK    | OK     |       |
| Tomcat Embed Core   | 9.0.29        | OK    | OK     |       |
| Log4j               | 1.2.17        | OK    | OK     | Consider migrating to Log4j2 separately |

---

## JDK 8 Upgrade — What Changed

1. **`pom.xml`**: `<source>` and `<target>` changed from `1.7` to `1.8` (via `${java.version}` property).
2. **No Java source changes required**: the codebase does not use any JDK 7-only APIs that were removed or changed in JDK 8.
3. **CI machines**: any JDK 8+ (Temurin/Adoptium, Oracle, Amazon Corretto) will work.

---

## JDK 11 Upgrade — Additional Steps

If upgrading to JDK 11 in the future:

### 1. JAXB Removal

JDK 11 removed `javax.xml.bind` (JAXB). If any code uses JAXB annotations or `JAXBContext`, add:

```xml
<dependency>
    <groupId>javax.xml.bind</groupId>
    <artifactId>jaxb-api</artifactId>
    <version>2.3.1</version>
</dependency>
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>2.3.3</version>
</dependency>
```

**Current status**: this codebase does **not** use JAXB directly — no action needed unless a transitive dependency requires it.

### 2. JavaFX Removal

JDK 11 removed JavaFX. This project does not use JavaFX — no action needed.

### 3. Property Change

In `pom.xml`, change the `java.version` property:

```xml
<java.version>11</java.version>
```

### 4. Tomcat Compatibility

Tomcat 9.0.x supports JDK 11. No Tomcat upgrade needed.

---

## Verification

After the upgrade, verify with:

```bash
# Check compiler target
mvn clean package -P mysql
jar -tf target/ssmu8xr0.war | head -5

# Verify class file version (JDK 8 = major version 52)
javap -verbose -classpath target/ssmu8xr0/WEB-INF/classes com.controller.CommonController | grep "major version"
# Expected output: major version: 52
```
