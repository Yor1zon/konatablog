# KONATABLOG Controller层设计文档

## 概述

本文档定义了KONATABLOG博客系统的REST API控制器层设计，基于Service层的业务逻辑实现完整的HTTP接口。

## 设计原则

- **RESTful API**: 遵循REST架构风格
- **JWT认证**: 基于Token的无状态认证
- **权限分离**: 游客只读，博主全权限
- **统一响应**: 标准化的API响应格式
- **异常处理**: 统一的错误响应机制

## 认证机制

### 用户角色
- **GUEST**: 游客，只能查看公开内容
- **ADMIN**: 博主，拥有所有权限

### JWT Token流程
1. 用户通过 `/api/auth/login` 登录获取JWT Token
2. 后续请求在Header中携带 `Authorization: Bearer <token>`
3. 博主专属接口需要验证Token有效性

## API接口设计

### 1. AuthController - 认证控制器

#### 登录接口
```http
POST /api/auth/login
Content-Type: application/json

Request:
{
  "username": "string",  // 用户名或邮箱
  "password": "string"
}

Response:
{
  "success": true,
  "data": {
    "token": "jwt_token_string",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@blog.com",
      "role": "ADMIN",
      "nickname": "博主昵称"
    }
  }
}
```

#### 获取用户信息
```http
GET /api/auth/profile
Authorization: Bearer <token>

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@blog.com",
    "role": "ADMIN",
    "nickname": "博主昵称",
    "avatar": "avatar_url"
  }
}
```

#### 登出接口
```http
POST /api/auth/logout
Authorization: Bearer <token>

Response:
{
  "success": true,
  "message": "登出成功"
}
```

### 2. PostController - 博客文章控制器

#### 获取博客列表（游客可访问）
```http
GET /api/posts?page=0&size=10&sort=createdAt,desc
Response:
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "文章标题",
        "slug": "article-slug",
        "summary": "文章摘要",
        "content": "markdown内容",
        "status": "PUBLISHED",
        "viewCount": 100,
        "createdAt": "2025-01-01T12:00:00",
        "updatedAt": "2025-01-01T12:00:00",
        "publishedAt": "2025-01-01T12:00:00",
        "author": {
          "id": 1,
          "username": "admin",
          "nickname": "博主"
        },
        "category": {
          "id": 1,
          "name": "技术",
          "slug": "tech"
        },
        "tags": [
          {
            "id": 1,
            "name": "Java",
            "slug": "java"
          }
        ]
      }
    ],
    "totalElements": 50,
    "totalPages": 5,
    "size": 10,
    "number": 0
  }
}
```

#### 获取博客详情（游客可访问）
```http
GET /api/posts/{id}
Response: (格式同单篇文章，会增加viewCount)
```

#### 根据Slug获取博客（游客可访问）
```http
GET /api/posts/slug/{slug}
Response: (同上)
```

#### 创建博客（需要认证）
```http
POST /api/posts
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "title": "文章标题",
  "content": "markdown内容",
  "summary": "文章摘要",
  "status": "DRAFT",  // DRAFT | PUBLISHED
  "categoryId": 1,
  "tagIds": [1, 2]
}

Response: (返回创建的完整文章信息)
```

#### 更新博客（需要认证）
```http
PUT /api/posts/{id}
Authorization: Bearer <token>
Content-Type: application/json

Request: 同创建博客，所有字段可选
```

#### 删除博客（需要认证）
```http
DELETE /api/posts/{id}
Authorization: Bearer <token>

Response:
{
  "success": true,
  "message": "博客删除成功"
}
```

#### 发布/取消发布博客（需要认证）
```http
POST /api/posts/{id}/publish
POST /api/posts/{id}/unpublish
Authorization: Bearer <token>
```

#### 搜索博客（游客可访问）
```http
GET /api/posts/search?q=关键词&category=分类ID&tag=标签ID&page=0&size=10
Response: (分页格式，同列表接口)
```

### 3. CategoryController - 分类控制器

#### 获取分类列表（游客可访问）
```http
GET /api/categories
Query Params:
- includeCounts=true|false  // 是否附带文章数量统计，默认true
- parentId=number           // 仅返回指定父级的子分类

Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "技术",
      "slug": "tech",
      "description": "技术相关文章",
      "parentId": null,
      "postCount": 10,
      "createdAt": "2025-01-01T12:00:00"
    }
  ]
}
```

#### 获取分类详情（游客可访问）
```http
GET /api/categories/{id}
Response: (同单个分类格式)
```

#### 根据Slug获取分类（游客可访问）
```http
GET /api/categories/slug/{slug}
Response: (同单个分类格式，slug唯一)
```

#### 获取分类树（游客可访问）
```http
GET /api/categories/tree?includeEmpty=false
Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "技术",
      "slug": "tech",
      "children": [
        {
          "id": 2,
          "name": "Java",
          "slug": "java",
          "children": []
        }
      ]
    }
  ]
}
```

#### 获取分类下文章（游客可访问）
```http
GET /api/categories/{id}/posts?page=0&size=10&status=PUBLISHED
Response: (分页格式，content为文章列表)
```

#### 创建分类（需要认证）
```http
POST /api/categories
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "name": "新分类",
  "description": "分类描述",
  "parentId": 1,      // 可选，用于创建子分类
  "slug": "custom-slug"  // 可选，不传则根据名称生成
}

Response:
{
  "success": true,
  "data": {
    "id": 12,
    "name": "新分类",
    "slug": "xin-fen-lei",
    "description": "分类描述",
    "parentId": 1,
    "postCount": 0,
    "createdAt": "2025-01-01T12:00:00"
  }
}
```

#### 更新分类（需要认证）
```http
PUT /api/categories/{id}
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "name": "更新分类名",
  "description": "新的描述",
  "slug": "updated-slug",
  "parentId": null
}

Response: (同单个分类格式)
```

#### 删除分类（需要认证）
```http
DELETE /api/categories/{id}
Authorization: Bearer <token>
```

#### 批量调整分类顺序（需要认证）
```http
PATCH /api/categories/reorder
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "orders": [
    { "id": 1, "order": 1 },
    { "id": 2, "order": 2 }
  ]
}
```

#### 获取分类统计（需要认证）
```http
GET /api/categories/stats
Authorization: Bearer <token>

Response:
{
  "success": true,
  "data": {
    "totalCategories": 12,
    "topCategories": [
      {
        "id": 1,
        "name": "技术",
        "postCount": 34
      }
    ],
    "emptyCategories": 3
  }
}
```

> **验证规则**
- 分类名称、Slug唯一且长度 2-50 字符
- 不允许选择自身或子节点作为 `parentId`
- 删除分类前需确保无文章或启用级联转移策略

### 4. TagController - 标签控制器

#### 获取标签列表（游客可访问）
```http
GET /api/tags
Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Java",
      "slug": "java",
      "description": "Java相关",
      "postCount": 5,
      "createdAt": "2025-01-01T12:00:00"
    }
  ]
}
```

#### 热门标签（游客可访问）
```http
GET /api/tags/popular?limit=10
Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Java",
      "slug": "java",
      "postCount": 15
    }
  ]
}
```

#### 获取标签详情（游客可访问）
```http
GET /api/tags/{id}
Response: (同单个标签格式)
```

#### 根据Slug获取标签（游客可访问）
```http
GET /api/tags/slug/{slug}
Response: (同单个标签格式)
```

#### 标签模糊搜索（游客可访问）
```http
GET /api/tags/search?q=java&page=0&size=20
Response: (分页格式，content为标签列表)
```

#### 标签输入建议（游客可访问）
```http
GET /api/tags/suggestions?q=ja&limit=8
Response:
{
  "success": true,
  "data": [
    { "id": 1, "name": "Java" },
    { "id": 2, "name": "JavaScript" }
  ]
}
```

#### 创建标签（需要认证）
```http
POST /api/tags
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "name": "新标签",
  "description": "标签描述",
  "slug": "custom-slug"  // 可选
}

Response: (同单个标签格式)
```

#### 更新标签（需要认证）
```http
PUT /api/tags/{id}
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "name": "更新标签",
  "description": "新的描述",
  "slug": "new-slug"
}
```

#### 删除标签（需要认证）
```http
DELETE /api/tags/{id}
Authorization: Bearer <token>
```

#### 批量创建/绑定标签（需要认证）
```http
POST /api/tags/bulk
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "names": ["Java", "Spring", "Docker"]
}

Response:
{
  "success": true,
  "data": [
    { "id": 1, "name": "Java", "slug": "java" },
    { "id": 5, "name": "Docker", "slug": "docker" }
  ]
}
```

#### 获取标签下文章（游客可访问）
```http
GET /api/tags/{id}/posts?page=0&size=10&status=PUBLISHED
Response: (分页格式，content为文章列表)
```

> **标签规则**
- 标签名称与Slug唯一、长度 2-30 字符
- 热门标签按 `postCount` 降序缓存60秒
- 批量接口仅对新标签创建，已存在的标签直接返回

### 5. MediaController - 媒体文件控制器

#### 获取媒体列表（需要认证）
```http
GET /api/media?page=0&size=20
Authorization: Bearer <token>

Response:
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "fileName": "image.jpg",
        "originalName": "原始文件名.jpg",
        "mimeType": "image/jpeg",
        "size": 1024000,
        "url": "/uploads/image.jpg",
        "type": "IMAGE",
        "uploadedBy": {
          "id": 1,
          "username": "admin"
        },
        "uploadedAt": "2025-01-01T12:00:00"
      }
    ],
    "totalElements": 50,
    "totalPages": 3
  }
}
```

#### 上传文件（需要认证）
```http
POST /api/media/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

Request: file字段上传文件

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "fileName": "generated_uuid.jpg",
    "originalName": "原始文件名.jpg",
    "mimeType": "image/jpeg",
    "size": 1024000,
    "url": "/uploads/generated_uuid.jpg",
    "type": "IMAGE"
  }
}
```

#### 删除媒体（需要认证）
```http
DELETE /api/media/{id}
Authorization: Bearer <token>
```

### 6. SettingsController - 系统设置控制器

#### 获取系统设置（游客可访问）
```http
GET /api/settings/public
Response:
{
  "success": true,
  "data": {
    "blogName": "我的博客",
    "blogDescription": "个人技术博客",
    "authorName": "博主姓名",
    "authorEmail": "contact@blog.com",
    "pageSize": 10,
    "commentEnabled": true,
    "theme": "default"
  }
}
```

#### 获取所有设置（需要认证）
```http
GET /api/settings
Authorization: Bearer <token>
Response: (包含所有系统配置)
```

#### 更新设置（需要认证）
```http
PUT /api/settings
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "blogName": "新博客名",
  "blogDescription": "新博客描述",
  "authorName": "新作者名",
  "pageSize": 15
}
```

#### 更新头像（需要认证）
```http
POST /api/settings/avatar
Authorization: Bearer <token>
Content-Type: multipart/form-data

Request: avatar文件上传
```

### 7. ThemesController - 主题控制器

#### 获取主题列表（需要认证）
```http
GET /api/themes
Authorization: Bearer <token>

Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "default",
      "displayName": "默认主题",
      "description": "简洁的默认主题",
      "isActive": true,
      "config": {
        "primaryColor": "#007bff",
        "fontFamily": "Arial"
      }
    }
  ]
}
```

#### 切换主题（需要认证）
```http
POST /api/themes/{themeId}/activate
Authorization: Bearer <token>
```

#### 自定义主题配置（需要认证）
```http
PUT /api/themes/{themeId}/config
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "primaryColor": "#ff6b6b",
  "fontFamily": "Helvetica",
  "customCSS": "body { background: #f8f9fa; }"
}
```

## 统一响应格式

### 成功响应
```json
{
  "success": true,
  "data": {},  // 具体数据
  "message": "操作成功"  // 可选
}
```

### 错误响应
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "资源未找到"
  }
}
```

### 分页响应
```json
{
  "success": true,
  "data": {
    "content": [],  // 数据列表
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 0
  }
}
```

## 错误代码定义

| 错误代码 | HTTP状态码 | 说明 |
|---------|-----------|------|
| VALIDATION_ERROR | 400 | 请求参数验证失败 |
| UNAUTHORIZED | 401 | 未登录或Token无效 |
| FORBIDDEN | 403 | 权限不足 |
| RESOURCE_NOT_FOUND | 404 | 资源不存在 |
| DUPLICATE_RESOURCE | 409 | 资源已存在 |
| INTERNAL_ERROR | 500 | 服务器内部错误 |

## 权限控制

### 游客（未认证）
- `GET /api/posts/*` - 博客相关只读接口
- `GET /api/categories/*` - 分类查看
- `GET /api/tags/*` - 标签查看
- `GET /api/settings/public` - 公开设置查看

### 博主（已认证）
- 所有游客权限
- `POST /api/auth/*` - 认证相关
- `POST/PUT/DELETE /api/posts/*` - 博客管理
- `POST/PUT/DELETE /api/categories/*` - 分类管理
- `POST/PUT/DELETE /api/tags/*` - 标签管理
- `POST/GET/DELETE /api/media/*` - 媒体管理
- `PUT /api/settings/*` - 设置管理
- `GET/POST/PUT /api/themes/*` - 主题管理

## 实现注意事项

1. **JWT Token**: 有效期设置为24小时
2. **文件上传**: 限制文件类型（jpg, png, gif, webp）和大小（5MB）
3. **分页**: 默认每页10条记录，最大不超过100条
4. **搜索**: 支持标题、内容、作者、分类、标签组合搜索
5. **缓存**: 博客内容和设置可适当缓存
6. **日志**: 记录重要操作和错误信息
7. **验证**: 使用Spring Validation进行输入验证
8. **事务**: 涉及多表操作时使用@Transactional

---

**文档版本**: v1.0
**创建日期**: 2025-11-18
**最后更新**: 2025-11-18
