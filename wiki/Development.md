# 开发指南

## 本地开发环境搭建

### 1. 环境要求

- **JDK 17**：项目使用 Java 17 语法特性
- **Maven 3.6+**：依赖管理与构建
- **MySQL 8.x**：数据库（默认端口 7904）
- **IDE**：推荐 IntelliJ IDEA（项目已包含 `.idea` 配置）

### 2. 克隆与配置

```bash
git clone <repo-url>
cd child-notes-backend
```

### 3. 数据库准备

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS child_notes
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
```

项目启动时会自动通过 `schema.sql` 创建/补齐表结构，无需手动建表。

### 4. 启动应用

```bash
mvn spring-boot:run -Plocal
```

默认连接 `127.0.0.1:7904/child_notes`，可通过环境变量覆盖。

### 5. 验证

```bash
# 健康检查（返回 401 为正常，说明服务启动成功）
curl http://localhost:8080/api/auth/me

# 开发模式登录
curl -X POST http://localhost:8080/api/auth/wx-login \
  -H "Content-Type: application/json" \
  -d '{"code":"test123"}'
```

> 未配置 `WECHAT_APPID` 时自动进入开发模式，任意 code 即可登录。

---

## 代码规范

### 命名约定

| 类型 | 规范 | 示例 |
|------|------|------|
| Controller | `{Module}Controller` | `BabyController` |
| Service 接口 | `{Module}Service` | `RecordService` |
| Service 实现 | `{Module}ServiceImpl` | `RecordServiceImpl` |
| Mapper | `{Entity}Mapper` | `ChildRecordMapper` |
| Entity | 名词，与表名对应 | `AppUser`, `ChildRecord` |
| DTO | `{Entity}{Purpose}Dto` | `FeedRecordDto`, `LoginUserDto` |
| 请求 DTO | `{Action}{Entity}Request` | `CreateBabyRequest`, `UpdateBabyRequest` |
| 响应 DTO | `{Purpose}Response` | `DailyRecordsResponse`, `TodayStatsResponse` |

### 包结构

```
com.ycz.childnotesbackend
├── config/       # @Configuration + @ConfigurationProperties
├── context/      # 请求上下文（ThreadLocal）
├── controller/   # @RestController，仅做参数接收和响应封装
├── filter/       # @Component + OncePerRequestFilter
├── interceptor/  # @Component + HandlerInterceptor
├── mapper/       # @Mapper（MyBatis-Plus BaseMapper）
├── model/
│   ├── auth/     # 认证领域模型
│   ├── base/     # 通用模型（Response 等）
│   ├── dto/      # 按模块分子包（auth/, baby/, record/）
│   └── entity/   # @TableName 实体
├── service/      # 接口
│   └── impl/     # @Service 实现
└── util/         # 工具类（@Component）
```

### 依赖注入

使用**构造器注入**（不使用 `@Autowired` 字段注入）：

```java
// ✅ 推荐
public class RecordServiceImpl implements RecordService {
    private final ChildRecordMapper childRecordMapper;
    private final ObjectMapper objectMapper;

    public RecordServiceImpl(ChildRecordMapper childRecordMapper, ObjectMapper objectMapper) {
        this.childRecordMapper = childRecordMapper;
        this.objectMapper = objectMapper;
    }
}
```

### 统一响应

所有 Controller 返回值使用 `Response<T>` 封装：

```java
// 成功
return new Response<>(data);

// 失败
return new Response<>("000520", "错误信息");

// 无数据成功
return Response.SUCCESS;
```

---

## 新增记录类型指南

以新增"洗澡"记录类型（`bath`）为例：

### 1. 创建 DTO

在 `model/dto/record/` 下新建 `BathRecordDto.java`：

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class BathRecordDto extends BaseRecordDto {
    private String time;            // 记录时间
    private Integer duration;       // 时长（分钟）
    private String waterTemp;       // 水温
    private String note;            // 备注
}
```

### 2. 添加 Service 方法

在 `RecordService` 接口新增：

```java
BathRecordDto addBathRecord(BathRecordDto data);
```

在 `RecordServiceImpl` 实现：

```java
@Override
public BathRecordDto addBathRecord(BathRecordDto data) {
    return addRecord("bath", data, BathRecordDto::getTime);
}
```

### 3. 更新摘要填充

在 `RecordServiceImpl.fillRecordSummary()` 中添加：

```java
if (payload instanceof BathRecordDto) {
    BathRecordDto bath = (BathRecordDto) payload;
    record.setDurationSec(bath.getDuration() == null ? null : bath.getDuration() * 60);
    return;
}
```

### 4. 更新 payload 解析

在 `RecordServiceImpl.readPayloadJson()` 的 switch 中添加：

```java
case "bath":
    return objectMapper.readValue(payloadJson, BathRecordDto.class);
```

### 5. 更新时间提取

在 `RecordServiceImpl.extractFrontTime()` 中添加：

```java
if (payload instanceof BathRecordDto) return ((BathRecordDto) payload).getTime();
```

### 6. 更新查询响应

如需在 `DailyRecordsResponse` 中返回，需：

1. 在 `DailyRecordsResponse` 中添加 `List<BathRecordDto> baths` 字段
2. 在 `addToDailyResponse()` 的 switch 中添加 `case "bath"` 分支

### 7. 更新统计（可选）

如需在 `DailyStatsResponse` 中统计：

1. 在 `DailyStatsResponse` 中添加 `BathStats` 内部类
2. 在 `addToDailyStats()` 的 switch 中添加统计逻辑
3. 实现 `addBathStats()` 方法

### 8. 添加 Controller 接口

在 `RecordController` 中新增：

```java
@PostMapping("/bath")
public Response<BathRecordDto> addBathRecord(@RequestBody BathRecordDto data) {
    return new Response<>(recordService.addBathRecord(data));
}
```

### 9. 数据库（通常无需变更）

`child_record` 表的 `payload_json` 字段可存储任意 JSON，新记录类型无需 DDL 变更。

如需新增摘要列（如需要按水温查询），则在 `schema.sql` 中以幂等方式添加列。

---

## 常用开发操作

### 编译打包

```bash
# 编译
mvn compile

# 打包（跳过测试）
mvn package -Ptest -DskipTests

# 运行 jar
java -jar target/child-notes-backend-0.0.1-SNAPSHOT.jar
```

### 运行测试

```bash
# 全部测试
mvn -Ptest test

# 单个测试类
mvn -Ptest test -Dtest=ApiControllerTests
```

### 添加数据库列

在 `schema.sql` 末尾以幂等方式添加：

```sql
SET @col_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'child_record'
      AND COLUMN_NAME = 'new_column'
);
SET @col_sql = IF(@col_count = 0, 'ALTER TABLE child_record ADD COLUMN new_column VARCHAR(64)', 'SELECT 1');
PREPARE col_stmt FROM @col_sql;
EXECUTE col_stmt;
DEALLOCATE PREPARE col_stmt;
```

> 切勿直接写 `ALTER TABLE child_record ADD COLUMN new_column VARCHAR(64)`，因为第二次启动会报列已存在的错误。

### 添加索引

同样使用条件判断确保幂等：

```sql
SET @idx_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'child_record'
      AND INDEX_NAME = 'idx_new_index'
);
SET @idx_sql = IF(@idx_count = 0, 'CREATE INDEX idx_new_index ON child_record(new_column)', 'SELECT 1');
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;
```

---

## 注意事项

### ThreadLocal 清理

`AuthContext` 使用 ThreadLocal 存储用户上下文，由 `AuthInterceptor.afterCompletion()` 自动清理。在异步场景下需手动管理：

```java
try {
    AuthContext.setCurrentUser(authUser);
    // 业务逻辑
} finally {
    AuthContext.clear();
}
```

### 记录时间格式

前端传入的时间字段格式为 `yyyy-MM-dd HH:mm` 或 `yyyy-MM-dd HH:mm:ss`，`RecordServiceImpl` 会根据长度自动选择解析格式。

### 体温发烧阈值

`37.3℃` 为发烧判定阈值（`FEVER_THRESHOLD`），定义在 `RecordServiceImpl` 中。体温记录和异常记录中，`temperature ≥ 37.3` 自动标记 `abnormal_flag = true`。

### 宝宝年龄计算

`BabyServiceImpl.calcBabyAge()` 根据 `birthDate` 计算年龄，返回格式：
- `X岁Y个月Z天`（1 岁以上）
- `Y个月Z天`（1 岁以下）
- `Z天`（1 个月以下）

### 文件上传

`OssService` 每次上传创建新的 OSSClient 实例并在 finally 中关闭。文件路径格式为 `{yyyy/MM/dd}/{UUID}.{ext}`。未配置 `accessKeyId` 时会抛出 `IllegalStateException("OSS not configured")`。
