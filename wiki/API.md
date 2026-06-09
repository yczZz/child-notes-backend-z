# API 接口文档

## 通用说明

### Base URL

```
http://{host}:8080
```

### 认证方式

除 `/api/auth/wx-login` 外，所有 `/api/**` 接口均需携带 Token。支持以下方式：

| 方式 | 格式 |
|------|------|
| Authorization Header | `Authorization: Bearer <token>` |
| Authorization Header（无 Bearer 前缀） | `Authorization: <token>` |
| token Header | `token: <token>` |
| URL 参数 | `?token=<token>` |

### 统一响应格式

```json
{
  "state": "000000",
  "msg": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `state` | String | 状态码，`000000` 为成功，`000520` 为失败 |
| `msg` | String | 消息 |
| `data` | Object | 业务数据 |

### 通用错误

| HTTP 状态码 | 说明 |
|-------------|------|
| 401 | 未登录或 Token 无效/过期 |
| 404 | 资源不存在 |

---

## 认证模块 `/api/auth`

### POST `/api/auth/wx-login`

微信小程序登录（**无需认证**）。

**请求体：**

```json
{
  "code": "string (必填, 微信登录code)",
  "userInfo": {
    "nickName": "string",
    "avatarUrl": "string",
    "gender": 0
  }
}
```

**响应体：**

```json
{
  "state": "000000",
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "expiresAt": "2026-06-26T10:30:00",
    "userInfo": {
      "id": 1,
      "openid": "oXXXXXXXXXXXX",
      "nickName": "微信用户",
      "avatarUrl": "https://...",
      "gender": 1
    }
  }
}
```

### GET `/api/auth/me`

获取当前登录用户信息。

**响应体：**

```json
{
  "state": "000000",
  "msg": "success",
  "data": {
    "id": 1,
    "openid": "oXXXXXXXXXXXX",
    "nickName": "微信用户",
    "avatarUrl": "https://...",
    "gender": 1
  }
}
```

### PUT `/api/auth/profile`

更新用户资料。

**请求体：**

```json
{
  "nickName": "新昵称",
  "avatarUrl": "https://...",
  "gender": 1
}
```

所有字段均为可选，仅传需要更新的字段。

---

## 宝宝管理 `/api/baby`

### GET `/api/baby/current`

获取当前用户的第一个宝宝（按 ID 升序）。

**响应体：**

```json
{
  "state": "000000",
  "msg": "success",
  "data": {
    "id": 1,
    "name": "小宝",
    "avatar": "https://...",
    "gender": "boy",
    "birthDate": "2025-01-15",
    "age": "1岁4个月12天"
  }
}
```

> `age` 由服务端根据 `birthDate` 自动计算。

### GET `/api/baby/list`

获取当前用户的所有宝宝列表。

### POST `/api/baby/add`

添加宝宝。

**请求体：**

```json
{
  "name": "小宝",
  "avatar": "https://...",
  "gender": "boy",
  "birthDate": "2025-01-15"
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| name | String | 否 | "宝宝" | 会自动 trim |
| avatar | String | 否 | "" | |
| gender | String | 否 | "boy" | |
| birthDate | String | 否 | null | 格式 YYYY-MM-DD |

### PUT `/api/baby/update`

更新宝宝信息。

**请求体：**

```json
{
  "id": 1,
  "name": "新名字",
  "avatar": "https://...",
  "gender": "girl",
  "birthDate": "2025-02-01"
}
```

> `id` 为空时更新当前用户的第一个宝宝。所有其他字段均为可选。

---

## 记录管理 `/api/records`

### 查询接口

#### GET `/api/records/today`

获取今日所有记录，按类型分组返回。

**响应体：**

```json
{
  "state": "000000",
  "msg": "success",
  "data": {
    "date": "2026-05-27",
    "feeds": [],
    "diapers": [],
    "sleeps": [],
    "supplements": [],
    "growths": [],
    "temperatures": [],
    "abnormals": [],
    "pumps": [],
    "complementaries": []
  }
}
```

#### GET `/api/records/today/stats`

获取今日统计概览。

**响应体：**

```json
{
  "state": "000000",
  "msg": "success",
  "data": {
    "lastFeedTime": "2026-05-27 14:30",
    "timeSinceLastFeed": "2小时15分钟",
    "feedCount": 5,
    "totalMilk": 450,
    "todaySleepTotal": "3小时20分钟",
    "totalSleepMin": 200,
    "sleepCount": 3,
    "hasFever": false,
    "feverInfo": null,
    "lastMedicineTime": null,
    "hasDiarrhea": false,
    "diarrheaTime": null,
    "diarrheaTypes": null,
    "hasOtherAbnormal": false,
    "otherAbnormalInfo": null,
    "dailyTips": ["母乳喂养的宝宝每天大便2-5次是正常的哦"]
  }
}
```

> 健康追踪会跨天延续。恢复标记只关闭它之前的异常，恢复后新增异常会重新打开追踪。

#### GET `/api/records/date?date=YYYY-MM-DD`

按日期查询记录，返回格式同 `/today`。

发热体温、腹泻尿布和异常症状记录会额外返回 `resolved` 和 `resolvedTime`。恢复状态支持跨天回看；恢复后新增加的异常记录仍会返回 `resolved: false`。

#### GET `/api/records/stats/date?date=YYYY-MM-DD`

按日期查询统计数据。

**响应体：**

```json
{
  "state": "000000",
  "msg": "success",
  "data": {
    "date": "2026-05-27",
    "recordCount": 12,
    "feed": {
      "count": 5, "breastCount": 2, "bottleCount": 2, "expressedCount": 1,
      "totalMilk": 450, "bottleMilk": 200, "expressedMilk": 100,
      "breastDurationSec": 1800, "breastLeftDurationSec": 900, "breastRightDurationSec": 900,
      "lastFeedTime": "2026-05-27 14:30"
    },
    "sleep": { "count": 3, "ongoingCount": 0, "totalDurationMin": 200, "totalDurationSec": 12000 },
    "diaper": { "count": 4, "wetCount": 2, "dirtyCount": 1, "bothCount": 1, "dryCount": 0, "abnormalCount": 0, "diarrheaCount": 0 },
    "temperature": { "count": 1, "abnormalCount": 0, "maxTemperature": 36.8, "maxTemperatureTime": "2026-05-27 08:00" },
    "supplement": { "count": 1, "medicineCount": 0, "nutritionCount": 1 },
    "growth": { "count": 1, "latestHeight": 70.5, "latestWeight": 8.2, "latestTime": "2026-05-27 09:00" },
    "pump": { "count": 1, "totalAmount": 120, "totalDurationSec": 1200, "totalDurationMin": 20 },
    "complementary": { "count": 1, "abnormalCount": 0 },
    "abnormal": { "count": 0, "feverCount": 0, "diarrheaCount": 0, "vomitCount": 0, "medicineCount": 0 },
    "vaccine": { "count": 0 },
    "activity": { "count": 0, "totalDurationMin": 0, "totalDurationSec": 0 },
    "milestone": { "count": 0 }
  }
}
```

#### GET `/api/records/stats/range?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`

按日期范围查询统计，返回每日统计数组。最大范围 370 天，超出自动截断。

#### GET `/api/records/history`

获取今日之前的所有历史记录，按日期倒序分组。

#### GET `/api/records/feed/latest`

获取今日最近一条喂养记录。

#### GET `/api/records/sleep/latest`

获取今日最近一条睡眠记录。

#### GET `/api/records/vaccines`

获取所有疫苗记录，按日期倒序。

#### GET `/api/records/activities`

获取所有活动记录，按日期倒序。

#### GET `/api/records/milestones`

获取所有里程碑记录，按日期正序。

---

### 新增记录接口

所有新增记录接口均为 `POST`，请求体为对应记录类型的 JSON。时间字段格式为 `yyyy-MM-dd HH:mm` 或 `yyyy-MM-dd HH:mm:ss`。

#### POST `/api/records/feed` — 喂养

```json
{
  "type": "breast",
  "side": "left",
  "duration": 15,
  "leftDuration": 10,
  "leftDurationSec": 600,
  "leftStartTime": "2026-05-27 14:00",
  "rightDuration": 5,
  "rightDurationSec": 300,
  "rightStartTime": "2026-05-27 14:10",
  "amount": 120,
  "time": "2026-05-27 14:00",
  "saveProgress": true
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| type | String | 喂养类型：`breast`（母乳）/ `expressed`（母乳瓶喂）/ 其他（奶粉瓶喂） |
| side | String | 喂养侧：`left`/`right` |
| duration | Integer | 总时长（分钟） |
| leftDuration / rightDuration | Integer | 左/右侧时长（分钟） |
| leftDurationSec / rightDurationSec | Integer | 左/右侧时长（秒），优先于分钟 |
| leftStartTime / rightStartTime | String | 左/右侧开始时间（用于计算进行中时长） |
| amount | Integer | 奶量（ml），非母乳时使用 |
| time | String | 记录时间 |
| saveProgress | Boolean | 是否保存进度 |

#### POST `/api/records/diaper` — 换尿布

```json
{
  "type": "wet",
  "color": "yellow",
  "urineColor": "clear",
  "consistency": "soft",
  "diarrhea": ["watery"],
  "abnormal": false,
  "photos": ["https://..."],
  "time": "2026-05-27 10:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| type | String | `wet`/`dirty`/`both`/`dry` |
| color | String | 便便颜色 |
| urineColor | String | 尿液颜色 |
| consistency | String | 便便稠度 |
| diarrhea | List\<String\> | 腹泻特征列表 |
| abnormal | Boolean | 是否异常 |
| photos | List\<String\> | 照片 URL 列表 |
| time | String | 记录时间 |

#### POST `/api/records/sleep` — 睡眠

```json
{
  "startTime": "2026-05-27 13:00",
  "endTime": null,
  "duration": null
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| startTime | String | 入睡时间 |
| endTime | String | 醒来时间（空表示进行中） |
| duration | Integer | 睡眠时长（分钟），进行中时可为空 |

> 进行中的睡眠可后续通过 `PUT /api/records/sleep/{sleepId}/wake` 标记醒来。

#### POST `/api/records/temperature` — 体温

```json
{
  "temperature": 37.5,
  "isAbnormal": true,
  "note": "额温枪测量",
  "time": "2026-05-27 08:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| temperature | BigDecimal | 体温值（℃） |
| isAbnormal | Boolean | 是否异常（≥37.3℃ 自动标记异常） |
| note | String | 备注 |
| time | String | 记录时间 |

#### POST `/api/records/supplement` — 补充剂

```json
{
  "type": "nutrition",
  "name": "维生素D",
  "dose": "400IU",
  "note": "",
  "time": "2026-05-27 09:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| type | String | `medicine`（药物）/ `nutrition`（营养品） |
| name | String | 名称 |
| dose | String | 剂量 |
| note | String | 备注 |
| time | String | 记录时间 |

#### POST `/api/records/growth` — 成长数据

```json
{
  "height": 70.5,
  "weight": 8.2,
  "time": "2026-05-27 09:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| height | BigDecimal | 身高（cm） |
| weight | BigDecimal | 体重（kg） |
| time | String | 记录时间 |

#### POST `/api/records/abnormal` — 异常记录

```json
{
  "temperature": 38.5,
  "respiratory": ["cough"],
  "diarrhea": ["watery"],
  "vomit": "少量",
  "medicine": {
    "name": "布洛芬",
    "dose": "2ml"
  },
  "note": "精神状态尚可",
  "photos": ["https://..."],
  "time": "2026-05-27 15:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| temperature | BigDecimal | 体温 |
| respiratory | List\<String\> | 呼吸道症状 |
| diarrhea | List\<String\> | 腹泻特征 |
| vomit | String | 呕吐描述 |
| medicine | MedicineDto | 用药信息（name + dose） |
| note | String | 备注 |
| photos | List\<String\> | 照片 |
| time | String | 记录时间 |

> 服务端会根据 `temperature ≥ 37.3` 自动将 `record_sub_type` 设为 `fever`，否则根据腹泻/呕吐/用药设置对应子类型。

#### POST `/api/records/pump` — 吸奶

```json
{
  "leftDuration": 10,
  "rightDuration": 10,
  "leftAmount": 60,
  "rightAmount": 60,
  "totalAmount": 120,
  "note": "",
  "time": "2026-05-27 12:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| leftDuration / rightDuration | Integer | 左/右侧时长（分钟） |
| leftAmount / rightAmount | Integer | 左/右侧量（ml） |
| totalAmount | Integer | 总量（ml） |
| note | String | 备注 |
| time | String | 记录时间 |

#### POST `/api/records/complementary` — 辅食

```json
{
  "foodTypes": ["蔬菜", "谷物"],
  "texture": "泥状",
  "foodName": "胡萝卜米糊",
  "amount": "50",
  "amountUnit": "g",
  "note": "",
  "photos": ["https://..."],
  "reaction": "无不良反应",
  "abnormal": false,
  "time": "2026-05-27 11:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| foodTypes | List\<String\> | 食物类型标签 |
| texture | String | 质地 |
| foodName | String | 食物名称 |
| amount | String | 数量 |
| amountUnit | String | 单位 |
| note | String | 备注 |
| photos | List\<String\> | 照片 |
| reaction | String | 反应描述 |
| abnormal | Boolean | 是否异常反应 |
| time | String | 记录时间 |

#### POST `/api/records/vaccine` — 疫苗

```json
{
  "name": "乙肝疫苗（第二针）",
  "nextName": "脊灰疫苗",
  "nextDate": "2026-06-27",
  "note": "",
  "time": "2026-05-27 10:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 疫苗名称 |
| nextName | String | 下一针名称 |
| nextDate | String | 下一针日期 |
| note | String | 备注 |
| time | String | 接种时间 |

#### POST `/api/records/activity` — 活动

```json
{
  "name": "户外散步",
  "category": "outdoor",
  "duration": 30,
  "time": "2026-05-27 16:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 活动名称 |
| category | String | 活动分类 |
| duration | Integer | 时长（分钟） |
| time | String | 记录时间 |

#### POST `/api/records/milestone` — 里程碑

```json
{
  "title": "第一次翻身",
  "content": "宝宝今天第一次自己翻身了！",
  "date": "2026-05-27",
  "photos": ["https://..."]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| title | String | 里程碑标题 |
| content | String | 内容描述 |
| date | String | 日期（YYYY-MM-DD，仅日期无时间） |
| photos | List\<String\> | 照片 |

#### POST `/api/records/fever-resolved`

标记发烧已消退。无需请求体。

#### POST `/api/records/diarrhea-resolved`

标记腹泻已消退。无需请求体。

#### POST `/api/records/abnormal-resolved`

标记其他异常已恢复。无需请求体。

---

### 更新接口

#### PUT `/api/records/{id}`

更新任意记录的 payload JSON。

**请求体：** 纯 JSON 字符串（记录类型的完整 JSON 数据）

#### PUT `/api/records/sleep/{sleepId}/wake`

标记睡眠记录为醒来。自动计算时长（从 startTime 到当前时间，向上取整到分钟）。

**响应体：** 更新后的 SleepRecordDto（含 endTime 和 duration）

#### PUT `/api/records/milestone/{id}`

更新里程碑记录。

**请求体：** MilestoneRecordDto 的 JSON

---

## 文件上传 `/api/upload`

### POST `/api/upload`

上传文件至阿里云 OSS。

**请求格式：** `multipart/form-data`，字段名 `file`

**最大文件大小：** 20MB

**响应体：**

```json
{
  "state": "000000",
  "msg": "success",
  "data": {
    "url": "https://your-oss-domain.example.com/2026/05/27/abc123.jpg"
  }
}
```

> 文件存储路径格式：`{yyyy/MM/dd}/{UUID}.{ext}`

---

## 测试接口 `/test`

### POST `/test/note`

旧版笔记创建接口（无需认证）。

**请求体：**

```json
{
  "childName": "小明",
  "title": "今日记录",
  "content": "今天很开心"
}
```
