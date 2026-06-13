# Tomcat 部署指南 — 白云会议管理系统

## 前置条件

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | **1.8** (推荐) / 11 | `java -version` 确认 |
| Tomcat | 8.5.x / 9.0.x | Servlet 3.0+ |
| MySQL | 5.7+ | 默认数据库 |
| Nginx | 1.18+ | 可选，生产反代 |

## 1. 构建 WAR

```bash
# 进入项目目录
cd ssmu8xr0

# MySQL 版本（默认）
mvn clean package -Pmysql

# SQL Server 版本
mvn clean package -Psqlserver

# 跳过前端构建（已手动 npm run build 或 dist/ 无变化时）
mvn clean package -Pmysql -Dskip.frontend=true

# JDK 11 编译
mvn clean package -Pmysql -Pjdk11
```

构建产出: `target/ssmu8xr0.war`

## 2. WAR 部署步骤

```bash
# 1. 停止 Tomcat
$CATALINA_HOME/bin/shutdown.sh

# 2. 清理旧部署
rm -rf $CATALINA_HOME/webapps/ssmu8xr0
rm -f  $CATALINA_HOME/webapps/ssmu8xr0.war

# 3. 复制新 WAR
cp target/ssmu8xr0.war $CATALINA_HOME/webapps/

# 4. 启动 Tomcat
$CATALINA_HOME/bin/startup.sh

# 5. 验证
curl -s http://localhost:8080/ssmu8xr0/front/index.html | head -5
```

## 3. Context Path 配置

### 默认（WAR 文件名决定）
WAR 文件名 `ssmu8xr0.war` → context path 自动为 `/ssmu8xr0`。

### 自定义 Context Path
创建 `$CATALINA_HOME/conf/Catalina/localhost/meeting.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context docBase="/opt/apps/ssmu8xr0.war"
         path="/meeting"
         reloadable="false"
         unpackWAR="true">
</Context>
```

访问地址变为 `http://server/meeting/`。

### 部署为根路径 (/)
将 WAR 重命名为 `ROOT.war`：
```bash
cp ssmu8xr0.war $CATALINA_HOME/webapps/ROOT.war
```
> **注意**: 根路径部署后，前端代码中的 contextPath 计算会自动适配为空字符串。

## 4. 运行时数据库配置

### 方式一：JVM 系统属性（推荐）

编辑 `$CATALINA_HOME/bin/setenv.sh`：

```bash
#!/bin/bash
export CATALINA_OPTS="$CATALINA_OPTS \
  -Djdbc_url=jdbc:mysql://db-server:3306/ssmu8xr0?useUnicode=true&characterEncoding=UTF-8&tinyInt1isBit=false \
  -Djdbc_username=app_user \
  -Djdbc_password=secure_password \
  -Xms256m -Xmx512m"
```

Windows 环境编辑 `setenv.bat`：
```bat
set CATALINA_OPTS=%CATALINA_OPTS% -Djdbc_url=jdbc:mysql://db-server:3306/ssmu8xr0?useUnicode=true^&characterEncoding=UTF-8^&tinyInt1isBit=false -Djdbc_username=app_user -Djdbc_password=secure_password
```

### 方式二：环境变量

Spring `system-properties-mode="OVERRIDE"` 同时支持环境变量：

```bash
export jdbc_url="jdbc:mysql://db-server:3306/ssmu8xr0"
export jdbc_username="app_user"
export jdbc_password="secure_password"
```

### 方式三：JNDI 数据源（高级）

在 `$CATALINA_HOME/conf/context.xml` 中添加 Resource：

```xml
<Resource name="jdbc/ssmu8xr0"
          auth="Container"
          type="javax.sql.DataSource"
          factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
          driverClassName="com.mysql.jdbc.Driver"
          url="jdbc:mysql://db-server:3306/ssmu8xr0?useUnicode=true&amp;characterEncoding=UTF-8"
          username="app_user"
          password="secure_password"
          maxActive="20"
          maxIdle="10"
          maxWait="60000"
          validationQuery="SELECT 1"/>
```

在 `spring-mybatis.xml` 中替换 Druid DataSource 为 JNDI 查找：

```xml
<!-- 替换原有的 DruidDataSource bean -->
<jee:jndi-lookup id="dataSource"
                 jndi-name="java:comp/env/jdbc/ssmu8xr0"
                 expected-type="javax.sql.DataSource"/>
```

需在 `spring.xml` 中添加 `xmlns:jee` 命名空间：
```xml
xmlns:jee="http://www.springframework.org/schema/jee"
http://www.springframework.org/schema/jee http://www.springframework.org/schema/jee/spring-jee.xsd
```

## 5. Nginx 反向代理

```nginx
server {
    listen       80;
    server_name  meeting.example.com;

    # 文件上传大小限制（匹配 Spring multipart maxUploadSize=300MB）
    client_max_body_size 300m;

    # === Vue Admin 静态资源（文件名含 hash，长期缓存）===
    location /ssmu8xr0/admin/dist/js/ {
        proxy_pass http://127.0.0.1:8080;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    location /ssmu8xr0/admin/dist/css/ {
        proxy_pass http://127.0.0.1:8080;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    location /ssmu8xr0/admin/dist/fonts/ {
        proxy_pass http://127.0.0.1:8080;
        expires 1y;
    }
    location /ssmu8xr0/admin/dist/img/ {
        proxy_pass http://127.0.0.1:8080;
        expires 30d;
    }

    # === Layui 前端静态资源 ===
    location /ssmu8xr0/front/layui/ {
        proxy_pass http://127.0.0.1:8080;
        expires 7d;
    }
    location /ssmu8xr0/front/css/ {
        proxy_pass http://127.0.0.1:8080;
        expires 7d;
    }
    location /ssmu8xr0/front/xznstatic/ {
        proxy_pass http://127.0.0.1:8080;
        expires 7d;
    }

    # === 上传文件 ===
    location /ssmu8xr0/upload/ {
        proxy_pass http://127.0.0.1:8080;
        expires 1d;
    }

    # === API + 其他请求 ===
    location /ssmu8xr0/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 不加缓存（API 请求）
        proxy_no_cache 1;
        proxy_cache off;
    }
}
```

### 直接代理静态文件（性能更优）

如果 Nginx 与 Tomcat 在同一台机器，静态资源可直接由 Nginx 服务：

```nginx
# Vue Admin 静态资源 — Nginx 直接服务
location /ssmu8xr0/admin/dist/ {
    alias /opt/tomcat/webapps/ssmu8xr0/admin/dist/;
    expires 1y;
    add_header Cache-Control "public, immutable";
}

# Layui 前端
location /ssmu8xr0/front/ {
    alias /opt/tomcat/webapps/ssmu8xr0/front/;
    expires 7d;
}
```

## 6. 常见问题排查

### 404 Not Found
- 检查 WAR 是否成功解压：`ls $CATALINA_HOME/webapps/ssmu8xr0/`
- 检查 context path：`curl http://localhost:8080/ssmu8xr0/front/index.html`
- 检查 Tomcat 日志：`tail -f $CATALINA_HOME/logs/catalina.out`

### 数据库连接失败
- 检查 `setenv.sh` 中的 JVM 参数是否生效
- 确认数据库允许 Tomcat 服务器 IP 连接
- 检查 Druid 日志：`$CATALINA_HOME/logs/` 下的应用日志

### 文件上传 413 (Nginx)
- 确认 `client_max_body_size 300m` 已配置在 Nginx `server` 或 `location` 块
- Nginx 默认限制 1MB

### 前端页面空白 / API 404
- 浏览器 F12 → Network 面板，检查 API 请求 URL 是否正确
- 确认 `base.js` 动态计算的 contextPath 与实际部署路径一致
- 确认 `http.js` 中 `baseURL: '/ssmu8xr0'` 与实际 context path 匹配

### JDK 11 特定问题
- `ClassNotFoundException: javax.xml.bind.JAXBException`
  → 确认激活了 `-Pjdk11` profile 或手动添加 `jaxb-api` + `jaxb-runtime` 依赖
- `java.lang.NoClassDefFoundError: javax/annotation/PostConstruct`
  → 添加 `javax.annotation-api` 依赖

### Tomcat 端口冲突
```bash
# 查找占用 8080 端口的进程
lsof -i :8080
# 或修改 Tomcat 端口: conf/server.xml 中的 <Connector port="8080"
```
