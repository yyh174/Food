-- =====================================================
-- 餐饮SaaS多租户管理系统 - 数据库初始化脚本
-- 数据库名: food_saas
-- MySQL 8.0+
-- =====================================================

DROP DATABASE IF EXISTS food_saas;
CREATE DATABASE food_saas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE food_saas;

-- =====================================================
-- 1. 租户表 (简化架构：租户即品牌)
-- =====================================================
CREATE TABLE tenant (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码',
  tenant_name VARCHAR(100) NOT NULL COMMENT '租户名称(品牌名称)',
  logo_url VARCHAR(255) DEFAULT NULL COMMENT '品牌Logo',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
  contact_name VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  contact_phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  invite_code VARCHAR(20) NOT NULL COMMENT '员工邀请码',
  api_quota INT DEFAULT 1000 COMMENT 'API配额',
  api_used INT DEFAULT 0 COMMENT '已使用API次数',
  expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted_at TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_code (tenant_code),
  UNIQUE KEY uk_tenant_name (tenant_name),
  UNIQUE KEY uk_invite_code (invite_code),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- =====================================================
-- 2. 门店表
-- =====================================================
CREATE TABLE shop (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '门店ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  shop_code VARCHAR(50) NOT NULL COMMENT '门店编码(系统自动生成)',
  shop_name VARCHAR(100) NOT NULL COMMENT '门店名称',
  province VARCHAR(50) DEFAULT NULL COMMENT '省份',
  city VARCHAR(50) DEFAULT NULL COMMENT '城市',
  district VARCHAR(50) DEFAULT NULL COMMENT '区县',
  address VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  manager_name VARCHAR(50) DEFAULT NULL COMMENT '店长姓名',
  manager_phone VARCHAR(20) DEFAULT NULL COMMENT '店长电话',
  meituan_id VARCHAR(50) DEFAULT NULL COMMENT '美团门店ID',
  ele_id VARCHAR(50) DEFAULT NULL COMMENT '饿了么门店ID',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-已停业, 1-营业中, 2-休息中',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted_at TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_shop_code (tenant_id, shop_code),
  UNIQUE KEY uk_meituan_id (meituan_id),
  UNIQUE KEY uk_ele_id (ele_id),
  KEY idx_tenant_id (tenant_id),
  KEY idx_shop_code (shop_code),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店表';

-- 添加外键
ALTER TABLE shop ADD CONSTRAINT fk_shop_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

-- =====================================================
-- 3. 系统角色表 (精简为3个角色)
-- =====================================================
CREATE TABLE sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
  role_type TINYINT NOT NULL COMMENT '角色类型: 1-超级管理员, 2-租户管理员, 3-店长',
  description VARCHAR(255) DEFAULT NULL COMMENT '描述',
  permissions JSON DEFAULT NULL COMMENT '权限列表',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- =====================================================
-- 4. 系统用户表
-- =====================================================
CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '用户名',
  password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  tenant_id BIGINT DEFAULT NULL COMMENT '所属租户ID(超级管理员为NULL)',
  shop_id BIGINT DEFAULT NULL COMMENT '所属门店ID(店长角色有值)',
  role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
  last_login_time DATETIME NULL DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted_at TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_username (tenant_id, username),
  KEY idx_tenant_id (tenant_id),
  KEY idx_role_code (role_code),
  KEY idx_status (status),
  KEY idx_shop_id (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 添加外键
ALTER TABLE sys_user ADD CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);
ALTER TABLE sys_user ADD CONSTRAINT fk_user_role FOREIGN KEY (role_code) REFERENCES sys_role(role_code);
ALTER TABLE sys_user ADD CONSTRAINT fk_user_shop FOREIGN KEY (shop_id) REFERENCES shop(id);

-- =====================================================
-- 5. 评价表
-- =====================================================
CREATE TABLE review (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  platform TINYINT NOT NULL COMMENT '平台: 1-美团, 2-饿了么',
  platform_order_id VARCHAR(100) DEFAULT NULL COMMENT '平台订单ID',
  platform_review_id VARCHAR(100) DEFAULT NULL COMMENT '平台评价ID',
  shop_id BIGINT NOT NULL COMMENT '门店ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID(冗余字段，用于快速查询)',
  star_rating TINYINT NOT NULL COMMENT '星级评分(1-5)',
  content TEXT COMMENT '评价内容',
  order_time DATETIME NULL DEFAULT NULL COMMENT '下单时间',
  reply_content TEXT COMMENT '商家回复(非空=已回复)',
  reply_time DATETIME NULL DEFAULT NULL COMMENT '回复时间',
  reply_user_id BIGINT DEFAULT NULL COMMENT '回复人ID',
  tags JSON DEFAULT NULL COMMENT '问题标签列表',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_platform_shop (platform, shop_id),
  KEY idx_tenant_created (tenant_id, created_at),
  KEY idx_star_rating (star_rating),
  KEY idx_order_time (order_time),
  UNIQUE KEY uk_platform_review (platform, platform_review_id, tenant_id),
  FULLTEXT KEY ft_content (content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

-- 添加外键
ALTER TABLE review ADD CONSTRAINT fk_review_shop FOREIGN KEY (shop_id) REFERENCES shop(id);

-- =====================================================
-- 5.1 评价标签表（预置+自定义）
-- =====================================================
CREATE TABLE review_tag (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  tenant_id BIGINT DEFAULT NULL COMMENT '租户ID(NULL表示系统预置)',
  name VARCHAR(50) NOT NULL COMMENT '标签名称',
  type VARCHAR(20) NOT NULL COMMENT '标签类型: system-系统预置, custom-自定义',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_tenant_id (tenant_id),
  UNIQUE KEY uk_tenant_name (tenant_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价标签表';

-- =====================================================
-- 7. 回复模板表
-- =====================================================
CREATE TABLE reply_template (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  name VARCHAR(100) NOT NULL COMMENT '模板名称',
  content VARCHAR(500) NOT NULL COMMENT '回复内容',
  sentiment_type TINYINT NOT NULL COMMENT '适用情感类型: 1-正面, 2-中性, 3-负面',
  sort_order INT DEFAULT 0 COMMENT '排序权重，越大越靠前',
  status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted_at TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (id),
  KEY idx_tenant_id (tenant_id),
  KEY idx_sentiment (sentiment_type),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回复模板表';

-- 添加外键
ALTER TABLE reply_template ADD CONSTRAINT fk_template_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

-- =====================================================
-- 8. 门店日指标表
-- =====================================================
CREATE TABLE shop_metric_daily (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  shop_id BIGINT NOT NULL COMMENT '门店ID',
  stat_date DATE NOT NULL COMMENT '统计日期',
  meituan_order_count INT DEFAULT 0 COMMENT '美团订单量',
  meituan_order_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '美团营业额(元)',
  meituan_review_count INT DEFAULT 0 COMMENT '美团评价数',
  meituan_avg_rating DECIMAL(3,1) DEFAULT 0.0 COMMENT '美团平均评分',
  meituan_negative_count INT DEFAULT 0 COMMENT '美团差评数(1-2星)',
  ele_order_count INT DEFAULT 0 COMMENT '饿了么订单量',
  ele_order_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '饿了么营业额(元)',
  ele_review_count INT DEFAULT 0 COMMENT '饿了么评价数',
  ele_avg_rating DECIMAL(3,1) DEFAULT 0.0 COMMENT '饿了么平均评分',
  ele_negative_count INT DEFAULT 0 COMMENT '饿了么差评数(1-2星)',
  total_order_count INT DEFAULT 0 COMMENT '总订单量',
  total_order_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '总营业额(元)',
  total_review_count INT DEFAULT 0 COMMENT '总评价数',
  total_avg_rating DECIMAL(3,1) DEFAULT 0.0 COMMENT '总平均评分',
  total_negative_count INT DEFAULT 0 COMMENT '总差评数(1-2星)',
  good_review_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '综合好评率(%)',
  avg_unit_price DECIMAL(10,2) DEFAULT 0.00 COMMENT '平均客单价(元)',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_shop_date (shop_id, stat_date),
  KEY idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店日指标表';

-- 添加外键
ALTER TABLE shop_metric_daily ADD CONSTRAINT fk_metric_shop FOREIGN KEY (shop_id) REFERENCES shop(id);

-- =====================================================
-- 9. 工单类型表
-- =====================================================
CREATE TABLE ticket_type (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  tenant_id BIGINT DEFAULT NULL COMMENT '租户ID(NULL表示系统预置)',
  name VARCHAR(50) NOT NULL COMMENT '类型名称',
  type VARCHAR(20) NOT NULL COMMENT '类型: system-系统预置, custom-自定义',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统预置: 0-否, 1-是',
  support_review TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持关联评价: 0-否, 1-是',
  sort_order INT DEFAULT 0 COMMENT '排序权重',
  status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_tenant_id (tenant_id),
  UNIQUE KEY uk_tenant_name (tenant_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单类型表';

-- =====================================================
-- 9.1 邀请码记录表
-- =====================================================
CREATE TABLE invite_code_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  invite_code VARCHAR(32) NOT NULL COMMENT '邀请码',
  status VARCHAR(20) NOT NULL DEFAULT 'unused' COMMENT '状态: unused/used/expired/invalidated',
  expire_time DATETIME NOT NULL COMMENT '过期时间',
  used_time DATETIME DEFAULT NULL COMMENT '使用时间',
  used_user_id BIGINT DEFAULT NULL COMMENT '使用人ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_invite_code (invite_code),
  KEY idx_tenant_status (tenant_id, status),
  KEY idx_expire_time (expire_time),
  KEY idx_used_user_id (used_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码记录表';

-- =====================================================
-- 10. 工单表
-- =====================================================
CREATE TABLE ticket (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  ticket_no VARCHAR(50) NOT NULL COMMENT '工单编号',
  type_id BIGINT NOT NULL COMMENT '工单类型ID',
  status TINYINT NOT NULL COMMENT '状态: 1-待处理, 2-处理中, 3-待核销, 4-已归档, 5-已忽略',
  category TINYINT DEFAULT NULL COMMENT '问题分类: 1-环境, 2-服务, 3-菜品, 4-其他',
  shop_id BIGINT NOT NULL COMMENT '门店ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  title VARCHAR(200) NOT NULL COMMENT '工单标题',
  description TEXT COMMENT '问题描述',
  detail_data JSON DEFAULT NULL COMMENT '异常详情数据',
  deadline DATETIME NULL DEFAULT NULL COMMENT '处理期限',
  solution TEXT COMMENT '整改处理',
  review_id BIGINT DEFAULT NULL COMMENT '关联的原始评价ID',
  creator_id BIGINT NOT NULL COMMENT '创建人ID',
  assignee_id BIGINT DEFAULT NULL COMMENT '处理人ID',
  priority VARCHAR(20) NOT NULL DEFAULT 'low' COMMENT '优先级: high-高, medium-中, low-低',
  images JSON DEFAULT NULL COMMENT '图片附件URL列表',
  suggestion TEXT COMMENT 'AI建议+租户管理员编辑',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted_at TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket_no (tenant_id, ticket_no),
  KEY idx_type_id (type_id),
  KEY idx_shop_status (shop_id, status),
  KEY idx_tenant_created (tenant_id, created_at),
  KEY idx_assignee_id (assignee_id),
  KEY idx_deadline (deadline),
  KEY idx_deleted_at (deleted_at),
  KEY idx_review_id (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

-- =====================================================
-- 11. 工单时间线表
-- =====================================================
CREATE TABLE ticket_timeline (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  ticket_id BIGINT NOT NULL COMMENT '工单ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  action VARCHAR(50) NOT NULL COMMENT '操作类型',
  operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  remark TEXT COMMENT '备注',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_ticket_id (ticket_id),
  KEY idx_tenant_id (tenant_id),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单时间线表';

-- 添加外键
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_type FOREIGN KEY (type_id) REFERENCES ticket_type(id);
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_shop FOREIGN KEY (shop_id) REFERENCES shop(id);
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_review FOREIGN KEY (review_id) REFERENCES review(id);
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id);
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id);
ALTER TABLE ticket_timeline ADD CONSTRAINT fk_timeline_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id);
ALTER TABLE ticket_timeline ADD CONSTRAINT fk_timeline_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id);
ALTER TABLE invite_code_record ADD CONSTRAINT fk_invite_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);
ALTER TABLE invite_code_record ADD CONSTRAINT fk_invite_used_user FOREIGN KEY (used_user_id) REFERENCES sys_user(id);

-- =====================================================
-- 11. AI对话会话表
-- =====================================================
CREATE TABLE ai_chat_session (
  session_id VARCHAR(100) NOT NULL COMMENT '会话ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  title VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  message_count INT DEFAULT 0 COMMENT '消息数量',
  last_message TEXT COMMENT '最后一条消息摘要',
  summary TEXT COMMENT '历史对话摘要',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (session_id),
  KEY idx_tenant_id (tenant_id),
  KEY idx_user_id (user_id),
  KEY idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- 添加外键
ALTER TABLE ai_chat_session ADD CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id);

-- =====================================================
-- 12. AI对话记录表
-- =====================================================
CREATE TABLE ai_chat_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  session_id VARCHAR(100) NOT NULL COMMENT '会话ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  intent_type VARCHAR(50) DEFAULT NULL COMMENT '意图类型',
  message_role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system',
  content TEXT NOT NULL COMMENT '消息内容',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_session_id (session_id),
  KEY idx_tenant_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话记录表';

-- 添加外键
ALTER TABLE ai_chat_log ADD CONSTRAINT fk_log_session FOREIGN KEY (session_id) REFERENCES ai_chat_session(session_id);
ALTER TABLE ai_chat_log ADD CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id);

-- =====================================================
-- 13. 操作日志表
-- =====================================================
CREATE TABLE operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
  operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  request_url VARCHAR(500) NOT NULL COMMENT '请求URL',
  request_method VARCHAR(10) NOT NULL COMMENT '请求方法',
  status TINYINT NOT NULL COMMENT '状态: 1-成功, 0-失败',
  error_message VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  response_time INT DEFAULT NULL COMMENT '响应耗时(ms)',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_tenant_id (tenant_id),
  KEY idx_request_url (request_url),
  KEY idx_status (status),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用日志表';

-- 添加外键
ALTER TABLE operation_log ADD CONSTRAINT fk_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id);

-- =====================================================
-- ==================== 初始化数据 ====================
-- =====================================================

-- 插入角色数据 (3个角色)
INSERT INTO sys_role (role_code, role_name, role_type, description) VALUES
('super_admin', '超级管理员', 1, '平台管理员，拥有最高权限'),
('tenant_admin', '租户管理员', 2, '租户账号所有者，负责租户管理、品牌/门店/账号管理'),
('shop_owner', '门店店长', 3, '门店负责人，负责看板查看、整改反馈、评价处理');

-- 插入租户数据
INSERT INTO tenant (tenant_code, tenant_name, status, contact_name, invite_code, api_quota) VALUES
('TN01', '租户1', 1, '管理员', 'T801704A1', 1000),
('TN02', '租户2', 1, '管理员', 'T404B6B03', 1000),
('TN03', '租户3', 1, '管理员', 'T3C17F1ED', 1000);

-- 插入门店数据
INSERT INTO shop (tenant_id, shop_code, shop_name, province, city, district, address, meituan_id, ele_id, status) VALUES
(1, 'SHOP001', '门店1', '广东省', '深圳市', '南山区', '科技园路100号', 'MT001', 'ELE001', 1),
(1, 'SHOP002', '门店2', '广东省', '广州市', '天河区', '天河路200号', 'MT002', 'ELE002', 1),
(2, 'SHOP003', '门店3', '北京市', '北京市', '朝阳区', '建国路300号', 'MT003', 'ELE003', 1);

-- 插入用户数据 (密码都是 123456，用BCrypt加密)
INSERT INTO sys_user (username, password, tenant_id, shop_id, role_code, status) VALUES
('admin', '$2a$10$eoHG43D.rmDVSCp/QRl42eV1B8oKsdXKLTtRj4pDgh41Pa5ER4toe', NULL, NULL, 'super_admin', 1),



('TN01_admin', '$2a$10$gspFPjZ5mMLQpAfYhU771.4PlCaF0xEMa5UbfdbXOx/Trks1.BMsu', 1, NULL, 'tenant_admin', 1),
('店长1', '$2a$10$IriAr7QQCVxD6Y14j7BlNeRdGm2ikUH70QqMU6aPTVXhkNGjlbfxa', 1, 1, 'shop_owner', 1),
('店长2', '$2a$10$HmwdE7cg9XR/QRYmB0LhPOB93WqHJV.ljuIHxqYC5dJiru7VyQjgi', 1, 2, 'shop_owner', 1),
('TN02_admin', '$2a$10$A2W3hi3qA.lrxnxIir658e7eIxMZ/VANcKYjwIEpP0AL2xpfEN8B2', 2, NULL, 'tenant_admin', 1);

-- 插入回复模板数据
INSERT INTO reply_template (tenant_id, name, content, sentiment_type, sort_order, status) VALUES
(1, '感谢好评-温暖版', '尊敬的顾客，感谢您的好评！您的认可是我们最大的动力，我们将继续努力，为您提供更优质的美食和服务，期待您的下次光临！', 1, 10, 1),
(1, '感谢好评-简洁版', '感谢您的五星好评！您的满意是我们永恒的追求，祝您用餐愉快，天天好心情！', 1, 9, 1),
(1, '中性反馈-改进版', '感谢您的反馈！我们非常重视每一位顾客的意见，已将您的建议转达给门店团队，未来会持续改进，希望下次能为您提供更好的体验！', 2, 10, 1),
(1, '中性反馈-关怀版', '感谢您的宝贵意见。我们会认真对待您的每一条建议，不断优化服务和品质，期待下次为您带来更满意的服务！', 2, 9, 1),
(1, '负面-道歉补救版', '非常抱歉给您带来不愉快的体验。我们非常重视您的反馈，已安排专人跟进处理，并将尽快改进。期待您下次光临，给我们一个为您更好服务的机会！', 3, 10, 1),
(1, '负面-诚恳整改版', '对不起，让您失望了。我们已将此问题记录在案，门店将立即整改。感谢您的批评，这帮助我们变得更好，欢迎您再次监督！', 3, 9, 1);

-- 插入评价标签数据（系统预置）
INSERT INTO review_tag (tenant_id, name, type) VALUES
(NULL, '包装破损', 'system'),
(NULL, '配送慢', 'system'),
(NULL, '味道一般', 'system'),
(NULL, '分量少', 'system'),
(NULL, '错漏餐', 'system'),
(NULL, '食材不新鲜', 'system'),
(NULL, '服务态度差', 'system'),
(NULL, '等餐时间长', 'system');

-- 插入自定义标签（租户1）
INSERT INTO review_tag (tenant_id, name, type) VALUES
(1, '优惠券问题', 'custom'),
(1, '新客体验', 'custom');

-- 插入评价数据
INSERT INTO review (platform, platform_order_id, platform_review_id, shop_id, tenant_id, star_rating, content, order_time, tags) VALUES
(1, 'MT20240115001', 'MT_REV_001', 1, 1, 5, '味道很棒！包装也很好，下次还来！', '2024-01-15 12:00:00', '[]'),
(1, 'MT20240115002', 'MT_REV_002', 1, 1, 2, '等餐太久了，包装也破了，不开心', '2024-01-15 13:00:00', '["包装破损", "等餐时间长"]'),
(2, 'ELE20240115001', 'ELE_REV_001', 1, 1, 4, '整体不错，就是有点咸', '2024-01-14 18:00:00', '["味道一般"]'),
(1, 'MT20240115003', 'MT_REV_003', 2, 1, 3, '中规中矩吧，没有特别惊艳', '2024-01-14 19:00:00', '["味道一般"]'),
(2, 'ELE20240115002', 'ELE_REV_002', 2, 1, 1, '完全错了，点的鸡翅给成了鸡腿，差评', '2024-01-13 20:00:00', '["错漏餐"]');

-- 插入工单数据
-- 插入工单类型数据（系统预置）
INSERT INTO ticket_type (tenant_id, name, type, is_default, support_review, sort_order) VALUES
(NULL, '差评预警', 'system', 1, 1, 10),
(NULL, '投诉预警', 'system', 1, 0, 9),
(NULL, '指标预警', 'system', 1, 0, 8),
(NULL, '自定义', 'system', 1, 0, 7);

-- 插入自定义工单类型（租户1）
INSERT INTO ticket_type (tenant_id, name, type, is_default, support_review, sort_order) VALUES
(1, '设备报修', 'custom', 0, 0, 6),
(1, '活动申请', 'custom', 0, 0, 5);

-- 插入邀请码记录数据
INSERT INTO invite_code_record (tenant_id, invite_code, status, expire_time, used_time, used_user_id) VALUES
(1, 'TENANT2026ABC123', 'unused', '2026-05-17 23:59:59', NULL, NULL);

-- 插入工单数据
INSERT INTO ticket (ticket_no, type_id, status, category, shop_id, tenant_id, title, description, detail_data, deadline, solution, review_id, creator_id, assignee_id, priority, images, suggestion) VALUES
('TK202401150001', 5, 2, 4, 1, 1, '门店打印机故障', '门店1的打印机无法正常工作，需要维修或更换', NULL, '2024-01-16 18:00:00', '已联系维修人员，明天上门', NULL, 2, 3, 'high', '[]', '建议先排查网络连接与驱动状态'),
('TK202401150002', 2, 3, 2, 1, 1, '希望增加优惠券功能', '建议增加优惠券发放功能，提升用户活跃度', NULL, '2024-01-20 18:00:00', '已提交产品需求评审', NULL, 2, 3, 'medium', '[]', '建议先进行AB测试，评估转化率提升'),
('TK202401150003', 3, 1, 2, 2, 1, 'POS系统无法登录', '门店2的收银系统无法登录，影响正常营业', '{"errorCode":"AUTH_TIMEOUT"}', '2024-01-17 12:00:00', NULL, NULL, 2, NULL, 'high', '[]', '建议先检查账号有效期与网络稳定性');

-- 插入工单时间线数据
INSERT INTO ticket_timeline (ticket_id, tenant_id, action, operator_id, remark) VALUES
(1, 1, 'created', 2, '创建工单'),
(1, 1, 'assigned', 2, '派单给店长1'),
(1, 1, 'process', 3, '已联系维修人员，明天上门'),
(2, 1, 'created', 2, '创建工单'),
(2, 1, 'assigned', 2, '派单给店长1');

-- =====================================================
-- AI聊天会话表
-- =====================================================
CREATE TABLE ai_chat_session (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  shop_id BIGINT DEFAULT NULL COMMENT '门店ID(店长角色有值)',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  title VARCHAR(100) DEFAULT NULL COMMENT '会话标题',
  context_summary TEXT COMMENT '对话上下文摘要',
  message_count_after_summary INT DEFAULT 0 COMMENT '摘要后消息数量',
  estimated_tokens INT DEFAULT 0 COMMENT '估算token数',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_tenant_id (tenant_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天会话表';

CREATE TABLE ai_chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  session_id BIGINT NOT NULL COMMENT '会话ID',
  role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system',
  content TEXT NOT NULL COMMENT '消息内容',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天消息表';

USE food_saas;
DROP TABLE IF EXISTS ai_chat_session;
DROP TABLE IF EXISTS ai_chat_message;
-- 然后重新执行 init.sql 的最后部分
CREATE TABLE ai_chat_session (
                                 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
                                 tenant_id BIGINT NOT NULL COMMENT '租户ID',
                                 shop_id BIGINT DEFAULT NULL COMMENT '门店ID(店长角色有值)',
                                 user_id BIGINT NOT NULL COMMENT '用户ID',
                                 title VARCHAR(100) DEFAULT NULL COMMENT '会话标题',
                                 context_summary TEXT COMMENT '对话上下文摘要',
                                 message_count_after_summary INT DEFAULT 0 COMMENT '摘要后消息数量',
                                 estimated_tokens INT DEFAULT 0 COMMENT '估算token数',
                                 created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (id),
                                 KEY idx_tenant_id (tenant_id),
                                 KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天会话表';

CREATE TABLE ai_chat_message (
                                 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                                 session_id BIGINT NOT NULL COMMENT '会话ID',
                                 role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system',
                                 content TEXT NOT NULL COMMENT '消息内容',
                                 created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 PRIMARY KEY (id),
                                 KEY idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天消息表';

ALTER TABLE ai_chat_session ADD COLUMN context_summary TEXT;
ALTER TABLE ai_chat_session ADD COLUMN message_count_after_summary INT DEFAULT 0;
ALTER TABLE ai_chat_session ADD COLUMN estimated_tokens INT DEFAULT 0;