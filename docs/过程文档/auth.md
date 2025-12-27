# 认证 API

Base URL: `http://<host>:8081`

鉴权方式：需要登录的接口使用 HTTP Header `Authorization: Bearer <JWT>`。

## 通用响应结构

所有接口返回 `CommonResponse<T>`：

```json
{
  "success": true,
  "data": {},
  "message": "可选提示信息",
  "error": {
    "code": "错误码",
    "message": "错误信息"
  }
}
```

其中 `success=false` 时，`error` 一定存在，`data` 一般为 `null`。

## 登录

`POST /api/auth/login`

### Request

Headers:

- `Content-Type: application/json`

Body(JSON):

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 用户名或邮箱 |
| `password` | string | 是 | 密码 |

示例：

```json
{
  "username": "admin",
  "password": "admin123"
}
```

### Response

成功：`200 OK`

```json
{
  "success": true,
  "data": {
    "token": "<JWT>",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@blog.com",
      "nickname": "konatabloger",
      "role": "ADMIN",
      "avatar": null,
      "isActive": true
    }
  },
  "message": "登录成功"
}
```

失败：

- `400 BAD REQUEST`：参数校验失败（例如 `username/password` 为空）
- `401 UNAUTHORIZED`：用户名或密码错误（`error.code=INVALID_CREDENTIALS`）
- `403 FORBIDDEN`：用户已禁用（`error.code=USER_INACTIVE`）
- `500 INTERNAL_SERVER_ERROR`：服务器内部错误（`error.code=INTERNAL_ERROR`）

## 获取当前用户信息

`GET /api/auth/profile`

Headers:

- `Authorization: Bearer <JWT>`

成功：`200 OK`，`data` 为 `UserProfileResponse`。

失败：

- `401 UNAUTHORIZED`：Token 无效或已过期（`error.code=INVALID_TOKEN`）
- `404 NOT FOUND`：用户不存在（`error.code=USER_NOT_FOUND`）

## Token 有效性校验（公开接口）

`GET /api/auth/validate`

说明：可带 Header `Authorization: Bearer <JWT>`；不带/无效则返回 `data=false`（仍是 `200 OK`）。

成功：`200 OK`

```json
{
  "success": true,
  "data": true,
  "message": "Token有效"
}
```

## 刷新 Token

`POST /api/auth/refresh`

Headers:

- `Authorization: Bearer <JWT>`

说明：仅当旧 Token 距离过期时间小于 1 小时才会生成新 Token，否则直接返回旧 Token。

成功：`200 OK`，`data` 为新 Token 或旧 Token。

失败：

- `401 UNAUTHORIZED`：Token 无效或已过期（`error.code=INVALID_TOKEN` 或 `error.code=INVALID_TOKEN`）
- `403 FORBIDDEN`：用户已禁用（`error.code=USER_INACTIVE`）
- `401 UNAUTHORIZED`：用户不存在（`error.code=USER_NOT_FOUND`）

## 登出

`POST /api/auth/logout`

说明：JWT 为无状态，服务端仅记录日志；实际登出需要客户端删除 Token。
