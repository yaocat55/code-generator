# Code Generator — 基于 SQL 文件的代码生成器

一个自包含的 Web 应用，上传 SQL 建表语句即可自动生成 Java CRUD 全套代码。

---

## 快速开始

### 环境要求

- JDK 8+（推荐 JDK 17）
- Maven 3.6+

### 启动项目

```bash
# 1. 编译打包
mvn clean package -DskipTests

# 2. 启动
java -jar target/code-generator-exec.jar
```

浏览器打开：**http://localhost:7000**

### 修改端口

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 7000   # 改成你想要的端口
```

---

## 使用指南

### 方式一：上传 SQL 文件

1. 打开页面，默认在 **Upload SQL File** 标签页
2. 点击上传区域或将 `.sql` 文件拖拽到虚线框内
3. 文件内容会自动加载并解析
4. 右侧面板展示解析出的**表名、字段、类型映射**

### 方式二：粘贴 SQL

1. 切换到 **Paste SQL** 标签页
2. 在编辑器中粘贴 CREATE TABLE 语句
3. 点击 **Parse SQL** 按钮（或按 `Ctrl + Enter`）

支持的 SQL 示例：

```sql
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `age` int DEFAULT 0 COMMENT '年龄',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

> 支持同时包含多条 CREATE TABLE 语句（用分号分隔），一次性生成多表代码。

### 配置生成参数

右侧面板可配置：

| 参数 | 说明 | 默认值 |
|------|------|--------|
| **Author** | 代码注释中的作者名 | `sue` |
| **Package Name** | Java 包名 | `com.example` |
| **Code Types** | 选择要生成的代码类型 | Entity/Mapper/XML/Service/Controller |

支持的代码类型：

| 类型 | 生成文件 | 适配脚手架 |
|------|----------|------------|
| Entity | `XxxEntity.java` + `XxxConditionEntity.java` | — |
| Mapper | `XxxMapper.java` | — |
| XML | `XxxMapper.xml` | — |
| Service | `XxxService.java` | — |
| Controller | `XxxController.java` | — |
| Vue | `api.js` + `index.vue` | [vue-element-admin](https://github.com/PanJiaChen/vue-element-admin) |
| React | `api.ts` + `index.tsx` | [Ant Design Pro](https://github.com/ant-design/ant-design-pro) |
| Test | `XxxMapperTest.java` + `XxxServiceTest.java` + `XxxControllerTest.java` | — |

### 生成并下载

1. 确认 SQL 已解析（右侧显示表信息）
2. 勾选需要的代码类型
3. 点击 **Generate & Download ZIP** 按钮
4. 浏览器自动下载 `code-generator.zip`

---

## 模板管理

展开页面底部的 **Template Manager** 面板：

- **查看模板** — 点击模板文件名加载内容到编辑器
- **编辑模板** — 修改后点 Save 保存为用户自定义模板
- **校验语法** — 点 Validate 检查 Velocity 模板语法是否正确
- **还原默认** — 点 Restore Defaults 备份并删除所有自定义模板（会下载备份 ZIP）

自定义模板保存在 `~/.code-generator/templates/` 目录下，优先级高于默认模板。

---

## API 接口

> 所有接口前缀：`/api/gen`

### 上传并解析 SQL 文件

```http
POST /api/gen/upload
Content-Type: multipart/form-data

file: your_ddl.sql
```

### 解析 SQL 文本

```http
POST /api/gen/parse
Content-Type: application/json

{ "sql": "CREATE TABLE ..." }
```

### 生成代码

```http
POST /api/gen/generate
Content-Type: application/json

{
  "author": "sue",
  "packageName": "com.example",
  "createSql": "CREATE TABLE ...",
  "codeTypes": ["entity", "mapper", "xml", "service", "controller"]
}
```

响应为 ZIP 文件流。

### 模板相关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/gen/templates` | 获取模板列表 |
| GET | `/api/gen/template?name=Entity.java.vm` | 获取模板内容 |
| POST | `/api/gen/template` | 保存自定义模板 |
| POST | `/api/gen/template/validate` | 校验模板语法 |
| GET | `/api/gen/template/custom?name=Entity.java.vm` | 检查是否存在自定义模板 |
| POST | `/api/gen/templates/restore` | 还原默认模板（下载备份ZIP） |

---

## 项目结构

```
code-generator/
├── pom.xml
├── src/main/java/cn/net/susan/generate/
│   ├── Application.java                   # 启动入口
│   ├── config/
│   │   ├── GenConfig.java                 # 生成器配置
│   │   └── YamlPropertySourceFactory.java # YAML 配置加载
│   ├── controller/
│   │   ├── GenController.java             # REST API 控制器
│   │   └── PageController.java            # 首页路由
│   ├── domain/                            # 数据对象
│   │   ├── BaseEntity.java
│   │   ├── ColumnConfigInfo.java
│   │   ├── ColumnInfo.java
│   │   ├── GenResult.java
│   │   ├── SqlGenRequest.java
│   │   ├── TableInfo.java
│   │   └── TemplateRequest.java
│   ├── service/
│   │   ├── IGenService.java
│   │   └── impl/GenServiceImpl.java       # 代码生成核心逻辑
│   └── util/
│       ├── CharsetKit.java
│       ├── Convert.java
│       ├── GenUtils.java                  # 模板/类型/文件名工具
│       ├── SqlParser.java                 # CREATE TABLE 解析器
│       ├── StrFormatter.java
│       ├── StringUtil.java
│       └── VelocityInitializer.java       # Velocity 引擎初始化
├── src/main/resources/
│   ├── application.yml                    # Spring Boot 配置
│   ├── generator.yml                      # 代码生成默认参数
│   ├── static/index.html                  # Web UI（自包含 SPA）
│   └── vm/                                # Velocity 模板
│       ├── java/
│       │   ├── ConditionEntity.java.vm
│       │   ├── Controller.java.vm
│       │   ├── ControllerTest.java.vm
│       │   ├── Entity.java.vm
│       │   ├── Mapper.java.vm
│       │   ├── MapperTest.java.vm
│       │   ├── Service.java.vm
│       │   └── ServiceTest.java.vm
│       ├── test/pom-dependencies.xml.vm
│       ├── vue/
│       │   ├── api.js.vm
│       │   └── index.vue.vm
│       ├── react/
│       │   ├── api.ts.vm
│       │   └── index.tsx.vm
│       └── xml/Mapper.xml.vm
```

---

## 设计思路

### 整体架构

```
 ┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
 │ SQL 输入  │ →  │  SqlParser   │ →  │  Velocity    │ →  │  ZIP 输出    │
 │ (文件/粘贴)│    │  解析为 Table │    │  模板渲染     │    │  (完整项目)  │
 └──────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

整个代码生成器由四个核心环节组成，以 **流水线** 的方式串联。

### 1. SQL 解析 → TableInfo

`SqlParser` 用正则表达式解析 `CREATE TABLE` 语句，提取：

- **表名** — 通过 `GenUtils.tableToJava()` 转为 UpperCamelCase 的 `className`
- **表注释** — 去掉"表/信息/管理"等冗余词，作为页面标题
- **字段列表** — 每个字段拆出：列名、数据类型、注释、是否自增

解析后对每个字段调用 `GenUtils.transColums()` 做二次加工：

| 加工步骤 | 示例 |
|----------|------|
| 列名转 camelCase | `create_time` → `createTime` / `CreateTime` |
| SQL 类型映射 Java 类型 | `varchar` → `String`，`bigint` → `Long` |
| 标记主键 | 自增字段自动设为主键 |

最终得到 `TableInfo`（含 `className`、`primaryKey`、`ColumnInfo[]`），作为后续模板渲染的**唯一数据来源**。

### 2. Velocity 上下文构建

`GenUtils.getVelocityContext()` 将 `TableInfo` 展开为模板变量：

| 变量 | 含义 | 示例 |
|------|------|------|
| `${className}` | 大驼峰类名 | `User` |
| `${classname}` | 小驼峰类名 | `user` |
| `${tableName}` | 原始表名 | `user` |
| `${tableComment}` | 表注释 | `用户` |
| `${primaryKey}` | 主键字段名 | `id` |
| `${columns}` | 字段列表 | 可迭代 `#foreach` |
| `${package}` | 包名 | `com.example` |
| `${moduleName}` | 模块名（包名最后一段） | `example` |
| `${author}` | 作者 | `sue` |
| `${datetime}` | 生成时间 | `2025-01-01 12:00:00` |

模板通过 Velocity 的 `#foreach`、`#if` 等指令遍历列、过滤审计字段，生成最终代码。

### 3. 模板组织与分发

模板按**输出目标**分目录存放：

```
vm/
├── java/       → Java 代码（Entity, Mapper, Service, Controller, Tests）
├── xml/        → MyBatis XML 映射文件
├── vue/        → Vue 2 + Element UI（vue-element-admin 脚手架）
└── react/      → React + TypeScript + Ant Design Pro（ProTable / ModalForm）
```

每套模板是一个**独立、可替换的产出单元**。勾选不同 Code Type，后端通过 `filterTemplatesByCodeTypes()` 筛选对应的 `.vm` 模板进行渲染，不勾选的模板完全不参与渲染。

Code Type → 模板映射关系：

```
entity      → Entity.java.vm, ConditionEntity.java.vm
mapper      → Mapper.java.vm
xml         → Mapper.xml.vm
service     → Service.java.vm
controller  → Controller.java.vm
vue         → api.js.vm, index.vue.vm
react       → api.ts.vm, index.tsx.vm
test        → MapperTest.java.vm, ServiceTest.java.vm, ControllerTest.java.vm
```

### 4. 文件命名与 ZIP 打包

`GenUtils.getFileName()` 根据模板名称推导输出路径。关键逻辑：

- **Java 类名** — 从模板内容正则提取 `class ${className}XXX` 中的后缀（如 `Entity`、`Mapper`），拼出完整文件名
- **包路径** — 从模板内容正则提取 `package ${package}.xxx` 中的子包名，拼出目录结构
- **前端文件** — 直接用固定规则：`api.js` → `templates/{classname}/api.js`

所有文件写入 `ZipOutputStream`，在内存中构建 ZIP，直接输出为 HTTP 响应流——不落盘。

### 5. 自定义模板覆盖机制

用户通过 UI 编辑模板后，内容保存到 `~/.code-generator/templates/` 目录。渲染时优先使用该目录下的同名文件：

```
classpath:vm/java/Entity.java.vm  (默认)
          ↓ 用户保存后
~/.code-generator/templates/Entity.java.vm  (优先)
```

由 `GenConfig.ignoreCustomTemplate` 控制开关，默认关闭（忽略自定义）。还原时会将自定义模板打包为 ZIP 下载后删除。

### 6. 前端框架扩展方式

要新增一个前端框架（如本次的 React），只需三步：

1. **新增模板** — 在 `vm/{framework}/` 下放置 `.vm` 模板文件
2. **注册模板** — 在 `GenUtils.getTemplates()` 添加路径，在 `getFileName()` 添加命名规则
3. **注册 Code Type** — 在 `GenServiceImpl.filterTemplatesByCodeTypes()` 添加映射，在 `index.html` 添加复选框

整个过程不涉及任何 SQL 解析、类型映射、ZIP 打包的改动，因为这些环节是**框架无关**的。模板只消费 `VelocityContext` 里的变量，前端框架之间的差异完全隔离在模板文件内部。

---

## 类型映射

SQL 类型到 Java 类型的默认映射：

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

## 常见问题

**Q: 支持哪些数据库的 SQL 语法？**

支持 MySQL / MariaDB 的 CREATE TABLE 语法。其他数据库的 DDL 如结构与 MySQL 接近通常也能解析。

**Q: 自定义模板不生效？**

检查 `generator.yml` 中 `gen.ignoreCustomTemplate` 是否为 `false`。默认为 `true`（忽略自定义模板）。

**Q: 如何修改生成的包名和作者默认值？**

编辑 `src/main/resources/generator.yml`：

```yaml
gen:
  author: 你的名字
  packageName: com.yourcompany
```
