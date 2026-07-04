CREATE DATABASE IF NOT EXISTS couple_life DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE couple_life;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（系统生成或注册时填写）',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0未知，1男，2女',
    phone VARCHAR(20) DEFAULT NULL UNIQUE COMMENT '手机号（手机号或邮箱二选一）',
    email VARCHAR(100) DEFAULT NULL UNIQUE COMMENT '邮箱（手机号或邮箱二选一）',
    password VARCHAR(255) DEFAULT NULL COMMENT '加密密码（短信登录注册时可暂为空，需在完善资料时设置）',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    birthday DATE DEFAULT NULL COMMENT '生日',
    invite_code VARCHAR(20) DEFAULT NULL COMMENT '邀请码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，0禁用',
    profile_completed TINYINT NOT NULL DEFAULT 1 COMMENT '资料是否完善：1已完善，0待完善（短信验证码首次登录注册时为0）',
    pet_display_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '个人侧是否展示悬浮宠物：1 是，0 否（仅影响自身端展示，不影响数据累计）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_invite_code (invite_code)
) COMMENT='用户表';

-- ===== 升级旧库（已存在 sys_user 时的兼容迁移） =====
-- MySQL 8 支持 IF NOT EXISTS 的部分语法因版本而异，下列语句在重复执行时可能报错，可手动按需执行：
-- ALTER TABLE sys_user MODIFY COLUMN phone VARCHAR(20) DEFAULT NULL;
-- ALTER TABLE sys_user MODIFY COLUMN email VARCHAR(100) DEFAULT NULL;
-- ALTER TABLE sys_user MODIFY COLUMN password VARCHAR(255) DEFAULT NULL;
-- ALTER TABLE sys_user ADD COLUMN profile_completed TINYINT NOT NULL DEFAULT 1 COMMENT '资料是否完善';
-- ALTER TABLE sys_user ADD COLUMN pet_display_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '个人侧是否展示悬浮宠物：1 是，0 否';

CREATE TABLE IF NOT EXISTS couple_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '情侣关系ID',
    couple_id BIGINT DEFAULT NULL COMMENT '情侣组共享ID（双方共用，绑定后回写）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    partner_id BIGINT NOT NULL COMMENT '伴侣用户ID',
    love_start_date DATE DEFAULT NULL COMMENT '恋爱开始日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，0解除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_partner (user_id, partner_id),
    INDEX idx_user_id (user_id),
    INDEX idx_partner_id (partner_id),
    INDEX idx_couple_id (couple_id)
) COMMENT='情侣关系表';

CREATE TABLE IF NOT EXISTS login_captcha (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account VARCHAR(100) NOT NULL COMMENT '手机号或邮箱',
    captcha_code VARCHAR(10) NOT NULL COMMENT '验证码',
    captcha_type VARCHAR(20) NOT NULL COMMENT '验证码类型',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_type (account, captcha_type)
) COMMENT='登录验证码表';

CREATE TABLE IF NOT EXISTS daily_checkin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    couple_id BIGINT DEFAULT NULL COMMENT '情侣关系ID',
    checkin_date DATE NOT NULL COMMENT '打卡日期',
    mood_emoji VARCHAR(20) NOT NULL COMMENT '心情表情',
    mood_text VARCHAR(100) DEFAULT NULL COMMENT '心情文字',
    content VARCHAR(500) DEFAULT NULL COMMENT '打卡内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, checkin_date),
    INDEX idx_couple_date (couple_id, checkin_date)
) COMMENT='每日打卡表';

CREATE TABLE IF NOT EXISTS travel_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    couple_id BIGINT NOT NULL COMMENT '情侣关系ID',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    location_name VARCHAR(100) NOT NULL COMMENT '地点名称',
    country VARCHAR(100) DEFAULT NULL COMMENT '国家',
    city VARCHAR(100) DEFAULT NULL COMMENT '城市',
    longitude DECIMAL(10,6) NOT NULL COMMENT '经度',
    latitude DECIMAL(10,6) NOT NULL COMMENT '纬度',
    travel_date DATE NOT NULL COMMENT '旅行日期',
    summary VARCHAR(255) DEFAULT NULL COMMENT '简短评论',
    detail TEXT DEFAULT NULL COMMENT '详细文字记录',
    my_feeling VARCHAR(1000) DEFAULT NULL COMMENT '我的感受',
    partner_feeling VARCHAR(1000) DEFAULT NULL COMMENT '伴侣感受',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_couple_id (couple_id),
    INDEX idx_travel_date (travel_date)
) COMMENT='旅游记录表';

CREATE TABLE IF NOT EXISTS travel_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    travel_id BIGINT NOT NULL COMMENT '旅游记录ID',
    image_url VARCHAR(255) NOT NULL COMMENT '图片地址',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_travel_id (travel_id)
) COMMENT='旅游图片表';

CREATE TABLE IF NOT EXISTS album_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    couple_id BIGINT NOT NULL COMMENT '情侣关系ID',
    uploader_id BIGINT NOT NULL COMMENT '上传人ID',
    image_url VARCHAR(255) NOT NULL COMMENT '图片地址',
    shoot_date DATE NOT NULL COMMENT '拍摄日期',
    title VARCHAR(100) DEFAULT NULL COMMENT '标题',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_couple_date (couple_id, shoot_date)
) COMMENT='相册图片表';

-- ===== 电子宠物模块 =====
-- 在 sys_user 表上新增 pet_display_enabled 字段（个人侧悬浮宠物开关，仅影响自身展示，不影响数据累计）
-- 升级旧库时手动执行：
--   ALTER TABLE sys_user ADD COLUMN pet_display_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '个人侧是否展示悬浮宠物：1 是，0 否';

-- 宠物种类（基础配置表，例如：猫、狗、兔子、龙、史莱姆等）
CREATE TABLE IF NOT EXISTS pet_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '宠物种类ID',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '类型唯一编码（如 CAT/DOG/RABBIT/DRAGON/SLIME）',
    name VARCHAR(50) NOT NULL COMMENT '展示名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '介绍/描述文案',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '图标 / 头像图片（可作前端列表缩略图）',
    sprite_url VARCHAR(255) DEFAULT NULL COMMENT '悬浮挂件主体动画/序列帧资源 URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序，数值越小越靠前',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否上架：1 上架，0 下架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='宠物种类配置表';

-- 内置默认宠物种类（首次部署即可使用），如已存在则无视
INSERT IGNORE INTO pet_type(code, name, description, sort_order, enabled) VALUES
  ('CAT',    '小猫咪',  '高冷又粘人的小猫，喜欢被抚摸',     1, 1),
  ('DOG',    '小狗狗',  '永远热情的好朋友，喜欢吃零食',     2, 1),
  ('RABBIT', '小兔子',  '安静温柔的兔子，胡萝卜爱好者',     3, 1),
  ('DRAGON', '小恐龙',  '蛋里孵出来的小恐龙，会喷火苗',     4, 1),
  ('SLIME',  '史莱姆',  'Q 弹的史莱姆，开心时会变色',       5, 1);

-- 情侣的宠物实例（每对情侣同一时刻仅有一只活跃宠物）
CREATE TABLE IF NOT EXISTS pet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '宠物实例ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID（来自 couple_relation.couple_id）',
    pet_type_id BIGINT NOT NULL COMMENT '宠物种类ID',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '宠物昵称（可选）',
    intimacy INT NOT NULL DEFAULT 0 COMMENT '亲密度（情侣共享，互动累计）',
    fullness INT NOT NULL DEFAULT 80 COMMENT '饱食度 0-100，每日衰减',
    mood INT NOT NULL DEFAULT 80 COMMENT '心情值 0-100，每日衰减',
    level INT NOT NULL DEFAULT 1 COMMENT '等级（依据亲密度自动成长）',
    stage VARCHAR(20) NOT NULL DEFAULT 'BABY' COMMENT '成长阶段：BABY/TEEN/ADULT',
    bound_date DATE NOT NULL COMMENT '宠物绑定（生效）日期，用于计算陪伴天数',
    last_decay_date DATE DEFAULT NULL COMMENT '最近一次属性衰减处理的日期，避免重复扣减',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 活跃，0 已弃养/被替换',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 注意：MySQL 不支持部分唯一索引，因此「同一情侣同一时刻仅允许一只活跃宠物」
    -- 由 Service 层（PetServiceImpl#agreeSelectionRequest）通过先 status=0 旧宠物再插入新宠物保证。
    INDEX idx_couple_status (couple_id, status)
) COMMENT='情侣宠物实例表';

-- 宠物互动日志（喂食 / 抚摸 / 玩耍）
CREATE TABLE IF NOT EXISTS pet_interaction_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pet_id BIGINT NOT NULL COMMENT '宠物实例ID',
    couple_id BIGINT NOT NULL COMMENT '情侣空间ID（冗余便于聚合）',
    user_id BIGINT NOT NULL COMMENT '互动操作的用户ID（双方任一）',
    action VARCHAR(20) NOT NULL COMMENT '互动类型：FEED/PET/PLAY',
    intimacy_delta INT NOT NULL DEFAULT 0 COMMENT '本次互动给宠物增加的亲密度',
    fullness_delta INT NOT NULL DEFAULT 0 COMMENT '本次互动给宠物增加的饱食度',
    mood_delta INT NOT NULL DEFAULT 0 COMMENT '本次互动给宠物增加的心情值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pet_id (pet_id),
    INDEX idx_user_action_date (user_id, action, create_time)
) COMMENT='宠物互动日志表';

-- 宠物选择请求（双方共同同意机制）
CREATE TABLE IF NOT EXISTS pet_selection_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    couple_id BIGINT NOT NULL COMMENT '情侣空间ID',
    requester_id BIGINT NOT NULL COMMENT '发起方用户ID',
    partner_id BIGINT NOT NULL COMMENT '需要确认的伴侣用户ID',
    pet_type_id BIGINT NOT NULL COMMENT '希望选择/更换的宠物种类ID',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '本次请求建议的宠物昵称',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/AGREED/REJECTED/EXPIRED',
    expire_time DATETIME NOT NULL COMMENT '请求过期时间（默认 24h）',
    decided_time DATETIME DEFAULT NULL COMMENT '伴侣同意/拒绝的时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_couple_status (couple_id, status),
    INDEX idx_partner_status (partner_id, status)
) COMMENT='宠物选择请求表（情侣共同同意机制）';

CREATE TABLE IF NOT EXISTS photo_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    photo_id BIGINT NOT NULL COMMENT '相册图片ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_photo_id (photo_id)
) COMMENT='图片评论表';
