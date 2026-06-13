# SSM 部署与接口前缀说明

> **版本**: 1.0 | **日期**: 2026-06-13 | **适用范围**: 系统集成方、运维部署人员
> **对应配置**: `web.xml` / `config.properties` / `admin/src/utils/http.js`

---

## 1. 项目技术栈与打包方式

| 项 | 值 |
|---|---|
| 框架 | Spring 4 + SpringMVC + MyBatis-Plus (SSM) |
| 构建工具 | Maven (`pom.xml`) |
| 打包格式 | WAR |
| 运行容器 | Tomcat (建议 8.5+) |
| 数据库 | MySQL 5.7+ |
| 连接池 | Druid |
| 前端框架 | Vue 2 + Element UI (admin 端) / Layui (前台端) |

### 1.1 Maven 打包命令

```bash
cd ssmu8xr0
mvn clean package -DskipTests
```

产物路径: `target/ssmu8xr0.war`

---

## 2. WAR Context Path: `/ssmu8xr0`

### 2.1 Context Path 的决定方式

SSM 项目的 Context Path **不是代码中硬编码的**，而是由 Tomcat 部署时决定。有以下几种情况：

| 部署方式 | Context Path | 说明 |
|---|---|---|
| WAR 文件名为 `ssmu8xr0.war` | `/ssmu8xr0` | Tomcat 默认以 WAR 文件名作为 context path |
| 部署到 `webapps/ROOT/` | `/` (根路径) | WAR 重命名为 `ROOT.war` 或解压到 `webapps/ROOT/` |
| 在 `server.xml` 中显式配置 | 自定义 | `<Context path="/meeting" docBase="..." />` |
| 在 `conf/Catalina/localhost/` 下创建 XML | 自定义 | 文件名为 context path |

**当前默认约定**: 项目名为 `ssmu8xr0`，WAR 文件名保持为 `ssmu8xr0.war`，因此 context path 为 `/ssmu8xr0`。

### 2.2 为什么所有接口都带 `/ssmu8xr0` 前缀

```
浏览器请求: http://localhost:8080/ssmu8xr0/huiyishiyuyue/page
                           └──────┘ └────────────────────┘
                           Context    Controller 路径
                           Path       (@RequestMapping)
```

Tomcat 的路由机制：
1. 收到请求 `/ssmu8xr0/huiyishiyuyue/page`
2. 匹配 Context Path `/ssmu8xr0` → 路由到本 WAR 应用
3. 剩余路径 `/huiyishiyuyue/page` → 交给 SpringMVC DispatcherServlet
4. DispatcherServlet 匹配 `@RequestMapping("/huiyishiyuyue")` + `@RequestMapping("/page")`

**关键**: 如果你将 WAR 部署到根路径 (`/`)，所有接口的前缀会消失，变为 `http://host/huiyishiyuyue/page`。

---

## 3. Admin 前端 `http.js` baseURL 配置

### 3.1 当前配置

**文件**: `ssmu8xr0/src/main/webapp/admin/src/utils/http.js`

```javascript
const http = axios.create({
    timeout: 1000 * 86400,          // 超时：24小时（极长）
    withCredentials: true,           // 跨域携带 Cookie
    baseURL: '/ssmu8xr0',            // ← 接口前缀
    headers: {
        'Content-Type': 'application/json; charset=utf-8'
    }
})
```

### 3.2 baseURL 与 Context Path 的关系

```
┌───────────────────────────────────────────────────────────────┐
│  admin 前端 (浏览器)                                           │
│                                                               │
│  axios 请求:                                                  │
│  http.get('/huiyishiyuyue/page')                              │
│                                                               │
│  ↓ axios 自动拼接 baseURL                                     │
│                                                               │
│  实际请求: GET /ssmu8xr0/huiyishiyuyue/page                   │
│                    └──────┘                                   │
│                    baseURL                                    │
└───────────────────────────────────────────────────────────────┘
                           │
                           ↓
┌───────────────────────────────────────────────────────────────┐
│  Tomcat                                                       │
│                                                               │
│  Context Path: /ssmu8xr0 → 匹配本应用                         │
│  剩余路径: /huiyishiyuyue/page → SpringMVC 路由               │
└───────────────────────────────────────────────────────────────┘
```

### 3.3 如果修改 Context Path，必须同步修改 baseURL

| 场景 | Tomcat Context Path | http.js baseURL | 是否匹配 |
|---|---|---|---|
| 默认 | `/ssmu8xr0` | `'/ssmu8xr0'` | ✅ |
| 部署到根 | `/` | `'/ssmu8xr0'` | ❌ 404 |
| 自定义 | `/meeting` | `'/ssmu8xr0'` | ❌ 404 |
| 自定义+同步改 | `/meeting` | `'/meeting'` | ✅ |

### 3.4 前台端 (Layui) 的 http.js 对比

**文件**: `ssmu8xr0/src/main/webapp/front/modules/http/http.js`

```javascript
baseurl = "http://localhost:8080/ssmu8xr0/"
```

| 对比项 | Admin 端 | 前台端 (Layui) |
|---|---|---|
| 框架 | Axios (Vue) | jQuery AJAX |
| baseURL 写法 | 相对路径 `/ssmu8xr0` | 绝对路径 `http://localhost:8080/ssmu8xr0/` |
| 跨域适配 | 相对路径，同源无问题 | **硬编码 localhost:8080**，部署到其他域名/IP 会跨域失败 |
| 认证方式 | Token header | Token header |
| Cookie | `withCredentials: true` | 未设置 |

**⚠ 部署注意**: 前台端 `http.js` 硬编码了 `localhost:8080`，部署到生产环境（不同 IP/域名/端口）时 **必须手动修改此值**，否则所有 API 请求会发送到 localhost 导致跨域失败。

---

## 4. 认证机制

### 4.1 Token 拦截器

**配置**: `spring-mvc.xml` 中注册了 `AuthorizationInterceptor`

```
拦截路径: /**
排除路径: /upload/**
```

### 4.2 认证流程

```
请求 → AuthorizationInterceptor
         │
         ├─ 检查 Header "Token" 或 Cookie "Token"
         │
         ├─ 无 Token 且方法有 @IgnoreAuth → 放行
         ├─ 无 Token 且方法无 @IgnoreAuth → 401
         ├─ 有 Token → 查库验证 → 有效则放行，无效则 401
         │
         └─ 401 响应 → 前端拦截 → 重定向到登录页
```

### 4.3 `@IgnoreAuth` 接口（无需登录）

| 接口 | 说明 |
|---|---|
| `GET /huiyishiyuyue/list` | 前台预约列表 |
| `GET /huiyishiyuyue/detail/{id}` | 前台预约详情 |
| `GET /huiyishi/list` | 前台会议室列表 |
| `GET /huiyishi/detail/{id}` | 前台会议室详情 |
| 各模块的 `/list` 和 `/detail` | 前台展示类接口 |

---

## 5. 集成方 curl 示例

### 5.1 登录获取 Token

```bash
# 管理员登录
curl -X POST 'http://localhost:8080/ssmu8xr0/users/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "123456"
  }'

# 响应示例（提取 token 字段）:
# {"code":0,"msg":"success","token":"abc123..."}
```

### 5.2 查询预约列表

```bash
# 后台分页查询（需要 Token）
curl -X GET 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/page?page=1&limit=10' \
  -H 'Token: abc123...'

# 按会议室名称搜索
curl -X GET 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/page?page=1&limit=10&huiyishimingcheng=三楼大会议室' \
  -H 'Token: abc123...'

# 按审核状态筛选
curl -X GET 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/page?page=1&limit=10&sfsh=待审核' \
  -H 'Token: abc123...'
```

### 5.3 前台查询（无需 Token）

```bash
# 前台预约列表（@IgnoreAuth）
curl -X GET 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/list?page=1&limit=10'

# 前台预约详情（@IgnoreAuth）
curl -X GET 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/detail/1234567890'
```

### 5.4 创建预约

```bash
curl -X POST 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/save' \
  -H 'Content-Type: application/json' \
  -H 'Token: abc123...' \
  -d '{
    "yuyuebianhao": "YH20260613001",
    "huiyishibianhao": "HS001",
    "huiyishimingcheng": "三楼大会议室",
    "huiyishiguimo": "大型",
    "huiyishiweizhi": "行政楼三楼",
    "yujirenshu": "30",
    "kaishishijian": "2026-06-15 09:00:00",
    "jieshushijian": "2026-06-15 11:00:00",
    "huiyishuoming": "季度经营分析会",
    "yonghuzhanghao": "zhangsan",
    "yonghuxingming": "张三",
    "shoujihaoma": "13800138000",
    "bumen": "行政部",
    "shebeibianhao": "1,3"
  }'

# 注意: sfsh 和 shenqingshijian 由后端自动设置，无需传入
```

### 5.5 审核预约

```bash
# 审核通过（自动创建使用记录）
curl -X POST 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/shenhe' \
  -H 'Content-Type: application/json' \
  -H 'Token: abc123...' \
  -d '{
    "id": 1234567890,
    "sfsh": "通过",
    "shhf": "同意，请准时使用"
  }'

# 审核驳回
curl -X POST 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/shenhe' \
  -H 'Content-Type: application/json' \
  -H 'Token: abc123...' \
  -d '{
    "id": 1234567890,
    "sfsh": "驳回",
    "shhf": "该时段已有其他安排，请更换时间"
  }'
```

### 5.6 修改预约

```bash
curl -X POST 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/update' \
  -H 'Content-Type: application/json' \
  -H 'Token: abc123...' \
  -d '{
    "id": 1234567890,
    "kaishishijian": "2026-06-15 14:00:00",
    "jieshushijian": "2026-06-15 16:00:00"
  }'

# ⚠ 注意: 通过 /update 修改 sfsh 不会触发使用记录自动创建
# 审核必须使用 /shenhe 接口
```

### 5.7 删除预约

```bash
# 单条删除
curl -X POST 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/delete' \
  -H 'Content-Type: application/json' \
  -H 'Token: abc123...' \
  -d '[1234567890]'

# 批量删除
curl -X POST 'http://localhost:8080/ssmu8xr0/huiyishiyuyue/delete' \
  -H 'Content-Type: application/json' \
  -H 'Token: abc123...' \
  -d '[1234567890, 1234567891, 1234567892]'
```

### 5.8 查询会议室列表

```bash
curl -X GET 'http://localhost:8080/ssmu8xr0/huiyishi/page?page=1&limit=10' \
  -H 'Token: abc123...'
```

### 5.9 查询设备信息

```bash
curl -X GET 'http://localhost:8080/ssmu8xr0/shebeixinxi/page?page=1&limit=10' \
  -H 'Token: abc123...'
```

### 5.10 查询使用记录

```bash
curl -X GET 'http://localhost:8080/ssmu8xr0/shiyongjilu/page?page=1&limit=10' \
  -H 'Token: abc123...'
```

---

## 6. 完整请求链路图

```
┌─────────────────────────────────────────────────────────────────────┐
│  浏览器 / curl / 集成方                                              │
│                                                                     │
│  请求: http://host:8080/ssmu8xr0/huiyishiyuyue/page?page=1          │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│  Tomcat                                                             │
│                                                                     │
│  1. 匹配 Context Path: /ssmu8xr0                                    │
│  2. 路由到 WAR 应用                                                  │
│  3. 剩余路径: /huiyishiyuyue/page                                   │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│  SpringMVC DispatcherServlet                                        │
│                                                                     │
│  1. CharacterEncodingFilter: UTF-8                                  │
│  2. AuthorizationInterceptor: 校验 Token                             │
│  3. HandlerMapping: /huiyishiyuyue → HuiyishiyuyueController        │
│  4. /page → page() 方法                                             │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│  HuiyishiyuyueController.page()                                     │
│                                                                     │
│  1. 解析分页参数 (page, limit)                                       │
│  2. 解析搜索条件 (huiyishimingcheng, sfsh, ...)                      │
│  3. 调用 HuiyishiyuyueService                                       │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│  MyBatis-Plus                                                       │
│                                                                     │
│  1. SqlSessionFactory → SqlSession                                  │
│  2. HuiyishiyuyueDao.xml → selectListView                           │
│  3. Druid 连接池 → MySQL                                            │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│  MySQL (数据库: ssmu8xr0)                                            │
│                                                                     │
│  SELECT * FROM huiyishiyuyue                                        │
│  WHERE huiyishimingcheng LIKE '%三楼%'                               │
│  LIMIT 0, 10                                                        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 7. 常见部署问题

### 7.1 Context Path 不匹配

**现象**: 前端所有接口 404

**原因**: Tomcat 部署的 Context Path 与 `http.js` 的 `baseURL` 不一致

**解决**:
```bash
# 方案 A: 修改 WAR 文件名与 baseURL 一致
cp target/ssmu8xr0.war $CATALINA_HOME/webapps/ssmu8xr0.war

# 方案 B: 修改 baseURL 与实际 Context Path 一致
# 编辑 admin/src/utils/http.js → baseURL: '/实际路径'
```

### 7.2 前台端跨域失败

**现象**: 前台页面 (Layui) 所有请求 CORS 错误

**原因**: `front/modules/http/http.js` 硬编码 `http://localhost:8080/ssmu8xr0/`

**解决**: 修改为实际部署地址，如 `http://192.168.1.100:8080/ssmu8xr0/`

### 7.3 数据库连接失败

**配置文件**: `src/main/resources/config.properties`

```properties
jdbc_url=jdbc:mysql://127.0.0.1:3306/ssmu8xr0?useUnicode=true&characterEncoding=UTF-8&tinyInt1isBit=false
jdbc_username=root
jdbc_password=123456
```

支持环境变量覆盖:
```bash
export DB_URL=jdbc:mysql://prod-db:3306/ssmu8xr0?useUnicode=true&characterEncoding=UTF-8
export DB_USERNAME=app_user
export DB_PASSWORD=secure_password
```

### 7.4 静态资源 404

`web.xml` 中配置了静态资源不经过 Servlet:
```xml
<servlet-mapping>
    <servlet-name>default</servlet-name>
    <url-pattern>*.js</url-pattern>
</servlet-mapping>
<!-- 同理 *.css, *.html, /upload/* -->
```

如果静态资源 404，检查 Tomcat 的 `conf/web.xml` 中 `default` Servlet 是否被注释。

---

## 8. 环境要求速查

| 项 | 最低版本 | 推荐版本 |
|---|---|---|
| JDK | 1.8 | 1.8 (项目基于 Java 8 编写) |
| Tomcat | 8.0 | 8.5 / 9.0 |
| MySQL | 5.6 | 5.7 / 8.0 |
| Maven | 3.3 | 3.6+ |
| Node.js (前端构建) | 10 | 14 (如需重新构建 admin 前端) |
