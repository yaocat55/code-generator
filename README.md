# Code Generator — Admin 后台管理代码生成器

[![Java](https://img.shields.io/badge/Java-17-brightgreen.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![AI Ready](https://img.shields.io/badge/AI-OpenAI%20Compatible-purple.svg)](https://spring.io/projects/spring-ai)

一个自包含的 Web 应用，上传 SQL 建表语句或需求文档即可自动生成 Admin 后台 CRUD 全套代码。集成 Spring AI，支持 AI 需求分析生成 SQL、AI 表结构审计、AI 修复 SQL。

面向 **管理系统、运营后台、内部工具** 等 B 端场景。

![演示截图](示意图.png)

---

## 目录

- [功能特性](#功能特性)
- [快速开始](#快速开始)
- [使用指南](#使用指南)
  - [上传 SQL / 粘贴 SQL](#上传-sql--粘贴-sql)
  - [AI 需求分析](#ai-需求分析)
  - [AI 表结构审计](#ai-表结构审计)
  - [AI 修复](#ai-修复)
  - [架构模式](#架构模式)
  - [代码类型](#代码类型)
  - [数据库类型](#数据库类型)
- [配置](#配置)
  - [应用配置](#应用配置)
  - [生成器配置](#生成器配置)
- [API 接口](#api-接口)
- [模板管理](#模板管理)
- [项目结构](#项目结构)
- [类型映射](#类型映射)
- [表设计规范](#表设计规范)
- [常见问题](#常见问题)

---

## 功能特性

- **SQL 解析** — 上传 `.sql` 文件或粘贴 SQL 文本，自动解析表名、字段、类型映射
- **AI 需求分析** — 上传需求文档（`.docx` / `.md` / `.txt`）或输入文本，AI 自动生成建表 SQL
- **AI 表结构审计** — 逐表分析问题，自动识别关联表，按 ❌ 错误 / ⚠️ 警告 / 💡 建议 分级展示
- **AI 修复** — 一键根据审计结果修复 SQL，自动刷新表结构预览
- **丰富架构支持** — 标准 MyBatis / MyBatis-Plus / JPA / DDD，自由组合
- **多端前端代码** — Vue（vue-element-admin）/ React（Ant Design Pro）前端页面生成
- **多数据库** — MySQL / PostgreSQL / SQL Server，自动识别方言
- **测试代码** — MapperTest / ServiceTest / ControllerTest 自动生成
- **模板管理** — 在线编辑、保存、校验 Velocity 模板，支持自定义和还原

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- AI 功能需要可用的 OpenAI 兼容 API（DeepSeek / Qwen / ZhipuAI 等）

### 启动项目

```bash
mvn spring-boot:run
```

浏览器打开：**http://localhost:7000**

### 修改端口 / AI 配置

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 7000

spring:
  ai:
    openai:
      api-key: ${AI_API_KEY:your-api-key}
      base-url: ${AI_BASE_URL:https://api.deepseek.com}
      chat:
        options:
          model: ${AI_MODEL:deepseek-chat}
          temperature: 0.3
```

支持通过环境变量配置（方便 CI/CD 和容器化部署）：

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `AI_API_KEY` | API 密钥 | — |
| `AI_BASE_URL` | API 端点 | `https://api.deepseek.com` |
| `AI_MODEL` | 模型名称 | `deepseek-chat` |

---

## 使用指南

### 上传 SQL / 粘贴 SQL

1. 上传 `.sql` 文件或将 CREATE TABLE 语句粘贴到编辑器
2. 文件上传后自动解析，粘贴后点击「解析 SQL」（或 `Ctrl+Enter`）
3. 右侧面板展示解析出的**表名、字段列表、类型映射**

### AI 需求分析

1. 切换到「AI 分析」标签页
2. 上传需求文档（`.docx` / `.md` / `.txt`）或直接输入需求描述
3. 点击「AI 分析生成 SQL」，AI 自动生成 CREATE TABLE 语句
4. 可在编辑器中查看/修改生成的 SQL，然后解析、生成代码

### AI 表结构审计

解析 SQL 后，点击「AI 审计」按钮：

- AI 逐表分析问题，自动识别关联表（不会对中间表误报缺少审计字段）
- 问题按严重程度用 ❌ 错误 / ⚠️ 警告 / 💡 建议 标识，直接显示在对应表头
- 点击表头旁的圆圈 `!` 图标查看详细分析和修复建议

### AI 修复

审计发现问题后，点击「AI 修复」——AI 根据审计结果修复 SQL 并自动刷新表结构预览。

### 架构模式

右侧配置面板支持四种架构模式，可任意组合：

| 模式 | 说明 |
|------|------|
| 标准 MyBatis | 分层架构：entity / mapper / service / controller |
| 🔥 MyBatis-Plus | @TableName / BaseMapper / IService，自动禁用 XML |
| 🍃 JPA | @Entity / JpaRepository，自动禁用 XML |
| 🏗 DDD 架构 | 四层：domain / infrastructure / application / interfaces |

- MyBatis-Plus 和 JPA 互斥（不同 ORM）
- DDD 可与标准 MyBatis 或 MyBatis-Plus 组合
- DDD 模式下每张表按领域名分包，而非统一模块名

### 代码类型

| 类型 | 生成文件 |
|------|----------|
| Entity | `XxxEntity.java` + `XxxConditionEntity.java` |
| Mapper | `XxxMapper.java`（或 Repository / JpaRepository） |
| XML | `XxxMapper.xml`（MySQL / PostgreSQL / SQL Server 方言） |
| Service | `XxxService.java` + `XxxServiceImpl.java` |
| Controller | `XxxController.java` |
| Vue | `api.js` + `index.vue`（vue-element-admin） |
| React | `api.ts` + `index.tsx`（Ant Design Pro） |
| Test | MapperTest + ServiceTest + ControllerTest |

### 数据库类型

支持 MySQL / PostgreSQL / SQL Server，影响：

- AI 生成 SQL 的方言语法
- Mapper XML 的分页语法和时间函数
- SQL 解析器自动识别并高亮对应选项

---

## 配置

### 应用配置

`src/main/resources/application.yml`：

```yaml
server:
  port: 7000

spring:
  ai:
    openai:
      api-key: ${AI_API_KEY:your-api-key}
      base-url: ${AI_BASE_URL:https://api.deepseek.com}
      chat:
        options:
          model: ${AI_MODEL:deepseek-chat}
          temperature: 0.3
```

### 生成器配置

`src/main/resources/generator.yml`：

```yaml
gen:
  author: yourname           # 默认作者名
  packageName: com.example   # 默认包名
  autoRemovePre: true        # 自动移除表前缀
  tablePrefix: ""            # 表前缀（自动移除）
  ignoreCustomTemplate: true # 是否忽略自定义模板
```

---

## API 接口

所有接口前缀：`/api/gen`

### 解析

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/upload` | 上传 SQL 文件 |
| POST | `/parse` | 解析 SQL 文本 |

### AI

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/analyze` | AI 分析需求文档（文件） |
| POST | `/ai/analyze-text` | AI 分析需求文本 |
| POST | `/ai/audit` | AI 逐表审计 SQL |
| POST | `/ai/fix` | AI 修复 SQL |

### 生成

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/generate` | 生成代码 ZIP |

### 模板

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/templates` | 获取模板列表 |
| GET | `/template?name=` | 获取模板内容 |
| POST | `/template` | 保存自定义模板 |
| POST | `/template/validate` | 校验模板语法 |
| GET | `/template/custom?name=` | 检查自定义模板 |
| POST | `/templates/restore` | 还原默认模板 |

---

## 模板管理

展开右侧「模板管理」面板：

- 点击模板文件加载到编辑器
- 修改后保存为用户自定义模板（`~/.code-generator/templates/`）
- 校验 Velocity 语法
- 还原默认模板（自动备份下载 ZIP）

---

## 项目结构

```
code-generator/
├── pom.xml
├── src/main/java/cn/net/yao/generate/
│   ├── Application.java
│   ├── config/
│   │   ├── GenConfig.java
│   │   ├── JacksonConfig.java
│   │   └── YamlPropertySourceFactory.java
│   ├── controller/
│   │   ├── GenController.java
│   │   └── PageController.java
│   ├── domain/
│   │   ├── AiAnalysisResult.java
│   │   ├── ColumnInfo.java
│   │   ├── GenResult.java
│   │   ├── SqlGenRequest.java
│   │   ├── TableAuditResult.java
│   │   ├── TableInfo.java
│   │   └── TemplateRequest.java
│   ├── service/
│   │   ├── IAiSqlService.java
│   │   ├── IGenService.java
│   │   └── impl/
│   │       ├── AiSqlServiceImpl.java
│   │       └── GenServiceImpl.java
│   └── util/
│       ├── DocumentReader.java
│       ├── GenUtils.java
│       ├── SqlParser.java
│       └── VelocityInitializer.java
├── src/main/resources/
│   ├── application.yml
│   ├── generator.yml
│   ├── templates/
│   │   ├── index.html
│   │   └── fragments/
│   │       ├── head.html
│   │       └── header.html
│   └── vm/
│       ├── java/           # 标准 MyBatis 模板
│       │   ├── ddd/        #   DDD 架构
│       │   │   └── mp/     #     DDD + MyBatis-Plus
│       │   ├── jpa/        #   JPA
│       │   └── mp/         #   MyBatis-Plus
│       ├── xml/            # MyBatis XML 模板
│       ├── vue/            # Vue 前端模板
│       ├── react/          # React 前端模板
│       └── test/           # 测试依赖 POM
```

---

## 类型映射

| SQL 类型 | Java 类型 |
|----------|-----------|
| tinyint / smallint / int / integer | `Integer` |
| bigint | `Long` |
| float | `Float` |
| double | `Double` |
| decimal | `BigDecimal` |
| bit | `Boolean` |
| char / varchar / text / longtext | `String` |
| date / datetime / timestamp / time | `Date` |

---

## 表设计规范

### 审计字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `bigint` | 主键（雪花ID） |
| `create_user_id` | `bigint` | 创建人ID |
| `create_user_name` | `varchar(50)` | 创建人名称 |
| `create_time` | `datetime` | 创建时间 |
| `update_user_id` | `bigint` | 更新人ID |
| `update_user_name` | `varchar(50)` | 更新人名称 |
| `update_time` | `datetime(3)` | 更新时间（兼乐观锁） |
| `is_del` | `tinyint` | 软删除标记（0=正常，1=删除） |

### 关联表例外

多对多关联表（如 `user_role`、`role_permission`）仅含 PK + FK 字段时，无需包含审计字段。AI 审计会自动识别关联表并跳过相关检查。

---

## 常见问题

**Q: AI 功能不生效？**

检查 `application.yml` 中的 `spring.ai.openai.*` 配置是否正确。本项目使用 OpenAI 兼容协议，只要你的 LLM 服务商提供兼容的 API 端点即可使用。

**Q: 支持哪些数据库？**

MySQL / PostgreSQL / SQL Server。SQL 解析器自动识别三种数据库的引用语法；AI 分析和修复都会根据所选数据库生成对应方言。

**Q: 如何修改生成的包名和作者默认值？**

编辑 `src/main/resources/generator.yml` 或在 Web UI 右侧配置面板中直接修改。
