# AuthController 使用说明

## 概述

AuthController是KONATABLOG博客系统的认证控制器，实现了基于JWT的用户登录、登出和用户信息管理功能。

## 🔧 核心组件

### 1. JWT工具类 (`JwtTokenUtil`)
- **功能**：JWT Token生成、解析、验证
- **配置**：通过`application.properties`配置
  - `app.jwt.secret`: JWT秘钥（生产环境必须修改）
  - `app.jwt.expiration`: Token有效期（默认24小时）

### 2. 认证DTO类
- `LoginRequest`: 登录请求（用户名/邮箱+密码）
- `LoginResponse`: 登录响应（JWT Token + 用户信息）
- `UserProfileResponse`: 用户信息响应
- `CommonResponse<T>`: 统一API响应格式

### 3. 安全过滤器 (`JwtAuthenticationFilter`)
- 自动从HTTP Header中提取JWT Token
- 验证Token有效性并设置用户认证上下文
- 跳过公共接口的认证

## 🔐 API 接口详解

### 登录接口
```http
POST /api/auth/login
Content-Type: application/json

Request:
{
  "username": "admin",           // 可以是用户名或邮箱
  "password": "password123"
}

Response (200 OK):
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@blog.com",
      "nickname": "博主昵称",
      "role": "ADMIN",
      "avatar": null,
      "isActive": true
    }
  },
  "message": "登录成功"
}
```

### 获取用户信息
```http
GET /api/auth/profile
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@blog.com",
    "nickname": "博主昵称",
    "role": "ADMIN",
    "avatar": null,
    "isActive": true,
    "lastLoginAt": "2025-11-18T16:30:00",
    "createdAt": "2025-11-10T10:00:00"
  }
}
```

### 登出接口
```http
POST /api/auth/logout
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "登出成功"
}
```

### 验证Token
```http
GET /api/auth/validate
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "data": true,        // 或 false
  "message": "Token有效"
}
```

### 刷新Token
```http
POST /api/auth/refresh
Authorization: Bearer <old_token>

Response (200 OK):
{
  "success": true,
  "data": "eyJhbGciOiJIUzUxMiJ9...",
  "message": "Token刷新成功"
}
```

## 🛡️ 权限控制

### 公共接口（无需认证）
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/validate` - Token验证
- `GET /api/posts/**` - 博客文章浏览
- `GET /api/categories/**` - 分类浏览
- `GET /api/tags/**` - 标签浏览
- `GET /api/settings/public` - 公开设置

### 需认证接口
- `GET /api/auth/profile` - 获取用户信息
- `POST /api/auth/logout` - 用户登出
- `POST /api/auth/refresh` - 刷新Token
- `POST /api/media/**` - 媒体文件管理
- `PUT /api/settings/**` - 系统设置
- `POST /api/themes/**` - 主题管理

## 🔑 认证流程

1. **用户登录**
   - 用户使用用户名/邮箱和密码登录
   - 系统验证凭据并检查用户状态
   - 生成JWT Token并返回用户信息

2. **API调用**
   - 客户端在所有需要认证的请求中添加Header：
     ```
     Authorization: Bearer <jwt_token>
     ```
   - JWT过滤器自动验证Token并设置认证上下文

3. **Token管理**
   - Token有效期24小时（可配置）
   - 支持Token刷新机制
   - 客户端应在Token过期前主动刷新

## ⚠️ 错误处理

### 常见错误响应

**认证失败（401）**
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "未认证或Token已过期"
  }
}
```

**权限不足（403）**
```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "权限不足"
  }
}
```

**登录失败（401）**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "用户名或密码错误"
  }
}
```

**账户禁用（403）**
```json
{
  "success": false,
  "error": {
    "code": "USER_INACTIVE",
    "message": "用户账户已被禁用"
  }
}
```

## 🔧 配置说明

### JWT配置 (`application.properties`)
```properties
# JWT秘钥 - 生产环境必须使用强随机字符串！
app.jwt.secret=konatablog-jwt-secret-key-for-production-environment-change-this-string

# Token有效期（秒）
app.jwt.expiration=86400  # 24小时

# 文件上传限制
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

### 安全配置 (`SecurityConfig`)
- 使用BCrypt密码加密（强度12）
- 无状态会话管理
- CORS跨域支持
- 自定义异常处理

## 📋 使用示例

### 前端集成示例（JavaScript）
```javascript
class AuthAPI {
  constructor() {
    this.baseURL = 'http://localhost:8081/api';
    this.token = localStorage.getItem('jwt_token');
  }

  async login(username, password) {
    const response = await fetch(`${this.baseURL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password })
    });

    const result = await response.json();
    if (result.success) {
      this.token = result.data.token;
      localStorage.setItem('jwt_token', this.token);
    }
    return result;
  }

  async getProfile() {
    const response = await fetch(`${this.baseURL}/auth/profile`, {
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });
    return await response.json();
  }

  async logout() {
    const response = await fetch(`${this.baseURL}/auth/logout`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });
    localStorage.removeItem('jwt_token');
    this.token = null;
    return await response.json();
  }
}

// 使用示例
const authAPI = new AuthAPI();

// 登录
const loginResult = await authAPI.login('admin', 'password123');
if (loginResult.success) {
  console.log('登录成功', loginResult.data.user);
}

// 获取用户信息
const profile = await authAPI.getProfile();
console.log('用户信息', profile.data);
```

## 🚀 部署注意事项

1. **JWT秘钥安全**：生产环境必须使用强随机字符串，建议至少32字符
2. **HTTPS部署**：生产环境必须使用HTTPS传输JWT Token
3. **Token存储**：前端应使用localStorage或httpOnly cookie存储Token
4. **日志监控**：监控登录失败和异常Token尝试
5. **定期轮换**：考虑实现JWT秘钥定期轮换机制

## 🔄 扩展功能

### 待实现的功能
1. **Token黑名单**：使用Redis实现Token注销机制
2. **多设备管理**：支持同一账号多设备登录管理
3. **登录限制**：防止暴力破解的登录尝试限制
4. **双因子认证**：可选的2FA支持
5. **OAuth集成**：支持第三方登录（Google、GitHub等）

---

**文档版本**: v1.0
**创建日期**: 2025-11-18
**最后更新**: 2025-11-18