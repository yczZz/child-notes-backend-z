# 幼儿简记微信小程序后端服务 Child Notes Backend

宝宝成长记录后端服务，为微信小程序提供用户登录、宝宝档案、喂养/睡眠/尿布/体温/成长等记录管理、统计分析、积分任务、社区讨论、文件上传和 AI 分析能力。

## 功能特性

- 微信小程序登录：支持真实 `code2session`，未配置微信密钥时自动进入本地开发登录模式。
- 宝宝与家庭管理：宝宝档案、家庭成员、角色关系、当前宝宝切换。
- 日常记录：喂养、睡眠、尿布、体温、成长、疫苗、辅食、异常症状、里程碑等。
- 统计与积分：日/区间统计、签到、任务积分、抽奖活动。
- 文件上传：通过阿里云 OSS 存储图片和附件。
- AI 分析：基于最近 7 天记录生成宝宝喂养和成长分析报告。
- 管理后台：内置静态后台页面和 `/admin/api/**` 管理接口。

## 技术栈

| 组件 | 说明 |
| --- | --- |
| Java 17 | 运行环境 |
| Spring Boot 2.6.13 | Web 服务框架 |
| MyBatis-Plus 3.5.x | ORM 与数据访问 |
| MySQL 8.x | 业务数据库 |
| Druid | 数据库连接池 |
| Aliyun OSS SDK | 文件上传 |
| Agentscope Harness | AI Agent 调用封装 |
| Maven | 构建工具 |

## 快速开始

准备环境：

- JDK 17
- Maven 3.8+
- MySQL 8.x

创建数据库：

```sql
CREATE DATABASE child_notes DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

本地启动：

```bash
mvn spring-boot:run -Plocal
```

默认服务地址为 `http://localhost:8080`。首次启动会执行 `src/main/resources/schema.sql` 初始化或补齐表结构。

Windows PowerShell 示例：

```powershell
$env:MYSQL_URL="jdbc:mysql://127.0.0.1:3306/child_notes?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"
$env:MYSQL_USER="root"
$env:MYSQL_PWD="your-password"
$env:JWT_SECRET="replace-with-a-long-random-secret"
mvn spring-boot:run -Plocal
```

## 环境变量

开源版本不包含任何真实密钥，部署前需要通过环境变量注入：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP 端口 |
| `MYSQL_URL` | 本地 `child_notes` | 主数据库 JDBC 地址 |
| `MYSQL_USER` | `root` | 数据库用户名 |
| `MYSQL_PWD` | 空 | 数据库密码 |
| `WECHAT_APPID` | 空 | 微信小程序 AppID |
| `WECHAT_SECRET` | 空 | 微信小程序 AppSecret |
| `JWT_SECRET` | 示例占位值 | JWT 签名密钥，生产环境必须替换 |
| `JWT_EXPIRE_DAYS` | `30` | 用户 Token 有效天数 |
| `ADMIN_INIT_USERNAME` | `admin` | 首个后台管理员用户名 |
| `ADMIN_INIT_PASSWORD` | 空 | 首个后台管理员密码，留空则不自动创建管理员 |
| `ADMIN_INIT_DISPLAY_NAME` | `Administrator` | 首个后台管理员展示名 |
| `ADMIN_TOKEN_EXPIRE_HOURS` | `12` | 后台登录 Token 有效小时数 |
| `OSS_ENDPOINT` | 空 | OSS Endpoint |
| `OSS_ACCESS_KEY_ID` | 空 | OSS AccessKey ID |
| `OSS_ACCESS_KEY_SECRET` | 空 | OSS AccessKey Secret |
| `OSS_BUCKET_NAME` | 空 | OSS Bucket |
| `OSS_BASE_URL` | 空 | OSS 访问域名 |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | AI 服务 Base URL |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek API Key |
| `DEEPSEEK_MODEL` | `deepseek-chat` | AI 模型名 |

微信登录说明：`WECHAT_APPID` 或 `WECHAT_SECRET` 为空时，后端会使用 `dev_{code}` 作为本地开发 openid，方便不接入真实微信配置时调试。

后台管理员说明：只有数据库中没有管理员账号，且设置了 `ADMIN_INIT_PASSWORD` 时，系统才会自动创建首个管理员。公开部署后请使用强密码，并妥善保存。

## 常用接口

| 模块 | 路径 |
| --- | --- |
| 认证 | `/api/auth/**` |
| 宝宝与家庭 | `/api/baby/**` |
| 日常记录 | `/api/records/**` |
| 积分与抽奖 | `/api/points/**` |
| 社区讨论 | `/api/discussions/**` |
| 文件上传 | `/api/upload` |
| AI 分析 | `/api/smart-analysis/**` |
| 管理后台 API | `/admin/api/**` |
| 管理后台页面 | `/admin/index.html` |

统一响应结构：

```json
{
  "state": "000000",
  "msg": "success",
  "data": {}
}
```

## 测试

测试环境默认使用 `child_notes_test` 数据库，可通过 `MYSQL_TEST_URL`、`MYSQL_TEST_USER`、`MYSQL_TEST_PWD` 覆盖。

```bash
mvn -Ptest test
```

## Docker

```bash
docker build --build-arg BUILD_ENV=local -t child-notes-backend .

docker run --rm -p 8080:8080 \
  -e MYSQL_URL="jdbc:mysql://host.docker.internal:3306/child_notes?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true" \
  -e MYSQL_USER=root \
  -e MYSQL_PWD=your-password \
  -e JWT_SECRET=replace-with-a-long-random-secret \
  child-notes-backend
```

## 项目结构

```text
src/main/java/com/ycz/childnotesbackend/
├── agent/          # AI 分析 Agent
├── config/         # 配置属性
├── controller/     # REST Controller
├── filter/         # 请求过滤器
├── interceptor/    # 鉴权与限流拦截器
├── mapper/         # MyBatis-Plus Mapper
├── model/          # Entity / DTO / 响应模型
├── service/        # 业务接口与实现
└── util/           # 工具类
```

## 开源安全说明

- 真实微信、AI、OSS、数据库配置已从仓库文件中移除。
- `logs/`、`data/`、`.agentscope/`、`.m2/`、`.idea/`、`.codegraph/` 等本地产物已加入 `.gitignore`。
- 生产环境务必设置强 `JWT_SECRET`、后台管理员强密码，并通过平台 Secret/环境变量管理云服务密钥。
