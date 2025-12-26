
### v2.0 (2025-12-27) - 增强功能

**新增功能**:
- ✅ `/api/users/me` - 支持用户信息更新（用户名、邮箱、昵称、密码）
- ✅ 密码修改功能（密码+确认密码验证）
- ✅ 昵称更新支持
- ✅ 分步验证和冲突检查

---

## 接口详情

### 更新用户信息 - `/api/users/me`

更新当前认证用户的用户信息，包括用户名、邮箱、昵称，以及可选的密码修改。

#### 请求

```
PUT /api/users/me
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

#### 请求头

| 字段名 | 必填 | 说明 | 示例 |
|--------|------|------|------|
| Authorization | ✅ 是 | JWT Token | `Bearer eyJhbGciOiJIUzUxMiJ9...` |
| Content-Type | ✅ 是 | application/json | - |

#### 请求体

```json
{
  "username": "newusername",
  "email": "newemail@example.com",
  "nickname": "新的昵称",
  "password": "newpassword",
  "confirmPassword": "newpassword"
}
```

#### 字段说明

| 字段名 | 类型 | 必填 | 说明 | 限制 | 备注 |
|--------|------|------|------|------|------|
| username | String | ❌ 否 | 用户名，可选 | 1-50字符 | 用于登录和显示 |
| email | String | ❌ 否 | 邮箱地址，可选 | 有效邮箱格式，最大150字符 | 用于账号关联
| nickname | String | ❌ 否 | 昵称（显示名称），可选 | 最大100字符 | 用于在前端展示
| password | String | ❌ 否 | 新密码，可选 | 至少6位 | ⚠️ 如提供则必须同时提供confirmPassword
| confirmPassword | String | ❌ 否 | 确认新密码 | 必须与password相同 | ⚠️ password存在时必须提供

> **重要提示**: 至少需要提供一个字段进行更新（用户名字段、邮箱、昵称或密码其中的任意一个）

#### 请求示例

**1. 更新用户名和邮箱**
```bash
curl -X PUT "http://localhost:8081/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newadmin",
    "email": "newadmin@blog.com"
  }'
```

**2. 更新昵称**
```bash
curl -X PUT "http://localhost:8081/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "超级管理员"
  }'
```

**3. 修改密码**
```bash
curl -X PUT "http://localhost:8081/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "password": "newpassword123",
    "confirmPassword": "newpassword123"
  }'
```

**4. 同时更新所有信息**
```bash
curl -X PUT "http://localhost:8081/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "username": "updatedadmin",
    "email": "admin2025@blog.com",
    "nickname": "管理员",
    "password": "newpassword123",
    "confirmPassword": "newpassword123"
  }'
```

#### 成功响应

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "newadmin",
    "email": "admin2025@blog.com",
    "nickname": "超级管理员",
    "role": "ADMIN",
    "avatar": null,
    "isActive": true,
    "lastLoginAt": "2025-12-27T00:30:45",
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
| nickname | String | 昵称（显示名称） |
| role | String | 用户角色（ADMIN/USER等）|
| avatar | String | 头像URL（如果有）|
| isActive | Boolean | 是否激活 |
| lastLoginAt | LocalDateTime | 最后登录时间 |
| createdAt | LocalDateTime | 创建时间 |

#### 错误响应

**1. 未提供任何更新字段 (400 Bad Request)**

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "至少需要提供一个要更新的字段（用户名、邮箱、昵称或密码）"
  }
}
```

**2. 密码和确认密码不匹配 (400 Bad Request)**

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "确认密码与新密码不匹配"
  }
}
```

**3. 用户名已存在 (409 Conflict)**

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "DUPLICATE_RESOURCE",
    "message": "Username already exists: newadmin"
  }
}
```

**4. 邮箱已存在 (409 Conflict)**

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "DUPLICATE_RESOURCE",
    "message": "Email already exists: admin2025@blog.com"
  }
}
```

**5. 用户名格式错误 (400 Bad Request)**

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "用户名长度需在1-50之间"
  }
}
```

**6. 邮箱格式错误 (400 Bad Request)**

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

**7. 密码长度不够 (400 Bad Request)**

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "密码长度不能少于6位"
  }
}
```

**8. 认证失败 (401 Unauthorized)**

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

**9. 对话不存在或已登出 (404 Not Found)**

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "User not found with id: 12345"
  }
}
```

---

## 🔐 完整认证流程

### 1. 获取Token

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username":"admin",
    "password":"123456"
  }'
```

**响应**:
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

### 2. 使用Token更新用户信息

```bash
curl -X PUT http://localhost:8081/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "新昵称",
    "password": "newpassword123",
    "confirmPassword": "newpassword123"
  }'
```

---

## 📊 后端实现架构

### 服务层（UserService）

#### 已支持的方法

1. **updateUser(Long id, User userData)** - 更新用户基础信息
   - ⚠️ 支持字段：username, displayName, email, avatarUrl, bio
   - ✅ 自动检查用户名、邮箱重复
   - ✅ 安全：只更新提供的字段

2. **updatePassword(Long id, String newPassword)** - 修改密码
   - ✅ 密码长度验证（至少6位）
   - ✅ BCrypt加密存储
   - ✅ 日志记录

### 控制器层（UserController）

#### 新增特性

1. **分步处理**
   - 先验证更新字段
   - 后验证密码一致性
   - 再调用服务层操作

2. **灵活更新**
   - 可以只更新其中任何一个字段（用户名、邮箱、昵称或密码）
   - 支持同时更新多个字段

3. **完整的错误处理**
   - 参数验证错误（406 Bad Request）
   - 资源重复冲突（409 Conflict）
   - 认证失败（401 Unauthorized）
   - 用户不存在（404 Not Found）

---

## 🚀 使用最佳实践

### 1. 前端集成建议

**分步骤更新**:
```javascript
// 示例：vue/react 组件
async updateUserProfile(userData) {
  try {
    // 1. 检查必填项
    if (!userData.username && !userData.email && !userData.nickname && !userData.password) {
      alert('请至少提供一个要更新的字段');
      return;
    }

    // 2. 检查密码一致性（如果提供密码）
    if (userData.password && userData.password !== userData.confirmPassword) {
      alert('确认密码与新密码不匹配');
      return;
    }

    // 3. 发送请求
    const response = await fetch('/api/users/me', {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(userData)
    });

    const result = await response.json();

    if (result.success) {
      alert('用户信息更新成功');
      // 更新本地存储的用户信息
      this.updateLocalUser(result.data);
    } else {
      alert(result.error.message);
    }
  } catch (error) {
    console.error('更新用户信息失败:', error);
    alert('网络错误，请稍后重试');
  }
}
```

### 2. 测试步骤

**使用Postman测试**:
1. 先登录获取token
2. 创建新集合环境变量（如 `{{token}}`）
3. 设置认证头为 `Bearer {{token}}`
4. 测试不同更新的场景：
   - 只更新昵称
   - 只改密码
   - 修改用户名的同时改密码
   - 测试错误情况（空请求、密码不匹配等）

### 3. 数据库映射

以下字段保存到数据库 `users` 表：

| API字段 | 数据库字段 | 说明 |
|---------|-----------|------|
| username | username | 用户名，唯一 |
| email | email | 邮箱，唯一 |
| nickname | display_name | 显示名称 |
| password | password | BCrypt加密存储 |
| avatar | avatar_url | 头像链接 |
| bio | bio | 个人简介 |

---

## 📝 注意事项

### 安全注意事项

1. **密码修改**
   - 新密码必须通过HTTPS传输
   - 后端自动使用BCrypt进行加密（12轮）
   - 建议前端更新后清除本地存储的旧Token

2. **Token管理**
   - 修改用户名或密码后，Token保持有效
   - 若有更强安全需求，可以考虑在修改密码后使旧Token失效

3. **重复检查**
   - 后端会自动检查用户名/邮箱的重复性
   - 排除当前用户本身，如更新自己邮箱时不会报错

### 性能注意事项

1. **数据库查询优化**
   - 只更新提供的字段，不会更新所有字段
   - 密码修改是单独的数据库操作

2. **缓存建议**
   - 若使用Redis存储用户Session，可在用户信息更新后删除相关缓存

---

## 🐛 问题排查

### 常见错误和解决方案

**1. 400 - 至少需要提供一个要更新的字段**
- **原因**: 请求体为空或所有字段为空
- **解决**: 提供至少一个要更新的字段

**2. 409 - 用户名已存在**
- **原因**: 新用户名被其他用户占用
- **解决**: 使用另一个用户名

**3. 401 - Token无效或已过期**
- **原因**: Token格式错误或已过期
- **解决**: 重新登录获取新Token

**4. 密码修改后**
- **现象**: Token仍然有效
- **说明**: 这是正常行为，没有强制登出
- **建议**: 前端可选择是否在新密码修改后提示用户重新登录

---

**文档结束** ✅