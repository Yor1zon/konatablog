# KONATABLOG RESTful API 文档

## 概述

KONATABLOG 是一个个人博客系统的后端API，基于 Spring Boot 3.5.7 开发，提供博客文章、分类、标签、媒体文件、用户认证等完整的REST API接口。

**基础信息**
- **基础URL**: `http://localhost:8081/api`
- **版本**: v1.0
- **字符编码**: UTF-8
- **内容类型**: application/json
- **跨域支持**: 已启用（CORS）

---

## 认证机制

### JWT Token 认证

所有需要认证的接口都使用JWT（JSON Web Token）进行身份验证。

**请求头配置:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Token获取**: 通过登录接口 `/api/auth/login` 获取

**Token有效期**: 24小时（可通过 `/api/auth/refresh` 刷新）

---

## 通用响应格式

所有API接口都使用统一的 `CommonResponse` 响应格式：

```json
{
  "success": true,
  "data": {},
  "message": "操作成功",
  "error": null
}
```

**成功响应示例:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "博客标题"
  },
  "message": "获取成功",
  "error": null
}
```

**错误响应示例:**
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

---

## 认证模块 API

### 1. 用户登录

**POST** `/api/auth/login`

登录获取JWT Token。

**请求体:**
```json
{
  "username": "admin",
  "password": "password123"
}
```

**响应示例:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@blog.com",
      "nickname": "管理员",
      "role": "ADMIN",
      "avatar": "http://localhost:8081/uploads/avatars/avatar.jpg",
      "isActive": true
    }
  },
  "message": "登录成功"
}
```

### 2. 获取用户信息

**GET** `/api/auth/profile`

获取当前登录用户的详细信息。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**响应示例:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@blog.com",
    "nickname": "管理员",
    "role": "ADMIN",
    "avatar": "http://localhost:8081/uploads/avatars/avatar.jpg",
    "isActive": true,
    "lastLoginAt": "2024-01-15T10:30:00",
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

### 3. 用户登出

**POST** `/api/auth/logout`

用户登出（客户端需删除Token）。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**响应示例:**
```json
{
  "success": true,
  "message": "登出成功"
}
```

### 4. 验证Token

**GET** `/api/auth/validate`

验证Token的有效性。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**响应示例:**
```json
{
  "success": true,
  "data": true,
  "message": "Token有效"
}
```

### 5. 刷新Token

**POST** `/api/auth/refresh`

刷新即将过期的Token。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**响应示例:**
```json
{
  "success": true,
  "data": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Token刷新成功"
}
```

---

## 博客文章模块 API

### 1. 获取文章列表

**GET** `/api/posts`

分页获取博客文章列表。

**查询参数:**
- `page` (int): 页码，默认 0
- `size` (int): 每页数量，默认 10，最大 100
- `sort` (string): 排序规则，默认 "createdAt,desc"

**请求示例:**
```
GET /api/posts?page=0&size=10&sort=createdAt,desc
```

**响应示例:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "第一篇博客",
        "slug": "first-blog",
        "excerpt": "这是文章摘要...",
        "content": "这是完整的文章内容...",
        "status": "PUBLISHED",
        "isFeatured": false,
        "viewCount": 150,
        "createdAt": "2024-01-15T10:30:00",
        "publishedAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00",
        "author": {
          "id": 1,
          "username": "admin",
          "displayName": "管理员"
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
    "pageable": {},
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### 2. 获取文章详情

**GET** `/api/posts/{id}`

根据ID获取文章详情。

**路径参数:**
- `id` (long): 文章ID

**响应示例:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "第一篇博客",
    "slug": "first-blog",
    "excerpt": "这是文章摘要...",
    "content": "这是完整的文章内容...",
    "status": "PUBLISHED",
    "isFeatured": false,
    "viewCount": 151,
    "createdAt": "2024-01-15T10:30:00",
    "publishedAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00",
    "author": {
      "id": 1,
      "username": "admin",
      "displayName": "管理员"
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
}
```

### 3. 根据Slug获取文章

**GET** `/api/posts/slug/{slug}`

根据Slug获取文章详情。

**路径参数:**
- `slug` (string): 文章Slug

### 4. 搜索文章

**GET** `/api/posts/search`

搜索博客文章。

**查询参数:**
- `q` (string): 搜索关键词
- `category` (long): 分类ID
- `tag` (long): 标签ID
- `page` (int): 页码，默认 0
- `size` (int): 每页数量，默认 10
- `sort` (string): 排序规则，默认 "publishedAt,desc"

**请求示例:**
```
GET /api/posts/search?q=Java&category=1&page=0&size=10
```

### 5. 创建文章

**POST** `/api/posts`

创建新的博客文章（需要认证）。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**请求体:**
```json
{
  "title": "新文章标题",
  "content": "文章内容...",
  "excerpt": "文章摘要",
  "slug": "new-article",
  "status": "DRAFT",
  "isFeatured": false,
  "categoryId": 1,
  "tagIds": [1, 2]
}
```

**响应示例:**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "title": "新文章标题",
    "slug": "new-article",
    // ... 其他字段
  },
  "message": "博客创建成功"
}
```

### 6. 更新文章

**PUT** `/api/posts/{id}`

更新现有文章（需要认证）。

**路径参数:**
- `id` (long): 文章ID

**请求体:**
```json
{
  "title": "更新后的标题",
  "content": "更新后的内容...",
  "excerpt": "更新后的摘要",
  "slug": "updated-article",
  "status": "PUBLISHED",
  "isFeatured": true,
  "categoryId": 2,
  "tagIds": [2, 3]
}
```

### 7. 删除文章

**DELETE** `/api/posts/{id}`

删除文章（需要认证）。

**路径参数:**
- `id` (long): 文章ID

**响应示例:**
```json
{
  "success": true,
  "message": "博客删除成功"
}
```

### 8. 发布文章

**POST** `/api/posts/{id}/publish`

发布草稿文章（需要认证）。

**路径参数:**
- `id` (long): 文章ID

### 9. 取消发布文章

**POST** `/api/posts/{id}/unpublish`

将已发布文章转为草稿（需要认证）。

**路径参数:**
- `id` (long): 文章ID

---

## 分类模块 API

### 1. 获取分类列表

**GET** `/api/categories`

获取所有分类列表。

**查询参数:**
- `includeCounts` (boolean): 是否包含文章数量统计，默认 true
- `parentId` (long): 父分类ID，获取子分类时使用

**响应示例:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "技术",
      "slug": "tech",
      "description": "技术相关文章",
      "parentId": null,
      "parentName": null,
      "sortOrder": 1,
      "isActive": true,
      "postCount": 15,
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

### 2. 获取分类详情

**GET** `/api/categories/{id}`

根据ID获取分类详情。

### 3. 根据Slug获取分类

**GET** `/api/categories/slug/{slug}`

根据Slug获取分类详情。

### 4. 获取分类树

**GET** `/api/categories/tree`

获取层级分类树结构。

**查询参数:**
- `includeEmpty` (boolean): 是否包含空分类，默认 false

**响应示例:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "技术",
      "slug": "tech",
      "parentId": null,
      "postCount": 15,
      "children": [
        {
          "id": 2,
          "name": "Java",
          "slug": "java",
          "parentId": 1,
          "postCount": 8,
          "children": []
        }
      ]
    }
  ]
}
```

### 5. 获取分类下的文章

**GET** `/api/categories/{id}/posts`

获取指定分类下的文章列表。

**路径参数:**
- `id` (long): 分类ID

**查询参数:**
- `page` (int): 页码，默认 0
- `size` (int): 每页数量，默认 10
- `sort` (string): 排序规则，默认 "publishedAt,desc"
- `status` (string): 文章状态过滤（PUBLISHED/DRAFT）

### 6. 创建分类

**POST** `/api/categories`

创建新分类（需要认证）。

**请求体:**
```json
{
  "name": "新分类",
  "description": "分类描述",
  "slug": "new-category",
  "sortOrder": 10,
  "isActive": true,
  "parentId": 1
}
```

### 7. 更新分类

**PUT** `/api/categories/{id}`

更新分类信息（需要认证）。

**路径参数:**
- `id` (long): 分类ID

**请求体:**
```json
{
  "name": "更新后的分类名",
  "description": "更新后的描述",
  "slug": "updated-category",
  "sortOrder": 15,
  "isActive": true,
  "parentId": null
}
```

### 8. 删除分类

**DELETE** `/api/categories/{id}`

删除分类（需要认证）。

**路径参数:**
- `id` (long): 分类ID

### 9. 调整分类顺序

**PATCH** `/api/categories/reorder`

批量调整分类顺序（需要认证）。

**请求体:**
```json
{
  "orders": [
    {
      "id": 1,
      "order": 1
    },
    {
      "id": 2,
      "order": 2
    }
  ]
}
```

### 10. 分类统计

**GET** `/api/categories/stats`

获取分类统计信息（需要认证）。

**响应示例:**
```json
{
  "success": true,
  "data": {
    "totalCategories": 10,
    "categoriesWithPosts": 8,
    "emptyCategories": 2,
    "topCategories": [
      {
        "id": 1,
        "name": "技术",
        "postCount": 15
      }
    ]
  }
}
```

---

## 标签模块 API

### 1. 获取标签列表

**GET** `/api/tags`

获取所有标签列表。

**响应示例:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Java",
      "slug": "java",
      "description": "Java相关内容",
      "usageCount": 8,
      "postCount": 8,
      "color": "#FF5722",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

### 2. 获取标签详情

**GET** `/api/tags/{id}`

根据ID获取标签详情。

### 3. 根据Slug获取标签

**GET** `/api/tags/slug/{slug}`

根据Slug获取标签详情。

### 4. 获取热门标签

**GET** `/api/tags/popular`

获取热门标签列表。

**查询参数:**
- `limit` (int): 返回数量，默认 10，最大 50

### 5. 搜索标签

**GET** `/api/tags/search`

模糊搜索标签。

**查询参数:**
- `q` (string): 搜索关键词
- `page` (int): 页码，默认 0
- `size` (int): 每页数量，默认 20

### 6. 标签建议

**GET** `/api/tags/suggestions`

根据输入获取标签建议。

**查询参数:**
- `q` (string): 输入关键词
- `limit` (int): 返回数量，默认 8，最大 20

### 7. 获取标签下的文章

**GET** `/api/tags/{id}/posts`

获取指定标签下的文章列表。

**路径参数:**
- `id` (long): 标签ID

**查询参数:**
- `page` (int): 页码，默认 0
- `size` (int): 每页数量，默认 10
- `sort` (string): 排序规则，默认 "publishedAt,desc"
- `status` (string): 文章状态过滤

### 8. 创建标签

**POST** `/api/tags`

创建新标签（需要认证）。

**请求体:**
```json
{
  "name": "新标签",
  "slug": "new-tag",
  "description": "标签描述",
  "color": "#2196F3"
}
```

### 9. 更新标签

**PUT** `/api/tags/{id}`

更新标签信息（需要认证）。

**路径参数:**
- `id` (long): 标签ID

**请求体:**
```json
{
  "name": "更新后的标签",
  "slug": "updated-tag",
  "description": "更新后的描述",
  "color": "#4CAF50"
}
```

### 10. 删除标签

**DELETE** `/api/tags/{id}`

删除标签（需要认证）。

**路径参数:**
- `id` (long): 标签ID

**查询参数:**
- `force` (boolean): 是否强制删除（会清除关联关系），默认 false

### 11. 批量创建标签

**POST** `/api/tags/bulk`

批量创建或绑定标签（需要认证）。

**请求体:**
```json
{
  "names": ["前端", "后端", "数据库"]
}
```

---

## 媒体文件模块 API

### 1. 获取媒体列表

**GET** `/api/media`

获取媒体文件列表（需要认证）。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**查询参数:**
- `page` (int): 页码，默认 0
- `size` (int): 每页数量，默认 20
- `sort` (string): 排序规则，默认 "createdAt,desc"
- `type` (string): 媒体类型过滤（IMAGE/VIDEO/AUDIO/AVATAR）
- `uploadedBy` (long): 上传者ID过滤

**响应示例:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "originalName": "image.jpg",
        "fileName": "a1b2c3d4_image.jpg",
        "fileExtension": "jpg",
        "fileSize": 1024000,
        "mimeType": "image/jpeg",
        "url": "http://localhost:8081/uploads/media/a1b2c3d4_image.jpg",
        "localPath": "/uploads/media/a1b2c3d4_image.jpg",
        "type": "IMAGE",
        "width": 1920,
        "height": 1080,
        "description": "图片描述",
        "altText": "替代文字",
        "uploadedAt": "2024-01-15T10:30:00",
        "uploadedBy": {
          "id": 1,
          "username": "admin",
          "displayName": "管理员"
        }
      }
    ],
    "pageable": {},
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 2. 上传媒体文件

**POST** `/api/media/upload`

上传媒体文件（需要认证）。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: multipart/form-data
```

**请求参数:**
- `file` (MultipartFile): 上传的文件（必填）
- `description` (string): 文件描述（可选）
- `altText` (string): 图片替代文字（可选）
- `type` (string): 媒体类型（可选，自动检测）

**限制条件:**
- 最大文件大小：5MB
- 支持的图片格式：jpg, jpeg, png, gif, webp
- 支持的视频格式：mp4
- 支持的音频格式：mp3

**响应示例:**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "originalName": "photo.jpg",
    "fileName": "e5f6g7h8_photo.jpg",
    // ... 其他字段
  },
  "message": "文件上传成功"
}
```

### 3. 删除媒体文件

**DELETE** `/api/media/{id}`

删除媒体文件（需要认证）。

**路径参数:**
- `id` (long): 媒体文件ID

---

## 系统设置模块 API

### 1. 获取公开设置

**GET** `/api/settings/public`

获取博客的公开配置信息（无需认证）。

**响应示例:**
```json
{
  "success": true,
  "data": {
    "blogName": "我的博客",
    "blogDescription": "这是一个技术博客",
    "blogTagline": "分享技术与生活",
    "authorName": "博主",
    "authorEmail": "contact@blog.com",
    "pageSize": 10,
    "commentEnabled": true,
    "theme": "default"
  }
}
```

### 2. 获取所有设置

**GET** `/api/settings`

获取所有系统设置（需要认证）。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**响应示例:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "key": "SITE_TITLE",
      "value": "我的博客",
      "group": "site",
      "description": "网站标题",
      "optionType": "TEXT",
      "updatedBy": "admin",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ]
}
```

### 3. 更新系统设置

**PUT** `/api/settings`

批量更新系统设置（需要认证）。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**请求体:**
```json
{
  "blogName": "新博客名称",
  "blogDescription": "更新后的描述",
  "blogTagline": "新的标语",
  "authorName": "作者名",
  "authorEmail": "author@blog.com",
  "pageSize": 15,
  "commentEnabled": false,
  "theme": "new-theme"
}
```

### 4. 上传头像

**POST** `/api/settings/avatar`

上传用户头像（需要认证）。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: multipart/form-data
```

**请求参数:**
- `avatar` (MultipartFile): 头像文件

**限制条件:**
- 最大文件大小：5MB
- 支持的格式：jpg, jpeg, png, gif

**响应示例:**
```json
{
  "success": true,
  "data": {
    "avatarUrl": "http://localhost:8081/uploads/avatars/avatar123.jpg"
  },
  "message": "头像上传成功"
}
```

---

## 主题模块 API

### 1. 获取主题列表

**GET** `/api/themes`

获取所有可用主题（需要认证）。

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**响应示例:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "默认主题",
      "slug": "default",
      "description": "简洁的默认主题",
      "version": "1.0.0",
      "author": "KONATABLOG",
      "previewUrl": "http://localhost:8081/themes/default/preview.jpg",
      "active": true,
      "isDefault": true,
      "config": {
        "primaryColor": "#1976D2",
        "fontFamily": "Arial"
      },
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

### 2. 激活主题

**POST** `/api/themes/{themeId}/activate`

激活指定主题（需要认证）。

**路径参数:**
- `themeId` (long): 主题ID

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**响应示例:**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "name": "新主题",
    "slug": "new-theme",
    "active": true
  },
  "message": "主题已启用"
}
```

### 3. 更新主题配置

**PUT** `/api/themes/{themeId}/config`

更新主题的自定义配置（需要认证）。

**路径参数:**
- `themeId` (long): 主题ID

**请求头:**
```
Authorization: Bearer <JWT_TOKEN>
```

**请求体:**
```json
{
  "config": {
    "primaryColor": "#FF5722",
    "fontFamily": "Roboto",
    "showSidebar": true,
    "postsPerPage": 12
  }
}
```

---

## 错误代码说明

| HTTP状态码 | 错误代码 | 说明 |
|-----------|---------|------|
| 400 | VALIDATION_ERROR | 请求参数验证失败 |
| 401 | UNAUTHORIZED | 未认证或Token无效/过期 |
| 401 | INVALID_CREDENTIALS | 用户名或密码错误 |
| 401 | USER_INACTIVE | 用户账户已被禁用 |
| 403 | FORBIDDEN | 权限不足 |
| 404 | NOT_FOUND | 资源不存在 |
| 404 | POST_NOT_FOUND | 文章不存在 |
| 404 | CATEGORY_NOT_FOUND | 分类不存在 |
| 404 | TAG_NOT_FOUND | 标签不存在 |
| 404 | MEDIA_NOT_FOUND | 媒体文件不存在 |
| 404 | THEME_NOT_FOUND | 主题不存在 |
| 404 | RESOURCE_NOT_FOUND | 通用资源不存在 |
| 409 | CONFLICT | 资源冲突 |
| 409 | DUPLICATE_RESOURCE | 资源重复（如用户名、邮箱已存在） |
| 409 | CATEGORY_IN_USE | 分类正在被使用（无法删除）
| 409 | TAG_IN_USE | 标签正在被使用（无法删除） |
| 500 | INTERNAL_ERROR | 服务器内部错误 |
| 500 | STORAGE_ERROR | 文件存储相关错误 |

---

## 分页参数说明

所有支持分页的接口都使用标准的Spring Data分页格式：

**查询参数:**
- `page` (int): 页码，从0开始
- `size` (int): 每页数量
- `sort` (string): 排序规则，格式为 "字段名,方向"

**排序格式:**
- `createdAt,desc` - 按创建时间降序
- `createdAt,asc` - 按创建时间升序
- `title,asc` - 按标题升序
- `viewCount,desc` - 按浏览量降序

**分页响应结构:**
```json
{
  "content": [], // 当前页数据
  "pageable": {},
  "totalElements": 100, // 总记录数
  "totalPages": 10, // 总页数
  "first": true, // 是否首页
  "last": false, // 是否末页
  "size": 10, // 每页大小
  "number": 0 // 当前页码
}
```

---

## 常见问题

### 1. 如何处理Token过期？

当Token过期时，接口会返回401状态码和 `UNAUTHORIZED` 错误。客户端应该：
- 清除本地存储的Token
- 重定向到登录页面
- 或者尝试调用 `/api/auth/refresh` 刷新Token

### 2. 文件上传失败怎么办？

检查文件是否符合以下要求：
- 文件大小不超过限制
- 文件格式被支持
- 网络连接正常
- 服务器磁盘空间充足

### 3. 如何处理并发更新？

服务端使用数据库乐观锁机制来处理并发更新。如果检测到并发冲突，会返回409状态码。

### 4. 跨域问题如何解决？

API已启用CORS支持，允许来自任何源的跨域请求。生产环境建议配置具体的源域名。

---

## 开发建议

### 1. 前端集成

- 在所有需要认证的请求中添加 `Authorization` 头
- 实现Token过期自动刷新机制
- 使用统一的HTTP响应拦截器处理成功和错误响应
- 实现请求重试机制（特别是网络错误时）

### 2. 错误处理

- 根据HTTP状态码和错误代码进行国际化错误提示
- 网络超时时间建议设置为30秒
- 实现网络状态检测和离线提示

### 3. 性能优化

- 使用分页加载避免一次性加载大量数据
- 实现客户端缓存机制（特别是配置数据）
- 图片使用懒加载，并实现缩略图功能

### 4. 安全建议

- 不要在前端存储敏感信息
- 定期轮换JWT Secret
- 实现CSRF保护机制
- 对用户输入进行前端验证和清理

---

## 更新日志

### v1.0.0 (2024-01-15)
- 完整的用户认证系统
- 博客文章CRUD操作
- 分类和标签管理
- 媒体文件上传
- 系统配置管理
- 主题系统基础功能

---

*本文档最后更新时间：2024-01-15*