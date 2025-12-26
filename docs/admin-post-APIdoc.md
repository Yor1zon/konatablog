# 管理端API文档

## 概述

本文档描述了博客系统管理端专用的API接口。管理端API需要JWT认证，并可以访问所有文章（包括草稿状态的文章），而公共API只能访问已发布的文章。

## 认证要求

所有管理端API都需要在请求头中包含有效的JWT Token：

```
Authorization: Bearer <your-jwt-token>
```

## API端点对比

| 功能 | 公共API | 管理端API | 说明 |
|------|---------|-----------|------|
| 获取文章列表 | `GET /api/posts` | `GET /api/posts/admin/all` | 公共端只返回已发布，管理端返回所有 |
| 获取文章详情 | `GET /api/posts/{id}` | `GET /api/posts/admin/{id}` | 公共端只能访问已发布文章 |
| 根据Slug获取文章 | `GET /api/posts/slug/{slug}` | `GET /api/posts/admin/slug/{slug}` | 同上 |
| 搜索文章 | `GET /api/posts/search` | `GET /api/posts/admin/search` | 公共端只搜索已发布文章 |

## 管理端API详情

### 1. 获取所有文章列表

**端点**: `GET /api/posts/admin/all`

**描述**: 获取所有文章列表，包括草稿状态的文章。

**认证**: 需要管理员权限

**参数**:
- `page` (可选): 页码，默认0
- `size` (可选): 每页大小，默认10，最大100
- `sort` (可选): 排序字段和方向，默认 "createdAt,desc"

**请求示例**:
```bash
curl -X GET "http://localhost:8081/api/posts/admin/all?page=0&size=10&sort=createdAt,desc" \
     -H "Authorization: Bearer <token>"
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 6,
        "title": "这是一篇草稿文章",
        "slug": "draft-article",
        "content": "草稿内容...",
        "excerpt": "草稿测试",
        "status": "DRAFT",
        "isFeatured": false,
        "viewCount": 0,
        "publishedAt": null,
        "createdAt": "2025-01-01T10:00:00",
        "updatedAt": "2025-01-01T10:00:00",
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
        "tags": []
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
    "last": true,
    "totalPages": 1,
    "totalElements": 1,
    "first": true,
    "size": 10,
    "number": 0
  }
}
```

### 2. 获取文章详情（管理端）

**端点**: `GET /api/posts/admin/{id}`

**描述**: 根据ID获取文章详情，可以访问包括草稿状态在内的所有文章。

**认证**: 需要管理员权限

**路径参数**:
- `id`: 文章ID

**请求示例**:
```bash
curl -X GET "http://localhost:8081/api/posts/admin/6" \
     -H "Authorization: Bearer <token>"
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "id": 6,
    "title": "这是一篇草稿文章",
    "slug": "draft-article",
    "content": "完整的草稿内容...",
    "excerpt": "草稿测试",
    "status": "DRAFT",
    "isFeatured": false,
    "viewCount": 0,
    "publishedAt": null,
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00",
    "author": {
      "id": 1,
      "username": "admin",
      "nickname": "konatabloger",
      "email": "admin@blog.com"
    },
    "category": {
      "id": 1,
      "name": "技术",
      "slug": "tech"
    },
    "tags": []
  }
}
```

### 3. 根据Slug获取文章详情（管理端）

**端点**: `GET /api/posts/admin/slug/{slug}`

**描述**: 根据Slug获取文章详情，可以访问包括草稿状态在内的所有文章。

**认证**: 需要管理员权限

**路径参数**:
- `slug`: 文章Slug

**请求示例**:
```bash
curl -X GET "http://localhost:8081/api/posts/admin/slug/draft-article" \
     -H "Authorization: Bearer <token>"
```

### 4. 搜索所有文章（管理端）

**端点**: `GET /api/posts/admin/search`

**描述**: 搜索所有文章，包括草稿状态的文章。

**认证**: 需要管理员权限

**参数**:
- `q` (可选): 搜索关键词
- `category` (可选): 分类ID
- `tag` (可选): 标签ID
- `page` (可选): 页码，默认0
- `size` (可选): 每页大小，默认10
- `sort` (可选): 排序字段和方向，默认 "createdAt,desc"

**请求示例**:
```bash
curl -X GET "http://localhost:8081/api/posts/admin/search?q=测试&page=0&size=10" \
     -H "Authorization: Bearer <token>"
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 6,
        "title": "这是一篇草稿文章",
        "slug": "draft-article",
        "content": "包含测试关键词的草稿内容...",
        "excerpt": "草稿测试",
        "status": "DRAFT",
        "isFeatured": false,
        "viewCount": 0,
        "publishedAt": null,
        "createdAt": "2025-01-01T10:00:00",
        "updatedAt": "2025-01-01T10:00:00",
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
        "tags": []
      }
    ],
    "pageable": { /* 分页信息 */ },
    "last": true,
    "totalPages": 1,
    "totalElements": 1
  }
}
```

## 错误处理

### 未认证错误 (401)
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

### 未找到资源错误 (404)
```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "POST_NOT_FOUND",
    "message": "Post not found with id: 999"
  }
}
```

## 重要特性

### 1. 浏览量统计
- **公共端访问**: 会增加文章浏览量
- **管理端访问**: **不会**增加文章浏览量，避免管理员编辑时影响统计数据

### 2. 权限控制
- 所有管理端API都需要有效的JWT Token
- Token中必须包含有效用户ID，否则返回401错误
- 暂时基于用户认证，未来可扩展角色权限控制

### 3. 数据完整性
- 管理端可以看到完整的文章状态信息
- 包含草稿(DRAFT)、已发布(PUBLISHED)等所有状态
- 返回所有字段，包括内部管理使用的属性

## 前端开发建议

### 1. 认证处理
```javascript
// 设置请求拦截器
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token');
  if (token && config.url.startsWith('/api/posts/admin/')) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, error => {
  return Promise.reject(error);
});
```

### 2. 状态显示
```javascript
// 状态映射
const statusMap = {
  'DRAFT': { text: '草稿', color: 'warning' },
  'PUBLISHED': { text: '已发布', color: 'success' }
};

// 在组件中使用
<PostStatus status={post.status} />
```

### 3. 页面路由建议
- `/admin/posts` - 管理端文章列表页面
- `/admin/posts/{id}` - 管理端文章编辑页面
- `/posts` - 公共文章列表页面
- `/posts/{id}` - 公共文章详情页面

## 注意事项

1. **Token过期**: JWT Token有过期时间（默认24小时），需要处理过期刷新逻辑
2. **权限检查**: 前端也需要进行基本的权限检查，配合后端认证
3. **错误处理**: 建议统一处理API错误，提供友好的用户提示
4. **性能考虑**: 大量文章时考虑使用分页和懒加载
5. **缓存策略**: 管理端数据一般不缓存，确保数据实时性

---

**更新日期**: 2025-01-24
**版本**: v1.0
**维护者**: KANA Blog Team