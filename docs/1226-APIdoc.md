
## 1. 文章筛选接口

### 1.1 按标签查询文章

根据标签ID分页获取已发布文章列表。

#### 请求

```
GET /api/posts/filter/tag/{tagId}?page=0&size=10&sort=publishedAt,desc
```

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tagId | Long | ✅ 是 | 标签ID |

#### 查询参数

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| page | Integer | 0 | 页码（从0开始） |
| size | Integer | 10 | 每页数量 |
| sort | String | publishedAt,desc | 排序字段（格式：字段名,asc/desc） |

#### 响应

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 6,
        "title": "Spring Boot最佳实践",
        "slug": "spring-boot-best-practices",
        "content": "- this is a test post",
        "excerpt": "test post",
        "status": "PUBLISHED",
        "isFeatured": true,
        "viewCount": 70,
        "publishedAt": "2025-12-26T15:20:46.287",
        "createdAt": "2025-12-15T22:54:17.45",
        "updatedAt": "2025-12-26T19:25:12.35",
        "author": {
          "id": 1,
          "username": "admin",
          "nickname": "konatabloger"
        },
        "category": {
          "id": 1,
          "name": "技术",
          "slug": "tech"
        },
        "tags": [
          {
            "id": 2,
            "name": "Spring Boot",
            "slug": "spring-boot"
          },
          {
            "id": 9,
            "name": "最佳实践",
            "slug": "best-practices"
          }
        ]
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalPages": 1,
    "totalElements": 1,
    "last": true,
    "size": 10,
    "number": 0,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "numberOfElements": 1,
    "first": true,
    "empty": false
  },
  "message": null,
  "error": null
}
```

#### 响应字段说明

**数据字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| content | Array | 文章数据数组 |
| pageable | Object | 分页信息 |
| totalPages | Integer | 总页数 |
| totalElements | Long | 总记录数 |
| first | Boolean | 是否第一页 |
| last | Boolean | 是否最后一页 |
| empty | Boolean | 是否为空 |

**文章内容字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 文章ID |
| title | String | 文章标题 |
| slug | String | URL友好标识符 |
| content | String | 文章内容（Markdown格式） |
| excerpt | String | 文章摘要 |
| status | String | 状态（PUBLISHED/DRAFT） |
| isFeatured | Boolean | 是否推荐 |
| viewCount | Integer | 浏览次数 |
| publishedAt | LocalDateTime | 发布时间 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| author | Object | 作者信息 |
| category | Object | 分类信息 |
| tags | Array | 标签列表 |

#### 示例

```bash
# 查询ID为2的标签下的所有文章，5条一页，按发表时间倒序
curl -X GET "http://localhost:8081/api/posts/filter/tag/2?page=0&size=5&sort=publishedAt,desc" \
  -H "Content-Type: application/json"
```

---

### 1.2 按发表年份查询文章

根据发表年份分页获取已发布文章列表。

#### 请求

```
GET /api/posts/filter/year/{year}?page=0&size=10&sort=publishedAt,desc
```

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 | 限制 |
|--------|------|------|------|------|
| year | Integer | ✅ 是 | 4位数年份 | 1970-9999 |

#### 查询参数

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| page | Integer | 0 | 页码（从0开始） |
| size | Integer | 10 | 每页数量 |
| sort | String | publishedAt,desc | 排序字段（格式：字段名,asc/desc） |

#### 响应

响应结构与 [按标签查询文章](#11-按标签查询文章) 相同。

#### 错误响应

当年份不在有效范围内时：

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "年份参数无效"
  }
}
```

#### 示例

```bash
# 查询2025年发表的所有文章，10条一页，按发表时间倒序
curl -X GET "http://localhost:8081/api/posts/filter/year/2025?page=0&size=10&sort=publishedAt,desc" \
  -H "Content-Type: application/json"

# 查询2024年发表的文章，当前页，每页3条
curl -X GET "http://localhost:8081/api/posts/filter/year/2024?page=0&size=3" \
  -H "Content-Type: application/json"
```

---

## 2. 用户管理接口

### 2.1 更新当前用户信息

更新当前认证用户的用户名或邮箱信息。

#### 请求

```
PUT /api/users/me
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

#### 请求头

| 字段名 | 必填 | 说明 |
|--------|------|------|
| Authorization | ✅ 是 | JWT Token，格式：`Bearer <token>` |
| Content-Type | ✅ 是 | application/json |

#### 请求体

```json
{
  "username": "newusername",
  "email": "newemail@example.com"
}
```

#### 字段说明

| 字段名 | 类型 | 必填 | 说明 | 限制 |
|--------|------|------|------|------|
| username | String | ❌ 否 | 用户名 | 1-50字符，可选（如不提供则不更新） |
| email | String | ❌ 否 | 邮箱地址 | 最大150字符，有效邮箱格式，可选（如不提供则不更新） |

> **注意**: 至少需要提供 `username` 或 `email` 中的一个字段。

#### 成功响应

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "newusername",
    "email": "newemail@example.com",
    "nickname": "konatabloger",
    "role": "ADMIN",
    "avatar": null,
    "isActive": true,
    "lastLoginAt": "2025-12-26T10:30:45",
    "createdAt": "2025-12-15T22:54:17"
  },
  "message": "用户信息更新成功",
  "error": null
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |
| username | String | 用户名 |
| email | String | 邮箱地址 |
| nickname | String | 显示昵称 |
| role | String | 用户角色 |
| avatar | String | 头像URL |
| isActive | Boolean | 是否激活 |
| lastLoginAt | LocalDateTime | 最后登录时间 |
| createdAt | LocalDateTime | 创建时间 |

#### 错误响应

**1. 认证失败（401 Unauthorized）**

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Token无效或已过期"
  }
}
```

**2. 参数验证失败（400 Bad Request）**

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "邮箱格式不正确"
  }
}
```

**3. 重复资源（409 Conflict）**

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "DUPLICATE_RESOURCE",
    "message": "用户名已存在"
  }
}
```

#### 示例

```bash
# 更新用户名
curl -X PUT "http://localhost:8081/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"username": "newadmin"}'

# 更新邮箱
curl -X PUT "http://localhost:8081/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"email": "newadmin@example.com"}'

# 同时更新用户名和邮箱
curl -X PUT "http://localhost:8081/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"username": "admin2025", "email": "admin2025@blog.com"}'

# 错误示例 - 未提供用户名或邮箱
curl -X PUT "http://localhost:8081/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{}'
# 返回：至少需要提供用户名或邮箱
```

---

## 🚦 认证说明

### JWT Token获取

大部分接口需要认证用户才能访问。获取Token的方式：

1. **登录接口**
   ```
   POST /api/auth/login
   Content-Type: application/json

   {
     "username": "admin",
     "password": "123456"
   }
   ```

2. **响应示例**
   ```json
   {
     "success": true,
     "data": {
       "token": "eyJhbGciOiJIUzUxMiJ9...",
       "user": {
         "id": 1,
         "username": "admin"
       }
     },
     "message": "登录成功"
   }
   ```

3. **使用Token**
   ```
   Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
   ```

---

## 📦 通用响应格式

### 成功响应

```json
{
  "success": true,
  "data": { ... },
  "message": "操作成功",
  "error": null
}
```

### 失败响应

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误描述信息"
  }
}
```

### 错误代码说明

| 错误代码 | HTTP状态码 | 说明 |
|----------|------------|------|
| VALIDATION_ERROR | 400 | 参数验证失败 |
| UNAUTHORIZED | 401 | 未认证或Token无效 |
| DUPLICATE_RESOURCE | 409 | 资源已存在 |
| USER_NOT_FOUND | 404 | 用户不存在 |
| POST_NOT_FOUND | 404 | 文章不存在 |
| TAG_NOT_FOUND | 404 | 标签不存在 |
| CATEGORY_NOT_FOUND | 404 | 分类不存在 |

---

## 📝 更新日志

### v1.0 (2025-12-26)

**新增接口**

1. **PostController**
   - ✅ `GET /api/posts/filter/tag/{tagId}` - 按标签分页查询已发布文章
   - ✅ `GET /api/posts/filter/year/{year}` - 按发表年份分页查询已发布文章

2. **UserController**
   - ✅ `PUT /api/users/me` - 更新当前用户信息（用户名/邮箱）

**新增DTO**

- `UserUpdateRequest` - 用户更新请求
- `PostSummaryResponse` - 文章概览响应

**功能特性**

- ✅ 支持分页查询（page, size, sort参数）
- ✅ 排序支持（publishedAt, createdAt等字段）
- ✅ JWT认证机制
- ✅ 参数验证和错误处理
- ✅ 详细的响应数据

---

## 🎯 使用示例

### 完整测试流程

1. **启动应用**
   ```bash
   ./mvnw spring-boot:run
   ```

2. **登录获取Token**
   ```bash
   curl -X POST http://localhost:8081/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"123456"}'
   ```

3. **测试文章筛选接口**
   ```bash
   # 按标签查询
   curl -X GET "http://localhost:8081/api/posts/filter/tag/2?page=0&size=5" \
     -H "Content-Type: application/json"

   # 按年份查询
   curl -X GET "http://localhost:8081/api/posts/filter/year/2025?page=0&size=10" \
     -H "Content-Type: application/json"
   ```

4. **测试用户信息更新**
   ```bash
   curl -X PUT "http://localhost:8081/api/users/me" \
     -H "Authorization: Bearer YOUR_TOKEN_HERE" \
     -H "Content-Type: application/json" \
     -d '{"username":"updatedadmin","email":"admin@example.com"}'
   ```

---

## 📞 联系方式

如有问题，请查看：
- 需求文档: `docs/需求文档.md`
- 控制器文档: `docs/Controller层设计文档.md`
- 用户登录指南: `docs/测试用户登录完整指南.md`

---

**文档结束** 📚