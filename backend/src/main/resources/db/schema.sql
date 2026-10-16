-- ============================================================
-- FlowWork 全量建表脚本  MySQL 8.0
-- ============================================================
CREATE DATABASE IF NOT EXISTS flowwork DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE flowwork;

-- ---------- 认证授权 ----------
DROP TABLE IF EXISTS sys_user_permission;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  username         VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录名',
  password         VARCHAR(128) NOT NULL COMMENT 'bcrypt 密码',
  email            VARCHAR(128) UNIQUE,
  phone            VARCHAR(32)  UNIQUE,
  nickname         VARCHAR(64)  NOT NULL DEFAULT '',
  avatar           VARCHAR(256) DEFAULT '',
  status           TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用 2锁定',
  locked_until     DATETIME     NULL,
  login_fail_count INT          NOT NULL DEFAULT 0,
  last_login_at    DATETIME     NULL,
  last_login_ip    VARCHAR(64)  NULL,
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code   VARCHAR(64)  NOT NULL UNIQUE,
  role_name   VARCHAR(64)  NOT NULL,
  description VARCHAR(256) DEFAULT '',
  is_system   TINYINT      NOT NULL DEFAULT 0,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_permission (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  permission_code VARCHAR(128) NOT NULL UNIQUE COMMENT 'module:resource:action',
  permission_name VARCHAR(64)  NOT NULL,
  category        VARCHAR(32)  DEFAULT '功能权限'
);

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sys_role_permission (
  role_id       BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE sys_user_permission (
  user_id       BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  type          TINYINT NOT NULL COMMENT '1白名单 2黑名单',
  PRIMARY KEY (user_id, permission_id, type)
);

-- ---------- 项目 / 看板 / 任务 ----------
DROP TABLE IF EXISTS task_comment;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS board_column;
DROP TABLE IF EXISTS project_member;
DROP TABLE IF EXISTS project;

CREATE TABLE project (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(128) NOT NULL,
  description TEXT,
  owner_id    BIGINT NOT NULL,
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '1ACTIVE 2ARCHIVED 3DELETED',
  start_date  DATE NULL,
  end_date    DATE NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_name_owner (name, owner_id)
);

CREATE TABLE project_member (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id   BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  project_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT 'MANAGER/MEMBER/OBSERVER',
  joined_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_project_user (project_id, user_id),
  KEY idx_pm_user (user_id)
);

CREATE TABLE board_column (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  name       VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status     TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_bc_project (project_id, sort_order)
);

CREATE TABLE task (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id      BIGINT NOT NULL,
  column_id       BIGINT NOT NULL,
  title           VARCHAR(200) NOT NULL,
  description     TEXT,
  assignee_id     BIGINT NULL,
  priority        TINYINT NOT NULL DEFAULT 2 COMMENT '1低 2中 3高 4紧急',
  estimated_hours DECIMAL(6,1) NULL,
  actual_hours    DECIMAL(6,1) NULL,
  due_date        DATE NULL,
  status          TINYINT NOT NULL DEFAULT 1 COMMENT '1待办 2进行中 3已完成 4已取消',
  parent_id       BIGINT NULL,
  sort_order      INT NOT NULL DEFAULT 0,
  ai_generated    TINYINT NOT NULL DEFAULT 0,
  ai_source       VARCHAR(32) NULL,
  created_by      BIGINT NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_task_project (project_id, column_id, sort_order),
  KEY idx_task_assignee (assignee_id),
  KEY idx_task_due (due_date)
);

CREATE TABLE task_comment (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id       BIGINT NOT NULL,
  user_id       BIGINT NOT NULL,
  content       VARCHAR(2000) NOT NULL,
  mentions_json JSON NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_comment_task (task_id, created_at)
);

-- ---------- 实时动态 ----------
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS activity;
CREATE TABLE activity (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id   BIGINT NOT NULL,
  task_id      BIGINT NULL,
  type         VARCHAR(40) NOT NULL,
  actor_id     BIGINT NOT NULL DEFAULT 0 COMMENT '0 表示系统/AI',
  actor_name   VARCHAR(64) DEFAULT '',
  content      TEXT NULL,
  ai_generated TINYINT NOT NULL DEFAULT 0,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_activity_project (project_id, created_at),
  KEY idx_activity_task (task_id)
);

-- ---------- 站内通知 ----------
CREATE TABLE notification (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL COMMENT '接收人',
  type        VARCHAR(50)  NOT NULL COMMENT 'TASK_ASSIGNED/TASK_COMMENTED/TASK_DUE_SOON/TASK_OVERDUE/MEMBER_JOINED',
  title       VARCHAR(100) NOT NULL,
  content     VARCHAR(500) NULL,
  project_id  BIGINT NULL,
  task_id     BIGINT NULL,
  is_read     TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notif_user (user_id, is_read, created_at),
  KEY idx_notif_task (task_id)
);

-- ---------- AI 对话历史 ----------
DROP TABLE IF EXISTS chat_message;
CREATE TABLE chat_message (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  role        VARCHAR(10) NOT NULL COMMENT 'user / ai',
  content     MEDIUMTEXT NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_chat_user (project_id, user_id, id)
);

-- ---------- 知识库 ----------
DROP TABLE IF EXISTS knowledge_doc;
CREATE TABLE knowledge_doc (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id      BIGINT NOT NULL,
  title           VARCHAR(256) NOT NULL,
  file_name       VARCHAR(256) NOT NULL,
  content_text    MEDIUMTEXT NULL,
  content_hash    VARCHAR(64) NOT NULL,
  vector_ids      VARCHAR(2000) NULL COMMENT 'VectorStore 文档 ID 列表(逗号分隔)',
  chunk_count     INT NULL,
  status          VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/READY/FAILED',
  error_msg       TEXT NULL,
  size_bytes      BIGINT NOT NULL,
  version         INT NOT NULL DEFAULT 1,
  created_by      BIGINT NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_kd_project (project_id, status)
);

-- ---------- AI ----------
DROP TABLE IF EXISTS ai_tool_call_log;
CREATE TABLE ai_tool_call_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id  BIGINT NULL,
  user_id     BIGINT NOT NULL,
  tool_name   VARCHAR(64) NOT NULL,
  tool_params TEXT NULL,
  tool_result TEXT NULL,
  duration_ms INT NOT NULL,
  success     TINYINT NOT NULL,
  error_msg   TEXT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ai_user (user_id, created_at),
  KEY idx_ai_project (project_id)
);

DROP TABLE IF EXISTS project_report;
CREATE TABLE project_report (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id   BIGINT NOT NULL,
  type         VARCHAR(32) NOT NULL COMMENT 'WEEKLY/RISK/CUSTOM',
  title        VARCHAR(256) NOT NULL,
  content      MEDIUMTEXT NOT NULL,
  ai_generated TINYINT NOT NULL DEFAULT 1,
  created_by   BIGINT NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_report_project (project_id, created_at)
);

-- ---------- 邀请码 ----------
DROP TABLE IF EXISTS invite_code;
CREATE TABLE invite_code (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  code                VARCHAR(64)  NOT NULL UNIQUE COMMENT '邀请码',
  preset_role         VARCHAR(30)  NOT NULL DEFAULT 'member' COMMENT '注册后授予的系统角色',
  preset_project_id   BIGINT NULL COMMENT '绑定项目(可空)',
  preset_project_role VARCHAR(20)  NULL COMMENT '项目内角色 MANAGER/MEMBER/OBSERVER',
  max_uses            INT NOT NULL DEFAULT 1 COMMENT '最大使用次数 0=无限',
  used_count          INT NOT NULL DEFAULT 0,
  expires_at          DATETIME NULL,
  created_by          BIGINT NOT NULL,
  status              VARCHAR(10)  NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_invite_code (code)
);

-- ---------- 注册申请 ----------
DROP TABLE IF EXISTS registration_application;
CREATE TABLE registration_application (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  applicant_name  VARCHAR(64)  NOT NULL COMMENT '申请人姓名',
  phone           VARCHAR(32)  NULL,
  email           VARCHAR(128) NULL,
  reason          VARCHAR(500) NOT NULL COMMENT '申请理由',
  status          VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  review_note     VARCHAR(500) NULL COMMENT '审批备注',
  invite_code     VARCHAR(64)  NULL COMMENT '审批通过后生成的邀请码',
  reviewed_by     BIGINT NULL,
  reviewed_at     DATETIME NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_reg_app_status (status, created_at)
);
