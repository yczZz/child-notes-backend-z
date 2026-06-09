# 部署与运维

## 多环境配置

项目通过 Maven Profile 管理四个环境：

| Profile | 用途 | 配置文件 | 数据库地址 |
|---------|------|----------|-----------|
| `local` | 本地开发 | `application-local.yaml` | `127.0.0.1:3306/child_notes` |
| `test` | 测试环境 | `application-test.yaml` | `127.0.0.1:3306/child_notes_test` |
| `uat` | 预发布环境 | `application-uat.yaml` | 通过环境变量注入 |
| `prod` | 生产环境 | 无独立配置文件 | 通过环境变量注入 |

> 活跃 Profile 通过 `application.yml` 的 `spring.profiles.active: @activeProfile@` 由 Maven 替换。

## 环境变量

所有敏感配置均支持环境变量覆盖，以下是完整列表：

### 数据库

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `MYSQL_URL` | 各环境不同 | JDBC 连接 URL |
| `MYSQL_USER` | `root` | 数据库用户名 |
| `MYSQL_PWD` | 空 | 数据库密码 |

### 微信小程序

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `WECHAT_APPID` | 空 | 小程序 AppID |
| `WECHAT_SECRET` | 空 | 小程序密钥 |

> 未设置时进入开发模式，使用 `dev_{code}` 作为 openid。

### JWT

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `JWT_SECRET` | 示例占位值 | JWT 签名密钥，生产环境必须替换 |
| `JWT_EXPIRE_DAYS` | `30` | Token 有效天数 |

### OSS

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `OSS_ENDPOINT` | 空 | OSS Endpoint |
| `OSS_ACCESS_KEY_ID` | 空 | AccessKey ID |
| `OSS_ACCESS_KEY_SECRET` | 空 | AccessKey Secret |
| `OSS_BUCKET_NAME` | 空 | Bucket 名称 |
| `OSS_BASE_URL` | 空 | 访问域名 |

---

## 本地运行

### 前置条件

- JDK 17
- Maven 3.6+
- MySQL 8.x

### 启动

```bash
mvn spring-boot:run -Plocal
```

服务启动在 `http://localhost:8080`。

### 指定环境变量

```bash
# Linux/Mac
MYSQL_URL=jdbc:mysql://localhost:3306/child_notes \
MYSQL_USER=root \
MYSQL_PWD=123456 \
mvn spring-boot:run -Plocal

# Windows PowerShell
$env:MYSQL_URL="jdbc:mysql://localhost:3306/child_notes"
$env:MYSQL_USER="root"
$env:MYSQL_PWD="123456"
mvn spring-boot:run -Plocal
```

---

## Docker 部署

### Dockerfile 解析

项目采用多阶段构建：

**阶段一：构建**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
ARG BUILD_ENV=local
RUN mvn clean package -P${BUILD_ENV} -DskipTests
```

**阶段二：运行**

```dockerfile
FROM eclipse-temurin:17-jre
# 设置时区为 Asia/Shanghai
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 构建与运行

```bash
# 构建镜像（指定环境）
docker build --build-arg BUILD_ENV=local -t child-notes-backend:latest .

# 运行容器
docker run -d \
  -p 8080:8080 \
  -e MYSQL_URL=jdbc:mysql://mysql8:3306/child_notes \
  -e MYSQL_USER=root \
  -e MYSQL_PWD=root \
  -e JWT_SECRET=your-strong-secret \
  -e WECHAT_APPID=your-appid \
  -e WECHAT_SECRET=your-secret \
  -e OSS_ACCESS_KEY_ID=your-key \
  -e OSS_ACCESS_KEY_SECRET=your-secret \
  --name child-notes \
  child-notes-backend:latest
```

### Kubernetes 部署

典型 Deployment 配置要点：

- 通过 ConfigMap / Secret 注入环境变量
- 配置 `livenessProbe` / `readinessProbe`（可基于 `/api/auth/me` 做健康检查，或添加自定义端点）
- 设置时区 `TZ=Asia/Shanghai`

---

## 测试

测试使用独立数据库 `child_notes_test`：

```text
jdbc:mysql://127.0.0.1:7904/child_notes_test
```

可通过环境变量覆盖：

| 环境变量 | 说明 |
|----------|------|
| `MYSQL_TEST_URL` | 测试数据库 URL |
| `MYSQL_TEST_USER` | 测试数据库用户名 |
| `MYSQL_TEST_PWD` | 测试数据库密码 |

运行测试：

```bash
mvn -Ptest test
```

---

## 日志管理

日志配置位于 `logback-spring.xml`。

### 日志输出

| 输出目标 | 路径 | 说明 |
|----------|------|------|
| 控制台 | stdout | 彩色格式，开发调试用 |
| 全量日志 | `./logs/child-notes.log` | 所有级别日志 |
| 错误日志 | `./logs/child-notes-error.log` | 仅 ERROR 级别 |

### 日志滚动

| 参数 | 值 |
|------|-----|
| 滚动策略 | 按天 + 按大小（50MB） |
| 文件名模式 | `child-notes.YYYY-MM-DD.N.gz` |
| 最大保留天数 | 30 天 |
| 启动时清理 | 是（`cleanHistoryOnStart`） |

### 日志级别

| Logger | 级别 |
|--------|------|
| `com.ycz.childnotesbackend` | DEBUG |
| `org.springframework.web` | INFO |
| `org.mybatis` | INFO |
| `com.baomidou.mybatisplus` | INFO |
| `com.aliyun.oss` | WARN |
| Root | INFO |

### 请求日志

`RequestLoggingFilter` 记录每个 API 请求的：

- HTTP 方法
- 请求 URI + Query String
- 请求体内容

格式：`[api] {METHOD} {URL} {BODY}`

---

## 数据库初始化

每次应用启动时自动执行 `schema.sql`（`spring.sql.init.mode=always`），执行幂等迁移：

1. 创建缺失的表
2. 添加缺失的列
3. 创建缺失的索引
4. 回填旧数据的 `user_id`

> 由于 `schema.sql` 每次启动都执行，修改表结构时需使用条件判断确保幂等性。
