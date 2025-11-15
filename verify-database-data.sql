-- KONATABLOG 数据库验证脚本
-- 用于验证集成测试数据是否真实写入数据库
-- 运行命令: sqlite3 data/konatablog-integration.db < verify-database-data.sql

-- ==========================================
-- 检查数据库表结构
-- ==========================================
.headers on
.mode table

.echo on
-- 检查所有表
SELECT "所有数据库表:" as info;
SELECT name FROM sqlite_master WHERE type='table';

-- ==========================================
-- 检查用户表数据
-- ==========================================

SELECT "用户表数据:" as info;
SELECT
    id,
    username,
    email,
    display_name as displayName,
    role,
    is_active as isActive,
    created_at as createdAt,
    last_login_at as lastLoginAt
FROM users;

-- ==========================================
-- 检查分类表数据
-- ==========================================

SELECT "分类表数据:" as info;
SELECT
    id,
    name,
    description,
    slug,
    parent_id as parentId,
    is_active as isActive,
    created_at as createdAt
FROM categories;

-- ==========================================
-- 检查文章表数据
-- ==========================================

SELECT "文章表数据:" as info;
SELECT
    id,
    title,
    excerpt,
    status,
    view_count as viewCount,
    is_featured as isFeatured,
    author_id as authorId,
    category_id as categoryId,
    slug,
    created_at as createdAt,
    published_at as publishedAt,
    updated_at as updatedAt
FROM posts;

-- ==========================================
-- 检查标签表数据
-- ==========================================

SELECT "标签表数据:" as info;
SELECT
    id,
    name,
    slug,
    description,
    usage_count as usageCount,
    created_at as createdAt
FROM tags;

-- ==========================================
-- 检查关联表数据
-- ==========================================

SELECT "文章-标签关联表数据:" as info;
SELECT
    post_id as postId,
    tag_id as tagId
FROM post_tags;

-- ==========================================
-- 数据统计
-- ==========================================

SELECT "数据统计:" as info;
SELECT
    'Users' as tableName,
    COUNT(*) as recordCount
FROM users
UNION ALL
SELECT
    'Categories' as tableName,
    COUNT(*) as recordCount
FROM categories
UNION ALL
SELECT
    'Posts' as tableName,
    COUNT(*) as recordCount
FROM posts
UNION ALL
SELECT
    'Tags' as tableName,
    COUNT(*) as recordCount
FROM tags
UNION ALL
SELECT
    'Post_Tags' as tableName,
    COUNT(*) as recordCount
FROM post_tags;

.echo off
SELECT "验证完成！" as message;