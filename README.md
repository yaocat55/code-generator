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

| 类型 | 生成文件 |
|------|----------|
| Entity | `XxxEntity.java` + `XxxConditionEntity.java` |
| Mapper | `XxxMapper.java` |
| XML | `XxxMapper.xml` |
| Service | `XxxService.java` |
| Controller | `XxxController.java` |
| Vue | `api.js` + `index.vue` |
| Test | `XxxMapperTest.java` + `XxxServiceTest.java` + `XxxControllerTest.java` |

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
│       └── xml/Mapper.xml.vm
```

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
