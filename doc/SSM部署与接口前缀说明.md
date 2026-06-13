# SSM 部署与接口前缀说明

> 版本: 1.0 | 基于源码分析 | 面向集成方与运维人员

---

## 1. WAR 包与 Context Path

### 1.1 构建产物

项目使用 Maven 构建，`pom.xml` 中的关键配置:

```xml
<packaging>war</packaging>
<build>
    <finalName>ssmu8xr0</finalName>
</build>
```

构建命令:
```bash
mvn clean package -P mysql
```

生成的 WAR 文件: `target/ssmu8xr0.war`

### 1.2 Context Path 机制

当 `ssmu8xr0.war` 部署到 Tomcat 的 `webapps/` 目录后，Tomcat 自动以 WAR 文件名(去除 `.war` 后缀)作为应用的 Context Path:

```
WAR 文件名:  ssmu8xr0.war
Context Path: /ssmu8xr0
完整根地址:   http://<host>:<port>/ssmu8xr0/
```

### 1.3 DispatcherServlet 映射

`web.xml` 中 Spring MVC 的 DispatcherServlet 映射到 `/`:

```xml
<servlet-mapping>
    <servlet-name>SpringMVC</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

因此完整的接口地址为:

```
http://<host>:<port>/ssmu8xr0/<controller-path>/<endpoint>
```

例如:
```
http://localhost:8080/ssmu8xr0/huiyishiyuyue/page
http://localhost:8080/ssmu8xr0/huiyishi/list
http://localhost:8080/ssmu8xr0/shiyongjilu/page
```

### 1.4 静态资源路径

`web.xml` 将以下扩展名和路径交由 Tomcat 默认 Servlet 直接处理(不经过 Spring MVC):

| 资源类型 | 路径示例 |
|---------|---------|
| `.js` | `/ssmu8xr0/admin/src/utils/http.js` |
| `.css` | `/ssmu8xr0/admin/css/style.css` |
| `.html` | `/ssmu8xr0/front/index.html` |
| 上传文件 | `/ssmu8xr0/upload/*` |

---

## 2. 前端 baseURL 配置

### 2.1 管理后台 (Vue.js + axios)

文件: `src/main/webapp/admin/src/utils/http.js`

```javascript
const http = axios.create({
    timeout: 1000 * 86400,
    baseURL: '/ssmu8xr0',
    headers: {
        'Content-Type': 'application/json; charset=utf-8'
    }
})
```

- **baseURL**: `'/ssmu8xr0'` (相对路径，依赖浏览器当前域名)
- **认证方式**: 请求头 `Token` 字段，值从 `localStorage` 读取
- **超时**: 86400 秒 (24 小时)
- 401 响应自动跳转 Vue Router 的 `login` 路由

### 2.2 前台页面 (Layui + jQuery)

文件: `src/main/webapp/front/modules/http/http.js`

```javascript
var baseurl = "http://localhost:8080/ssmu8xr0/";
```

- **baseurl**: 硬编码 `http://localhost:8080/ssmu8xr0/` (绝对路径)
- **认证方式**: 请求头 `Token` 字段，值从 `localStorage` 读取
- 401/403 响应跳转 `../login/login.html`

**注意**: 前台的 baseurl 硬编码了 `localhost:8080`，部署到非本地环境时必须修改此值，否则前台所有请求都会指向 localhost。

---

## 3. 认证机制

### 3.1 Token 获取

登录接口返回 Token，客户端存入 `localStorage`:

```bash
# 管理员登录
curl -X POST http://localhost:8080/ssmu8xr0/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# 响应示例
# {"code":0,"token":"eyJhbGciOi..."}
```

### 3.2 Token 使用

后续请求在请求头中携带 Token:

```
Token: <登录返回的 token 值>
```

### 3.3 免认证接口

标注 `@IgnoreAuth` 注解的接口不需要 Token，包括:
- `GET /<module>/list` -- 前端列表
- `GET /<module>/detail/{id}` -- 前端详情
- `GET /option/{tableName}/{columnName}` -- 下拉选项
- 等

---

## 4. 集成方 curl 示例

### 4.1 登录获取 Token

```bash
# 管理员登录
TOKEN=$(curl -s -X POST http://localhost:8080/ssmu8xr0/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "Token: $TOKEN"
```

### 4.2 会议室预约相关

```bash
# ========== 查询类 (分页列表) ==========

# 查询预约列表 (需认证)
curl -s http://localhost:8080/ssmu8xr0/huiyishiyuyue/page?page=1\&limit=10 \
  -H "Token: $TOKEN"

# 查询预约列表 (免认证 - 前端接口)
curl -s "http://localhost:8080/ssmu8xr0/huiyishiyuyue/list?page=1&limit=10"

# 按审核状态筛选
curl -s "http://localhost:8080/ssmu8xr0/huiyishiyuyue/page?page=1&limit=10&sfsh=待审核" \
  -H "Token: $TOKEN"

# 查询单条预约详情
curl -s http://localhost:8080/ssmu8xr0/huiyishiyuyue/info/1718438400123 \
  -H "Token: $TOKEN"

# ========== 创建预约 ==========

curl -s -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: $TOKEN" \
  -d '{
    "yuyuebianhao": "YY202606001",
    "huiyishibianhao": "H001",
    "huiyishimingcheng": "第一会议室",
    "huiyishiguimo": "大型",
    "huiyishiweizhi": "3楼301",
    "yujirenshu": "20",
    "kaishishijian": "2026-06-15 09:00:00",
    "jieshushijian": "2026-06-15 11:00:00",
    "huiyishuoming": "项目评审会",
    "yonghuzhanghao": "zhangsan",
    "yonghuxingming": "张三",
    "shoujihaoma": "13800138000",
    "bumen": "技术部",
    "shebeibianhao": "SB001,SB003"
  }'

# 响应 (成功): {"code":0,"msg":"success"}
# 响应 (冲突): {"code":500,"msg":"该会议室在所选时间段内已有预约（待审核或通过），存在时间冲突"}

# ========== 审核预约 ==========

# 通过
curl -s -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/shenhe \
  -H "Content-Type: application/json" \
  -H "Token: $TOKEN" \
  -d '{
    "id": 1718438400123,
    "sfsh": "通过",
    "shhf": "同意使用"
  }'

# 驳回
curl -s -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/shenhe \
  -H "Content-Type: application/json" \
  -H "Token: $TOKEN" \
  -d '{
    "id": 1718438400123,
    "sfsh": "驳回",
    "shhf": "时间冲突，请改期"
  }'

# ========== 修改预约 ==========

curl -s -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/update \
  -H "Content-Type: application/json" \
  -H "Token: $TOKEN" \
  -d '{
    "id": 1718438400123,
    "kaishishijian": "2026-06-15 14:00:00",
    "jieshushijian": "2026-06-15 16:00:00",
    "huiyishuoming": "改期后的项目评审会"
  }'

# ========== 删除预约 ==========

curl -s -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/delete \
  -H "Content-Type: application/json" \
  -H "Token: $TOKEN" \
  -d '[1718438400123]'
```

### 4.3 会议室管理

```bash
# 查询会议室列表 (免认证)
curl -s "http://localhost:8080/ssmu8xr0/huiyishi/list?page=1&limit=10"

# 查询会议室详情 (免认证)
curl -s http://localhost:8080/ssmu8xr0/huiyishi/detail/1718438400001
```

### 4.4 使用记录

```bash
# 查询使用记录列表
curl -s "http://localhost:8080/ssmu8xr0/shiyongjilu/page?page=1&limit=10" \
  -H "Token: $TOKEN"
```

### 4.5 设备与部门

```bash
# 查询设备列表 (免认证)
curl -s "http://localhost:8080/ssmu8xr0/shebeixinxi/list?page=1&limit=10"

# 查询部门列表 (免认证)
curl -s "http://localhost:8080/ssmu8xr0/bumen/list?page=1&limit=10"

# 获取下拉选项 (免认证，通用接口)
curl -s "http://localhost:8080/ssmu8xr0/option/huiyishi/huiyishimingcheng"
```

### 4.6 通用审核接口 (不推荐)

```bash
# 注意: 此接口绕过业务校验，不会自动创建使用记录
# 仅用于非预约表的简单审核场景
curl -s -X POST http://localhost:8080/ssmu8xr0/sh/huiyishiyuyue \
  -H "Content-Type: application/json" \
  -H "Token: $TOKEN" \
  -d '{"id": 1718438400123, "sfsh": "通过"}'
```

---

## 5. 接口路径速查表

### 5.1 会议室预约 `/huiyishiyuyue`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/page` | 需要 | 后端分页列表 (yonghu 角色仅看自己) |
| GET | `/list` | 免认证 | 前端分页列表 |
| GET | `/lists` | 需要 | 全量列表 |
| GET | `/query` | 需要 | 条件查询单条 |
| GET | `/info/{id}` | 需要 | 后端详情 |
| GET | `/detail/{id}` | 免认证 | 前端详情 |
| POST | `/save` | 需要 | 后端创建 (含冲突校验) |
| POST | `/add` | 需要 | 前端创建 (含冲突校验) |
| POST | `/update` | 需要 | 修改 (条件触发冲突校验) |
| POST | `/shenhe` | 需要 | 审核 (通过时自动创建使用记录) |
| POST | `/delete` | 需要 | 批量删除 |
| GET | `/remind/{col}/{type}` | 需要 | 提醒计数 |

### 5.2 会议室 `/huiyishi`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/page` | 需要 | 后端分页列表 |
| GET | `/list` | 免认证 | 前端分页列表 |
| GET | `/info/{id}` | 需要 | 后端详情 |
| GET | `/detail/{id}` | 免认证 | 前端详情 |
| POST | `/save` | 需要 | 创建 |
| POST | `/add` | 需要 | 前端创建 |
| POST | `/update` | 需要 | 修改 |
| POST | `/delete` | 需要 | 批量删除 |

### 5.3 使用记录 `/shiyongjilu`

与 5.2 结构相同，标准 CRUD。

### 5.4 设备信息 `/shebeixinxi`

与 5.2 结构相同，标准 CRUD。

### 5.5 部门 `/bumen`

与 5.2 结构相同，标准 CRUD。

### 5.6 通用接口 (CommonController)

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/option/{table}/{column}` | 免认证 | 获取字段去重值 (下拉选项) |
| GET | `/follow/{table}/{column}?columnValue=X` | 免认证 | 按字段值查单条记录 |
| POST | `/sh/{table}` | 需要 | 通用审核 (直接 UPDATE sfsh) |
| GET | `/remind/{table}/{column}/{type}` | 免认证 | 通用提醒计数 |
| GET | `/cal/{table}/{column}` | 免认证 | 列聚合 (sum/max/min/avg) |
| GET | `/group/{table}/{column}` | 免认证 | 分组统计 |
| GET | `/value/{table}/{xCol}/{yCol}` | 免认证 | 值统计 |

---

## 6. 部署环境变量

数据库连接支持环境变量覆盖 (`config.properties`):

| 环境变量 | 默认值 | 说明 |
|---------|-------|------|
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/ssmu8xr0` | JDBC 连接串 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `123456` | 数据库密码 |

部署到非本地环境时，在 Tomcat 启动脚本或系统环境中设置:

```bash
export DB_URL="jdbc:mysql://db.example.com:3306/ssmu8xr0?useSSL=true"
export DB_USERNAME="prod_user"
export DB_PASSWORD="secure_password"
```

---

## 7. 非本地部署注意事项

1. **前台 baseurl 必须修改**: `front/modules/http/http.js` 中的 `baseurl` 硬编码为 `http://localhost:8080/ssmu8xr0/`，生产环境需改为实际域名或使用相对路径
2. **管理后台 baseURL 无需修改**: `admin/src/utils/http.js` 使用相对路径 `'/ssmu8xr0'`，会自动适配当前域名
3. **Context Path 变更**: 若需修改 context path，需同步修改管理后台的 `baseURL` 和前台的 `baseurl`，并重新构建前端
4. **上传文件路径**: 文件上传存储在 WAR 包内的相对路径，重新部署会丢失已上传文件，生产环境应配置外部存储路径
