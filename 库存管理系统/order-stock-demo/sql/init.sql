-- ============================================================
-- 电商订单与库存管理系统 - 建库建表脚本
-- 直接在 MySQL 里执行本脚本即可初始化数据库和示例数据
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS order_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE order_demo;

-- 商品表（含库存）
CREATE TABLE IF NOT EXISTS t_product (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name        VARCHAR(128)  NOT NULL COMMENT '商品名称',
    price       DECIMAL(10,2) NOT NULL COMMENT '单价',
    stock       INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
    description VARCHAR(255)  DEFAULT NULL COMMENT '商品描述',
    remark      VARCHAR(255)  DEFAULT NULL COMMENT '商品备注',
    create_time DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME      DEFAULT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_no     VARCHAR(64)   NOT NULL COMMENT '订单号',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    status       TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0待支付 1已支付 2已取消',
    create_time  DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME      DEFAULT NULL COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单明细表（记录下单时的商品快照）
CREATE TABLE IF NOT EXISTS t_order_item (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_id     BIGINT        NOT NULL COMMENT '订单ID',
    product_id   BIGINT        NOT NULL COMMENT '商品ID',
    product_name VARCHAR(128)  NOT NULL COMMENT '商品名称（快照）',
    price        DECIMAL(10,2) NOT NULL COMMENT '下单时单价（快照）',
    quantity     INT           NOT NULL COMMENT '购买数量',
    total_price  DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 初始化示例商品（库存统一设为 10 万，压测时不会因库存不足而失败）
INSERT INTO t_product(name, price, stock, description, create_time, update_time) VALUES
('iPhone 15 Pro', 8999.00, 100000, '苹果旗舰手机', NOW(), NOW()),
('小米14', 3999.00, 100000, '小米旗舰手机', NOW(), NOW()),
('华为 Mate 60', 6999.00, 100000, '华为旗舰手机', NOW(), NOW()),
('联想拯救者笔记本', 7999.00, 100000, '高性能游戏本', NOW(), NOW()),
('AirPods Pro', 1899.00, 100000, '主动降噪耳机', NOW(), NOW()),
('机械键盘', 399.00, 100000, '青轴机械键盘', NOW(), NOW());
