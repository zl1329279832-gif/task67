# Tomcat Deployment Guide — ssmu8xr0 (白云会议管理系统)

## Prerequisites

| Component      | Minimum            | Recommended        |
|----------------|--------------------|--------------------|
| JDK            | 8                  | 11 (LTS)           |
| Tomcat         | 8.5.x              | 9.0.x              |
| MySQL          | 5.7                | 8.0                |
| SQL Server     | 2012 (optional)    | 2019               |
| Node.js        | 14.x (build only)  | 16.x               |

---

## 1. Build

```bash
# ── Step 1: Build Vue admin ──
cd ssmu8xr0/src/main/webapp/admin
npm ci
npm run build          # outputs to admin/dist/

# ── Step 2: Package WAR ──
cd ../../../../       # back to ssmu8xr0/
mvn clean package -P mysql      # or -P sqlserver
# produces: target/ssmu8xr0.war
```

---

## 2. Context Path

Drop `ssmu8xr0.war` into Tomcat's `webapps/` directory. Tomcat auto-deploys it at:

```
http://<host>:8080/ssmu8xr0/
```

To customize the context path or datasource, create `conf/Catalina/localhost/ssmu8xr0.xml`:

```xml
<Context path="/ssmu8xr0" docBase="/opt/tomcat/webapps/ssmu8xr0.war" reloadable="false">
    <!-- see JNDI DataSource section below -->
</Context>
```

---

## 3. Database Configuration

### Option A: Environment Variables (simple)

Set these before starting Tomcat (e.g. in `bin/setenv.sh`):

```bash
# setenv.sh
export DB_URL="jdbc:mysql://db-host:3306/ssmu8xr0?useUnicode=true&characterEncoding=UTF-8&tinyInt1isBit=false"
export DB_USERNAME="app_user"
export DB_PASSWORD="s3cret"
```

| Variable             | Default                               | Description        |
|----------------------|---------------------------------------|--------------------|
| `DB_URL`             | `jdbc:mysql://127.0.0.1:3306/ssmu8xr0...` | JDBC URL       |
| `DB_USERNAME`        | `root`                                | DB user            |
| `DB_PASSWORD`        | `123456`                              | DB password        |
| `DB_VALIDATION_QUERY`| `SELECT 1`                            | Pool health check  |

### Option B: JNDI DataSource (production recommended)

In `conf/context.xml` or `conf/Catalina/localhost/ssmu8xr0.xml`:

```xml
<!-- MySQL -->
<Resource name="jdbc/ssmu8xr0"
          auth="Container"
          type="javax.sql.DataSource"
          driverClassName="com.mysql.jdbc.Driver"
          url="jdbc:mysql://db-host:3306/ssmu8xr0?useUnicode=true&amp;characterEncoding=UTF-8"
          username="app_user"
          password="s3cret"
          maxTotal="20"
          maxIdle="10"
          maxWaitMillis="10000"
          validationQuery="SELECT 1" />
```

```xml
<!-- SQL Server -->
<Resource name="jdbc/ssmu8xr0"
          auth="Container"
          type="javax.sql.DataSource"
          driverClassName="com.microsoft.sqlserver.jdbc.SQLServerDriver"
          url="jdbc:sqlserver://db-host:1433;DatabaseName=ssmu8xr0"
          username="sa"
          password="s3cret"
          maxTotal="20"
          maxIdle="10"
          maxWaitMillis="10000"
          validationQuery="SELECT 1" />
```

Then in `spring-mybatis.xml`, uncomment the JNDI bean and comment out the Druid bean.

---

## 4. Nginx Reverse Proxy (`/ssmu8xr0`)

```nginx
upstream tomcat_backend {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name meeting.example.com;

    # ── API & dynamic pages ──
    location /ssmu8xr0/ {
        proxy_pass         http://tomcat_backend/ssmu8xr0/;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;

        # WebSocket (if needed later)
        proxy_http_version 1.1;
        proxy_set_header   Upgrade    $http_upgrade;
        proxy_set_header   Connection "upgrade";
    }

    # ── Static assets: Vue admin dist (long-term cache) ──
    location ~* ^/ssmu8xr0/admin/dist/.*\.(js|css|woff2?|ttf|eot|svg|png|jpg|gif|ico)$ {
        proxy_pass http://tomcat_backend;

        expires 30d;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    # ── Static assets: front-end pages ──
    location ~* ^/ssmu8xr0/front/.*\.(js|css|png|jpg|gif|ico|woff2?|ttf)$ {
        proxy_pass http://tomcat_backend;

        expires 7d;
        add_header Cache-Control "public";
        access_log off;
    }
}
```

### HTTPS (recommended)

Add a `server` block on port 443 with SSL certificates, then redirect port 80:

```nginx
server {
    listen 80;
    server_name meeting.example.com;
    return 301 https://$host$request_uri;
}
```

---

## 5. Switching Environments (staging / production)

When changing the domain or context path for a staging environment:

1. **Backend**: set `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` env vars — no code changes needed.
2. **Frontend (Vue admin)**: `base.js` now auto-detects the host from `window.location`, so no `base.js` edit is needed when switching domains.
3. **Nginx**: update `server_name` and upstream address in the config above.

---

## 6. Troubleshooting

| Symptom | Fix |
|---------|-----|
| 404 on `/ssmu8xr0/admin/dist/index.html` | Vue build missing — run `npm run build` before `mvn package` |
| DB connection refused | Check `DB_URL` env var or JNDI Resource; verify MySQL/SQLServer is reachable |
| WAR too large (>100 MB) | Ensure `node_modules/` is excluded — check maven-war-plugin `packagingExcludes` |
| Static assets not cached | Verify Nginx `location` blocks match the paths above |
