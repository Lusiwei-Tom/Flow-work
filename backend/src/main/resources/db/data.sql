-- ============================================================
-- FlowWork 初始化数据
-- 默认账号: admin / 123456 （bcrypt 密文见下）
-- ============================================================
USE flowwork;

-- 角色
INSERT INTO sys_role (id, role_code, role_name, description, is_system) VALUES
  (1, 'admin',           '超级管理员', '全部权限',           1),
  (2, 'project_manager', '项目管理员', '项目内管理权限',      1),
  (3, 'member',          '普通成员',   '仅读写自己负责的任务', 1);

-- 权限
INSERT INTO sys_permission (id, permission_code, permission_name, category) VALUES
  (1,  'auth:user:read',          '查看用户',   'auth'),
  (2,  'auth:user:manage',        '管理用户',   'auth'),
  (3,  'auth:role:manage',        '管理角色',   'auth'),
  (4,  'project:project:create',  '创建项目',   'project'),
  (5,  'project:project:read',    '查看项目',   'project'),
  (6,  'project:project:update',  '编辑项目',   'project'),
  (7,  'project:project:delete',  '删除项目',   'project'),
  (8,  'project:task:create',     '创建任务',   'project'),
  (9,  'project:task:read',       '查看任务',   'project'),
  (10, 'project:task:update',     '编辑任务',   'project'),
  (11, 'project:task:delete',     '删除任务',   'project'),
  (12, 'project:kanban:manage',   '管理看板',   'project'),
  (13, 'ai:agent:execute',        '执行AI管家', 'ai'),
  (14, 'ai:task:generate',        'AI任务拆解', 'ai'),
  (15, 'ai:meeting:parse',        'AI会议解析', 'ai'),
  (16, 'ai:report:generate',      'AI周报生成', 'ai'),
  (17, 'ai:risk:analyze',         'AI风险分析', 'ai'),
  (18, 'knowledge:doc:upload',    '上传文档',   'knowledge'),
  (19, 'knowledge:doc:read',      '查看文档',   'knowledge'),
  (20, 'knowledge:rag:query',     'RAG检索',    'knowledge'),
  (21, 'analytics:dashboard:read','查看数据看板','analytics'),
  (22, 'analytics:member:read',   '查看成员活跃度','analytics');

-- admin 拥有全部权限
INSERT INTO sys_role_permission (role_id, permission_id) SELECT 1, id FROM sys_permission;
-- project_manager
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
  (2,1),(2,4),(2,5),(2,6),(2,7),(2,8),(2,9),(2,10),(2,11),(2,12),
  (2,14),(2,15),(2,16),(2,17),(2,18),(2,19),(2,20),(2,21),(2,22);
-- member（含创建项目权限，新注册用户可自行建项目）
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
  (3,4),(3,5),(3,8),(3,9),(3,10),(3,19),(3,20);

-- 默认管理员 admin / 123456
-- bcrypt('123456') 由应用启动时若检测缺失也可重新生成；此处使用固定密文
INSERT INTO sys_user (id, username, password, email, nickname, status)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'admin@flowwork.dev', '管理员', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
