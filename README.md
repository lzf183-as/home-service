# 邻家帮 · 社区家政服务预约网站

基于 Spring Boot 3 的社区家政服务预约平台，包含**居民端、服务人员端、管理后台**三套界面，覆盖「浏览服务 → 预约下单 → 接单派单 → 上门服务 → 确认评价」完整闭环。

---

## 技术栈

| 层次 | 选型 |
|---|---|
| 后端 | Spring Boot 3.2.5 / Java 21 / Spring MVC |
| 持久层 | MyBatis-Plus 3.5.7 + HikariCP |
| 数据库 | H2（文件模式，默认，开箱即用） / MySQL 8.0（可切换） |
| 安全 | Spring Security 6 + BCrypt + Session + CSRF |
| 模板 | Thymeleaf 3.1（服务端渲染，无前后端分离） |
| 前端 | 原生 CSS 设计系统 + 少量原生 JS，无 CDN 依赖，可完全离线运行 |

---

## 功能清单

### 居民端
- 注册 / 登录 / 退出，个人资料与密码修改
- 首页分类导航、热门服务推荐
- 服务列表（分类筛选 + 关键字搜索 + 分页）
- 服务详情（服务内容、时长、评分、用户评价）
- **在线预约**：选择日期与时段、数量、地址簿或手填地址、备注，实时计算金额
- 我的订单：状态筛选、订单详情、状态时间轴、取消订单、确认完成、星级评价
- 地址簿：新增 / 编辑 / 删除 / 设为默认

### 服务人员端
- 接单大厅（待接单池，抢单）
- 我的工单（按状态筛选）
- 开始服务 → 提交完工
- 工作台统计（待上门 / 服务中 / 累计完成）

### 管理后台
- 数据看板：订单量、营收、用户数、人员数、近 7 日趋势、热门服务 Top5、状态分布、最近订单
- 订单管理：状态筛选、关键字搜索、**派单 / 改派**、强制取消
- 服务项目：新增、编辑、上下架、分类筛选
- 服务分类：新增、编辑、启停、删除（有服务时禁止删除）
- 居民用户：搜索、禁用 / 启用
- 服务人员：新增账号与档案、编辑工种等级、切换接单状态

---

## 快速开始

环境要求：**JDK 17+**（本项目用 21 编译）、Maven 3.6+。

```bash
cd home-service
mvn clean package -DskipTests
java -jar target/home-service.jar
```

打开 <http://localhost:8080>。

首次启动会自动建表并写入演示数据（8 个账号、15 个服务、6 个分类、5 位服务人员、5 笔不同状态的订单）。
数据库文件位于 `./data/homeservice.mv.db`，删除该目录即可重置演示数据。

### 演示账号

| 角色 | 账号 | 密码 | 登录后进入 |
|---|---|---|---|
| 管理员 | `admin` | `123456789` | `/admin/dashboard` |
| 居民 | `user` | `123456789` | 首页 / 我的订单 |
| 居民 | `lina` | `123456789` | 首页 / 我的订单 |
| 服务人员 | `staff` | `123456789` | `/staff/workbench` |
| 服务人员 | `chenjg` | `123456789` | `/staff/workbench` |

> 其他服务人员账号：`liuhui`、`zhaocm`、`zhoumin`，密码同为 `123456789`。

### 切换到 MySQL

1. 修改 `src/main/resources/application-mysql.yml` 中的 `username` / `password`；
2. 启动时切换 profile：

```bash
java -jar target/home-service.jar --spring.profiles.active=mysql
```

JDBC URL 已带 `createDatabaseIfNotExist=true`，会自动创建 `home_service` 库；表结构与演示数据由 `db/schema.sql` 和启动初始化器自动完成。

> H2 控制台：<http://localhost:8080/h2-console>，JDBC URL `jdbc:h2:file:./data/homeservice`，用户名 `sa`，密码为空。

---

## 订单状态机

```
待接单 PENDING_ACCEPT
   ├─ 服务人员接单 / 管理员派单 → 已接单 ACCEPTED
   │      ├─ 开始服务 → 服务中 SERVING
   │      │      └─ 提交完工 → 待确认 PENDING_CONFIRM
   │      │              └─ 用户确认 → 已完成 COMPLETED（可评价）
   │      └─ 用户/管理员取消 → 已取消 CANCELLED
   └─ 用户/管理员取消 → 已取消 CANCELLED
```

- 状态流转全部使用**带前置状态的条件 UPDATE**（`WHERE id = ? AND status = ?`），影响行数为 0 即视为并发冲突并提示刷新，避免重复接单、重复确认。
- 每次流转写入 `order_status_log`，订单详情页据此渲染进度时间轴。

---

## 数据库设计（8 张表）

| 表 | 说明 |
|---|---|
| `sys_user` | 账号，`role` 区分 USER / STAFF / ADMIN，BCrypt 密码 |
| `staff` | 服务人员档案（工种、等级、评分、接单数、接单状态） |
| `service_category` | 服务分类（图标、描述、排序、启停） |
| `service_item` | 服务项目（价格、计价单位、时长、封面、销量、评分、上下架） |
| `customer_address` | 用户地址簿（默认地址标记） |
| `service_order` | 预约订单（时间、地址、金额、状态、指派人员、各节点时间） |
| `order_review` | 服务评价（一单一评，唯一约束） |
| `order_status_log` | 订单状态流转日志 |

DDL 见 `src/main/resources/db/schema.sql`，语法同时兼容 H2（MySQL 模式）与 MySQL 8.0。

---

## 目录结构

```
home-service/
├── pom.xml
├── scripts/                    # 端到端回归测试脚本（curl，无需额外依赖）
└── src/main/
    ├── java/com/community/homeservice/
    │   ├── common/          # 登录用户、业务异常、全局模型与异常处理
    │   ├── config/          # Security、MyBatis-Plus 分页
    │   ├── controller/      # Home / Service / Order / Profile / Staff / Admin
    │   ├── dto/             # 表单对象与校验注解
    │   ├── entity/          # 8 个实体
    │   ├── enums/           # 订单状态机
    │   ├── init/            # 演示数据初始化
    │   ├── mapper/          # MyBatis-Plus Mapper
    │   └── service/         # 业务服务（订单、目录、用户、人员、统计）
    └── resources/
        ├── application.yml          # 默认 H2
        ├── application-mysql.yml    # MySQL profile
        ├── db/schema.sql
        ├── static/css/app.css       # 手写设计系统（约 1400 行）
        ├── static/js/app.js
        └── templates/               # Thymeleaf 页面（居民 / 员工 / 后台）
```

---

## 关键实现说明

- **权限控制**：`/admin/**` 仅 ADMIN、`/staff/**` 仅 STAFF；同时服务层校验数据归属（居民只能看自己的订单、服务人员只能操作自己的工单），避免改 URL 越权。
- **金额计算**：`BigDecimal` 存储与计算，下单时快照服务名称与单价，后续改价不影响历史订单。
- **密码安全**：BCrypt 加盐哈希；禁用账号在登录时直接拒绝。
- **XSS / CSRF**：Thymeleaf `th:text` 全量转义；表单由 Spring Security 自动注入 CSRF Token。
- **离线可用**：不引用任何 CDN，样式与图标（emoji）本地化，断网也能完整演示。

---

## 自动化功能测试

项目内置两套基于 curl 的端到端回归脚本，覆盖 148 项断言，无需额外依赖（Git Bash / Linux 均可跑）。
运行前请保证应用已启动，且数据库为**初始演示数据**（删掉 `data/` 目录重启即可）。

```bash
bash scripts/test-public-user.sh    # 公开页面 / 认证 / 用户中心 / 下单与订单状态
bash scripts/test-staff-admin.sh    # 员工端 / 管理后台 / 权限与安全
```

脚本会逐条输出 `[PASS]` / `[FAIL]`，末尾汇总数量；第二套会写入测试数据，跑完建议重置 `data/`。

覆盖范围：

| 套件 | 覆盖内容 | 断言数 |
|---|---|---|
| 一 | 首页/列表筛选/搜索/分页/详情/404、注册与登录校验、改资料改密、地址簿增删改、下单校验（数量0/缺日期/过期/超60天）、订单状态流转、取消与重复取消、评价校验、越权访问 | 77 |
| 二 | 接单→开工→完工→居民确认→评价闭环、非法状态流转拦截、他人工单越权、看板统计、订单搜索与派单、分类与服务 CRUD 及上下架联动、居民禁用/启用、员工账号创建与档案维护、跨角色 403、CSRF 拦截 | 71 |

---

## 已知限制

- 未接入真实支付，提交预约后直接进入「待接单」，金额为应付金额展示。
- 通知为页面内提示，未接入短信 / 微信模板消息。
- 服务封面使用 emoji + 渐变色块占位，生产环境可替换为对象存储图片。
- 演示数据中的手机号、地址均为虚构。
