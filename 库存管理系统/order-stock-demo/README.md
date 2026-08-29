# 电商订单与库存管理系统（示例项目）

一个适合初学者学习和二次改造的电商后端小项目，覆盖了最核心的「下单 → 扣库存」业务链路。

## 技术栈

| 技术 | 作用 |
| --- | --- |
| Java 8 | 编程语言 |
| Spring Boot 2.7 | 应用框架 |
| MyBatis | 数据库访问（注解方式写 SQL，简单直观） |
| MySQL | 数据存储 |

## 核心功能

- **商品管理**：查询、新增、调整库存、删除
- **订单管理**：创建订单（乐观锁扣库存）、支付、取消（回补库存）、查看明细
- **数据概览**：商品总数、订单总数、已支付金额、低库存统计
- **前端页面**：原生 HTML/CSS/JS 做的管理后台，无需前端构建工具

## 核心流程说明

```
用户下单
   │
   ├─ 1. 校验商品与数量
   ├─ 2. 乐观锁扣减库存  UPDATE ... SET stock = stock - n WHERE id=? AND stock >= n
   └─ 3. 创建订单 + 订单明细（同一事务，任何一步失败整体回滚）
```

关键点：
- **扣库存用乐观锁**：`WHERE stock >= n` 保证库存不为负，高并发下也安全。
- **事务**：扣库存、建订单在同一个 `@Transactional` 里。
- **金额用 BigDecimal**：避免浮点数精度丢失。

## 目录结构

```
order-stock-demo
├── pom.xml
├── docker-compose.yml              # 一键启动 MySQL
├── sql/init.sql                    # 建库建表 + 示例数据
└── src/main
    ├── java/com/example/order
    │   ├── OrderApplication.java     # 启动类
    │   ├── common/                   # 统一返回、异常处理
    │   ├── controller/               # 接口层
    │   ├── dto/                      # 请求参数对象
    │   ├── entity/                   # 实体（对应表）
    │   ├── mapper/                   # MyBatis 接口（注解 SQL）
    │   └── service/                  # 业务逻辑
    └── resources
        ├── application.yml           # 配置
        └── static/                   # 前端页面
```

## 环境准备

只需要两样东西：

1. **JDK 8+**（建议 8 或 11）
2. **Maven 3.6+**
3. **Docker Desktop**（用来跑 MySQL，避免你手动安装 MySQL）

## 启动步骤（跟着做就行）

### 第 1 步：启动 MySQL

在项目根目录打开终端，执行：

```bash
docker compose up -d
```

这一条命令会：
- 自动下载 MySQL 镜像
- 启动一个 MySQL 容器
- **自动执行 `sql/init.sql`**，建好 `order_demo` 数据库、3 张表，并插入 6 个示例商品

> 验证是否成功：`docker ps` 能看到 `order-mysql` 状态是 `Up`。
>
> 停止 MySQL：`docker compose down`（加 `-v` 会连数据一起删掉，下次重新初始化）

### 第 2 步：启动项目

在项目根目录执行：

```bash
mvn spring-boot:run
```

或者用 IDEA 打开项目，运行 `OrderApplication` 主类。

> 默认配置里 MySQL 的账号密码都是 `root`，和 `docker-compose.yml` 里设置的一致，一般不用改。
> 如果你本机 3306 端口已经被占用，改一下 `docker-compose.yml` 里的端口映射和 `application.yml` 里的 URL 即可。

### 第 3 步：打开页面

浏览器访问 http://localhost:8080

- **概览** http://localhost:8080/index.html — 统计卡片 + 最近订单
- **商品管理** http://localhost:8080/products.html — 新增 / 调整库存 / 删除
- **订单管理** http://localhost:8080/orders.html — 创建订单 / 支付 / 取消 / 详情

## 接口列表

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/products | 商品列表 |
| GET | /api/products/{id} | 商品详情 |
| POST | /api/products | 新增商品 |
| PUT | /api/products/{id}/stock | 调整库存 `{"delta": 5}`（正增负减） |
| DELETE | /api/products/{id} | 删除商品 |
| GET | /api/orders | 订单列表 |
| GET | /api/orders/{id} | 订单详情（含明细） |
| POST | /api/orders | 创建订单 `{"items":[{"productId":1,"quantity":2}]}` |
| POST | /api/orders/{id}/pay | 支付订单 |
| POST | /api/orders/{id}/cancel | 取消订单（回补库存） |
| GET | /api/stats | 概览统计 |

## 二次改造建议（等你熟练后可以加）

- **加 Redis 缓存**：商品列表、热点数据放 Redis，减轻数据库压力。
- **加 RocketMQ**：实现「下单 30 分钟未支付自动取消」之类的异步场景。
- **加用户表**：订单关联用户，实现登录、下单归属。
- **加订单号生成规则**：用分布式 ID（雪花算法 / 号段模式）。
- **拆成微服务**：商品服务、订单服务分开部署。

## 常见问题

- **Lombok 报红**：IDEA 安装 Lombok 插件，并在 `Settings → Build → Compiler → Annotation Processors` 勾选 `Enable annotation processing`。
- **连不上 MySQL**：确认 `docker compose up -d` 执行过、`docker ps` 里 `order-mysql` 是 Up 状态；确认 `application.yml` 的账号密码是 `root/root`。
- **端口占用**：改 `application.yml` 里的 `server.port`（项目端口）或 `docker-compose.yml` 里的 MySQL 端口映射。
