# 书签绑定功能 RESTful API 接口文档

## 概述

本文档详细描述了博客系统书签绑定功能的完整RESTful API接口，支持标签的智能创建、文章关联管理、不区分大小写搜索等功能。

---

## 基础信息

- **Base URL**: `/api`
- **Content-Type**: `application/json`
- **认证方式**: JWT Bearer Token
- **字符编码**: UTF-8

---

## 1. 标签搜索与智能创建接口

### 1.1 优化标签搜索（支持不区分大小写）

**接口地址**: `GET /api/tags/search`

**功能描述**: 支持不区分大小写的标签搜索，返回分页结果

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| q | string | 否 | - | 搜索关键词 |
| page | int | 否 | 0 | 页码 |
| size | int | 否 | 20 | 每页大小 (1-100) |
| ignoreCase | boolean | 否 | true | 是否忽略大小写 |

**请求示例**:
```bash
GET /api/tags/search?q=java&page=0&size=10&ignoreCase=true
```

**响应示例**:
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "JavaScript",
        "slug": "javascript",
        "description": "JavaScript编程语言",
        "usageCount": 15,
        "postCount": 15,
        "color": "#f7df1e",
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00"
      },
      {
        "id": 2,
        "name": "Java",
        "slug": "java",
        "description": "Java编程语言",
        "usageCount": 8,
        "postCount": 8,
        "color": "#007396",
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### 1.2 智能标签创建接口

**接口地址**: `POST /api/tags/smart-create`

**功能描述**: 智能创建标签，如果标签已存在（不区分大小写）则返回现有标签，否则创建新标签

**权限要求**: 需要登录用户权限

**请求体**:
```json
{
  "name": "Vue.js",
  "description": "Vue.js框架技术",
  "color": "#4FC08D"
}
```

**请求参数说明**:
| 字段名 | 类型 | 必填 | 验证规则 | 说明 |
|--------|------|------|----------|------|
| name | string | 是 | 1-50字符，非空 | 标签名称 |
| description | string | 否 | 最大200字符 | 标签描述 |
| color | string | 否 | 十六进制颜色码或为空 | 标签颜色，如 #4FC08D |

**响应示例**:
```json
{
  "success": true,
  "message": "标签创建成功",
  "data": {
    "id": 2,
    "name": "Vue.js",
    "slug": "vue-js",
    "description": "Vue.js框架技术",
    "usageCount": 0,
    "postCount": 0,
    "color": "#4FC08D",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
}
```

---

## 2. 文章标签关联管理接口

### 2.1 添加单个标签到文章

**接口地址**: `POST /api/posts/{postId}/tags/{tagId}`

**功能描述**: 将指定标签添加到指定文章

**权限要求**: 需要登录用户权限

**路径参数**:
- `postId` (long): 文章ID
- `tagId` (long): 标签ID

**响应示例**:
```json
{
  "success": true,
  "message": "标签添加成功"
}
```

### 2.2 从文章移除单个标签

**接口地址**: `DELETE /api/posts/{postId}/tags/{tagId}`

**功能描述**: 从指定文章移除指定标签

**权限要求**: 需要登录用户权限

**路径参数**:
- `postId` (long): 文章ID
- `tagId` (long): 标签ID

**响应示例**:
```json
{
  "success": true,
  "message": "标签移除成功"
}
```

### 2.3 批量设置文章标签

**接口地址**: `PUT /api/posts/{postId}/tags`

**功能描述**:替换文章的所有标签为指定的标签列表

**权限要求**: 需要登录用户权限

**路径参数**:
- `postId` (long): 文章ID

**请求体**:
```json
{
  "tagIds": [1, 2, 3]
}
```

**请求参数说明**:
| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tagIds | array | 否 | 标签ID列表，空数组表示清除所有标签 |

**响应示例**:
```json
{
  "success": true,
  "message": "标签设置成功",
  "data": {
    "id": 1,
    "title": "我的文章",
    "slug": "my-article",
    "excerpt": "文章摘要...",
    "content": "# 我的文章\n\n这是文章内容...",
    "status": "PUBLISHED",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T15:30:00",
    "author": {
      "id": 1,
      "username": "admin",
      "displayName": "管理员",
      "role": "ADMIN"
    },
    "category": {
      "id": 1,
      "name": "技术",
      "slug": "tech"
    },
    "tags": [
      {
        "id": 1,
        "name": "JavaScript",
        "slug": "javascript"
      },
      {
        "id": 2,
        "name": "Vue.js",
        "slug": "vue-js"
      },
      {
        "id": 3,
        "name": "前端",
        "slug": "frontend"
      }
    ],
    "featuredImage": null,
    "viewCount": 156,
    "publishedAt": "2024-01-01T10:00:00"
  }
}
```

---

## 3. 标签关联文章管理接口

### 3.1 获取标签关联文章简要信息

**接口地址**: `GET /api/tags/{tagId}/posts-summary`

**功能描述**: 获取指定标签关联的所有文章的简要信息，支持分页和状态筛选

**路径参数**:
- `tagId` (long): 标签ID

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | int | 否 | 0 | 页码 |
| size | int | 否 | 10 | 每页大小 (1-100) |
| status | string | 否 | - | 文章状态筛选 (PUBLISHED/DRAFT/ARCHIVED) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Vue.js入门教程",
        "slug": "vue-js-tutorial",
        "excerpt": "Vue.js基础入门教程，包含基本概念...",
        "status": "PUBLISHED",
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T15:30:00"
      },
      {
        "id": 2,
        "title": "Vue.js组件开发",
        "slug": "vue-js-component",
        "excerpt": "深入理解Vue.js组件的创建和使用...",
        "status": "PUBLISHED",
        "createdAt": "2024-01-02T09:15:00",
        "updatedAt": "2024-01-02T14:20:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 15,
    "totalPages": 2,
    "first": true,
    "last": false
  }
}
```

### 3.2 批量添加文章到标签

**接口地址**: `POST /api/tags/{tagId}/posts`

**功能描述**: 将多篇文章批量添加到指定标签

**权限要求**: 需要登录用户权限

**路径参数**:
- `tagId` (long): 标签ID

**请求体**:
```json
{
  "postIds": [1, 2, 3]
}
```

**请求参数说明**:
| 字段名 | 类型 | 必填 | 验证规则 | 说明 |
|--------|------|------|----------|------|
| postIds | array | 是 | 非空数组 | 文章ID列表 |

**响应示例**:
```json
{
  "success": true,
  "message": "批量添加成功，共添加3篇文章"
}
```

### 3.3 批量从标签移除文章

**接口地址**: `DELETE /api/tags/{tagId}/posts`

**功能描述**: 从指定标签批量移除多篇文章

**权限要求**: 需要登录用户权限

**路径参数**:
- `tagId` (long): 标签ID

**请求体**:
```json
{
  "postIds": [1, 2, 3]
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "批量移除成功，共移除3篇文章"
}
```

---

## 4. 优化现有接口

### 4.1 标签建议接口

**接口地址**: `GET /api/tags/suggestions`

**功能描述**: 根据关键词提供标签建议，支持不区分大小写搜索

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| q | string | 是 | - | 搜索关键词 |
| limit | int | 否 | 8 | 返回数量限制 (1-20) |
| ignoreCase | boolean | 否 | true | 是否忽略大小写 |

**请求示例**:
```bash
GET /api/tags/suggestions?q=vue&limit=10&ignoreCase=true
```

**响应示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Vue.js",
      "slug": "vue-js",
      "description": "Vue.js框架技术",
      "usageCount": 12,
      "postCount": 12,
      "color": "#4FC08D",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    },
    {
      "id": 2,
      "name": "Vue Router",
      "slug": "vue-router",
      "description": "Vue.js官方路由管理器",
      "usageCount": 5,
      "postCount": 5,
      "color": "#42b883",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    }
  ]
}
```

---

## 5. 错误处理

### 5.1 标准错误响应格式

所有API接口在发生错误时都会返回统一的错误响应格式：

```json
{
  "success": false,
  "error": "ERROR_CODE",
  "message": "错误描述信息"
}
```

### 5.2 常见错误码

| 错误码 | HTTP状态码 | 说明 |
|----------|-------------|------|
| `TAG_NOT_FOUND` | 404 | 标签不存在 |
| `POST_NOT_FOUND` | 404 | 文章不存在 |
| `DUPLICATE_RESOURCE` | 409 | 重复资源（标签已存在） |
| `VALIDATION_ERROR` | 400 | 参数验证错误 |
| `UNAUTHORIZED` | 401 | 未授权访问 |
| `RESOURCE_NOT_FOUND` | 404 | 通用资源不存在 |

### 5.3 具体错误示例

**标签不存在**:
```json
{
  "success": false,
  "error": "TAG_NOT_FOUND",
  "message": "Tag not found with id: 999"
}
```

**参数验证错误**:
```json
{
  "success": false,
  "error": "VALIDATION_ERROR",
  "message": "标签名称不能为空"
}
```

**重复标签**:
```json
{
  "success": false,
  "error": "DUPLICATE_RESOURCE",
  "message": "Tag name already exists: JavaScript"
}
```

**未授权访问**:
```json
{
  "success": false,
  "error": "UNAUTHORIZED",
  "message": "Token无效或已过期"
}
```

---

## 6. 使用场景示例

### 6.1 文章编辑页标签功能

```javascript
// 1. 搜索标签（不区分大小写）
GET /api/tags/search?q=javascript&ignoreCase=true

// 2. 如果没有找到，智能创建标签
POST /api/tags/smart-create
{
  "name": "JavaScript",
  "description": "JavaScript编程语言",
  "color": "#f7df1e"
}

// 3. 设置文章标签
PUT /api/posts/123/tags
{
  "tagIds": [1, 2, 3]
}
```

### 6.2 标签管理页功能

```javascript
// 1. 搜索标签
GET /api/tags/search?q=vue&ignoreCase=true

// 2. 查看标签关联文章
GET /api/tags/1/posts-summary?page=0&size=10&status=PUBLISHED

// 3. 批量添加文章到标签
POST /api/tags/1/posts
{
  "postIds": [4, 5, 6]
}

// 4. 批量移除文章
DELETE /api/tags/1/posts
{
  "postIds": [7, 8]
}
```

---

## 7. 接口限制说明

### 7.1 认证要求
- 所有写操作（POST、PUT、DELETE）都需要JWT token认证
- 搜索和查询接口无需认证

### 7.2 请求频率限制
- 搜索接口：每分钟最多100次请求
- 创建接口：每分钟最多20次请求
- 批量操作：每分钟最多10次请求

### 7.3 数据限制
- 标签名称：1-50字符
- 标签描述：最多200字符
- 单次批量操作：最多处理50个项目
- 搜索关键词：最多100字符

---

## 8. 技术实现细节

### 8.1 大小写处理
- 使用数据库的LOWER函数实现不区分大小写搜索
- 智能创建时先进行不区分大小写查询
- 标签名称在存储时保持原始大小写格式

### 8.2 关联关系管理
- 使用JPA的ManyToMany关系维护标签-文章关联
- 自动维护usageCount计数器
- 事务性操作保证数据一致性

### 8.3 性能优化
- 支持分页查询减少数据传输
- 使用连接池优化数据库连接
- 合理的缓存策略（如有需要）

---

**文档版本**: v1.0
**最后更新**: 2024-01-01
**维护团队**: Kana Kana Team