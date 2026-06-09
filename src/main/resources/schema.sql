CREATE TABLE IF NOT EXISTS child_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    child_name VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(2000),
    note_date DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS baby (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    avatar VARCHAR(500),
    gender VARCHAR(16),
    birth_date DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS baby_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    baby_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    is_owner TINYINT(1) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_baby_member_baby_user (baby_id, user_id),
    KEY idx_baby_member_user_status (user_id, status),
    KEY idx_baby_member_baby_status (baby_id, status)
);

CREATE TABLE IF NOT EXISTS child_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    baby_id BIGINT,
    record_type VARCHAR(32) NOT NULL,
    record_sub_type VARCHAR(32),
    record_date DATE NOT NULL,
    record_time TIMESTAMP NOT NULL,
    amount_ml INT,
    duration_sec INT,
    left_duration_sec INT,
    right_duration_sec INT,
    abnormal_flag TINYINT(1),
    temperature_value DECIMAL(5,2),
    height_cm DECIMAL(6,2),
    weight_kg DECIMAL(6,3),
    payload_json TEXT NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

SET @baby_user_id_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'baby'
      AND COLUMN_NAME = 'user_id'
);
SET @baby_user_id_sql = IF(@baby_user_id_count = 0, 'ALTER TABLE baby ADD COLUMN user_id BIGINT', 'SELECT 1');
PREPARE baby_user_id_stmt FROM @baby_user_id_sql;
EXECUTE baby_user_id_stmt;
DEALLOCATE PREPARE baby_user_id_stmt;

SET @child_record_user_id_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'child_record'
      AND COLUMN_NAME = 'user_id'
);
SET @child_record_user_id_sql = IF(@child_record_user_id_count = 0, 'ALTER TABLE child_record ADD COLUMN user_id BIGINT', 'SELECT 1');
PREPARE child_record_user_id_stmt FROM @child_record_user_id_sql;
EXECUTE child_record_user_id_stmt;
DEALLOCATE PREPARE child_record_user_id_stmt;

SET @child_record_baby_id_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'child_record'
      AND COLUMN_NAME = 'baby_id'
);
SET @child_record_baby_id_sql = IF(@child_record_baby_id_count = 0, 'ALTER TABLE child_record ADD COLUMN baby_id BIGINT', 'SELECT 1');
PREPARE child_record_baby_id_stmt FROM @child_record_baby_id_sql;
EXECUTE child_record_baby_id_stmt;
DEALLOCATE PREPARE child_record_baby_id_stmt;

SET @child_record_record_sub_type_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_record' AND COLUMN_NAME = 'record_sub_type'
);
SET @child_record_record_sub_type_sql = IF(@child_record_record_sub_type_count = 0, 'ALTER TABLE child_record ADD COLUMN record_sub_type VARCHAR(32)', 'SELECT 1');
PREPARE child_record_record_sub_type_stmt FROM @child_record_record_sub_type_sql;
EXECUTE child_record_record_sub_type_stmt;
DEALLOCATE PREPARE child_record_record_sub_type_stmt;

SET @child_record_amount_ml_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_record' AND COLUMN_NAME = 'amount_ml'
);
SET @child_record_amount_ml_sql = IF(@child_record_amount_ml_count = 0, 'ALTER TABLE child_record ADD COLUMN amount_ml INT', 'SELECT 1');
PREPARE child_record_amount_ml_stmt FROM @child_record_amount_ml_sql;
EXECUTE child_record_amount_ml_stmt;
DEALLOCATE PREPARE child_record_amount_ml_stmt;

SET @child_record_duration_sec_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_record' AND COLUMN_NAME = 'duration_sec'
);
SET @child_record_duration_sec_sql = IF(@child_record_duration_sec_count = 0, 'ALTER TABLE child_record ADD COLUMN duration_sec INT', 'SELECT 1');
PREPARE child_record_duration_sec_stmt FROM @child_record_duration_sec_sql;
EXECUTE child_record_duration_sec_stmt;
DEALLOCATE PREPARE child_record_duration_sec_stmt;

SET @child_record_left_duration_sec_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_record' AND COLUMN_NAME = 'left_duration_sec'
);
SET @child_record_left_duration_sec_sql = IF(@child_record_left_duration_sec_count = 0, 'ALTER TABLE child_record ADD COLUMN left_duration_sec INT', 'SELECT 1');
PREPARE child_record_left_duration_sec_stmt FROM @child_record_left_duration_sec_sql;
EXECUTE child_record_left_duration_sec_stmt;
DEALLOCATE PREPARE child_record_left_duration_sec_stmt;

SET @child_record_right_duration_sec_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_record' AND COLUMN_NAME = 'right_duration_sec'
);
SET @child_record_right_duration_sec_sql = IF(@child_record_right_duration_sec_count = 0, 'ALTER TABLE child_record ADD COLUMN right_duration_sec INT', 'SELECT 1');
PREPARE child_record_right_duration_sec_stmt FROM @child_record_right_duration_sec_sql;
EXECUTE child_record_right_duration_sec_stmt;
DEALLOCATE PREPARE child_record_right_duration_sec_stmt;

SET @child_record_abnormal_flag_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_record' AND COLUMN_NAME = 'abnormal_flag'
);
SET @child_record_abnormal_flag_sql = IF(@child_record_abnormal_flag_count = 0, 'ALTER TABLE child_record ADD COLUMN abnormal_flag TINYINT(1)', 'SELECT 1');
PREPARE child_record_abnormal_flag_stmt FROM @child_record_abnormal_flag_sql;
EXECUTE child_record_abnormal_flag_stmt;
DEALLOCATE PREPARE child_record_abnormal_flag_stmt;

SET @child_record_temperature_value_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_record' AND COLUMN_NAME = 'temperature_value'
);
SET @child_record_temperature_value_sql = IF(@child_record_temperature_value_count = 0, 'ALTER TABLE child_record ADD COLUMN temperature_value DECIMAL(5,2)', 'SELECT 1');
PREPARE child_record_temperature_value_stmt FROM @child_record_temperature_value_sql;
EXECUTE child_record_temperature_value_stmt;
DEALLOCATE PREPARE child_record_temperature_value_stmt;

SET @child_record_height_cm_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_record' AND COLUMN_NAME = 'height_cm'
);
SET @child_record_height_cm_sql = IF(@child_record_height_cm_count = 0, 'ALTER TABLE child_record ADD COLUMN height_cm DECIMAL(6,2)', 'SELECT 1');
PREPARE child_record_height_cm_stmt FROM @child_record_height_cm_sql;
EXECUTE child_record_height_cm_stmt;
DEALLOCATE PREPARE child_record_height_cm_stmt;

SET @child_record_weight_kg_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_record' AND COLUMN_NAME = 'weight_kg'
);
SET @child_record_weight_kg_sql = IF(@child_record_weight_kg_count = 0, 'ALTER TABLE child_record ADD COLUMN weight_kg DECIMAL(6,3)', 'SELECT 1');
PREPARE child_record_weight_kg_stmt FROM @child_record_weight_kg_sql;
EXECUTE child_record_weight_kg_stmt;
DEALLOCATE PREPARE child_record_weight_kg_stmt;

SET @child_record_deleted_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_record' AND COLUMN_NAME = 'deleted'
);
SET @child_record_deleted_sql = IF(@child_record_deleted_count = 0, 'ALTER TABLE child_record ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE child_record_deleted_stmt FROM @child_record_deleted_sql;
EXECUTE child_record_deleted_stmt;
DEALLOCATE PREPARE child_record_deleted_stmt;

SET @child_record_user_date_type_index_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'child_record'
      AND INDEX_NAME = 'idx_child_record_user_date_type'
);
SET @child_record_user_date_type_index_sql = IF(@child_record_user_date_type_index_count = 0, 'CREATE INDEX idx_child_record_user_date_type ON child_record(user_id, record_date, record_type)', 'SELECT 1');
PREPARE child_record_user_date_type_index_stmt FROM @child_record_user_date_type_index_sql;
EXECUTE child_record_user_date_type_index_stmt;
DEALLOCATE PREPARE child_record_user_date_type_index_stmt;

SET @child_record_baby_date_type_index_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'child_record'
      AND INDEX_NAME = 'idx_child_record_baby_date_type'
);
SET @child_record_baby_date_type_index_sql = IF(@child_record_baby_date_type_index_count = 0, 'CREATE INDEX idx_child_record_baby_date_type ON child_record(baby_id, record_date, record_type)', 'SELECT 1');
PREPARE child_record_baby_date_type_index_stmt FROM @child_record_baby_date_type_index_sql;
EXECUTE child_record_baby_date_type_index_stmt;
DEALLOCATE PREPARE child_record_baby_date_type_index_stmt;

SET @baby_user_id_index_count = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'baby'
      AND INDEX_NAME = 'idx_baby_user_id'
);
SET @baby_user_id_index_sql = IF(@baby_user_id_index_count = 0, 'CREATE INDEX idx_baby_user_id ON baby(user_id)', 'SELECT 1');
PREPARE baby_user_id_index_stmt FROM @baby_user_id_index_sql;
EXECUTE baby_user_id_index_stmt;
DEALLOCATE PREPARE baby_user_id_index_stmt;

CREATE TABLE IF NOT EXISTS ai_analysis_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    baby_id BIGINT NOT NULL,
    baby_name VARCHAR(64),
    range_start_date DATE NOT NULL,
    range_end_date DATE NOT NULL,
    source_text MEDIUMTEXT NOT NULL,
    skill_prompt MEDIUMTEXT NOT NULL,
    analysis_text MEDIUMTEXT NOT NULL,
    model VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_ai_analysis_user_baby_day (user_id, baby_id, range_end_date),
    KEY idx_ai_analysis_user_baby_time (user_id, baby_id, created_at),
    KEY idx_ai_analysis_baby_time (baby_id, created_at)
);

SET @ai_analysis_user_baby_day_index_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_analysis_record'
      AND INDEX_NAME = 'uk_ai_analysis_user_baby_day'
);
SET @ai_analysis_user_baby_day_index_sql = IF(@ai_analysis_user_baby_day_index_count = 0, 'CREATE UNIQUE INDEX uk_ai_analysis_user_baby_day ON ai_analysis_record(user_id, baby_id, range_end_date)', 'SELECT 1');
PREPARE ai_analysis_user_baby_day_index_stmt FROM @ai_analysis_user_baby_day_index_sql;
EXECUTE ai_analysis_user_baby_day_index_stmt;
DEALLOCATE PREPARE ai_analysis_user_baby_day_index_stmt;

CREATE TABLE IF NOT EXISTS ip_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip_address VARCHAR(64) NOT NULL,
    trigger_method VARCHAR(16),
    trigger_path VARCHAR(255),
    trigger_endpoint VARCHAR(300),
    request_count INT,
    window_started_at TIMESTAMP NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_ip_blacklist_ip_address (ip_address)
);

SET @ip_blacklist_ip_index_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ip_blacklist'
      AND INDEX_NAME = 'uk_ip_blacklist_ip_address'
);
SET @ip_blacklist_ip_index_sql = IF(@ip_blacklist_ip_index_count = 0, 'CREATE UNIQUE INDEX uk_ip_blacklist_ip_address ON ip_blacklist(ip_address)', 'SELECT 1');
PREPARE ip_blacklist_ip_index_stmt FROM @ip_blacklist_ip_index_sql;
EXECUTE ip_blacklist_ip_index_stmt;
DEALLOCATE PREPARE ip_blacklist_ip_index_stmt;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(128) NOT NULL,
    unionid VARCHAR(128),
    session_key VARCHAR(128),
    nick_name VARCHAR(128),
    avatar_url VARCHAR(500),
    gender INT,
    referrer_user_id BIGINT,
    referrer_bound_at TIMESTAMP NULL,
    token VARCHAR(1000),
    token_expire_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_app_user_openid (openid)
);

SET @app_user_token_index_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND INDEX_NAME = 'uk_app_user_token'
);
SET @app_user_token_index_sql = IF(@app_user_token_index_count = 0, 'SELECT 1', 'ALTER TABLE app_user DROP INDEX uk_app_user_token');
PREPARE app_user_token_index_stmt FROM @app_user_token_index_sql;
EXECUTE app_user_token_index_stmt;
DEALLOCATE PREPARE app_user_token_index_stmt;

UPDATE baby SET user_id = (SELECT id FROM app_user ORDER BY id LIMIT 1)
WHERE user_id IS NULL AND EXISTS (SELECT 1 FROM app_user);

INSERT INTO baby_member (baby_id, user_id, role_code, role_name, is_owner, status, created_at, updated_at)
SELECT b.id, b.user_id, 'guardian', '家人', 1, 'active', b.created_at, NOW()
FROM baby b
WHERE b.user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM baby_member bm
      WHERE bm.baby_id = b.id
        AND bm.user_id = b.user_id
  );

UPDATE child_record SET user_id = (SELECT id FROM app_user ORDER BY id LIMIT 1)
WHERE user_id IS NULL AND EXISTS (SELECT 1 FROM app_user);

UPDATE child_record cr
JOIN (
    SELECT user_id, MIN(id) AS baby_id
    FROM baby
    GROUP BY user_id
) first_baby ON first_baby.user_id = cr.user_id
SET cr.baby_id = first_baby.baby_id
WHERE cr.baby_id IS NULL;

ALTER TABLE app_user MODIFY COLUMN token VARCHAR(1000);

SET @app_user_referrer_user_id_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND COLUMN_NAME = 'referrer_user_id'
);
SET @app_user_referrer_user_id_sql = IF(@app_user_referrer_user_id_count = 0, 'ALTER TABLE app_user ADD COLUMN referrer_user_id BIGINT', 'SELECT 1');
PREPARE app_user_referrer_user_id_stmt FROM @app_user_referrer_user_id_sql;
EXECUTE app_user_referrer_user_id_stmt;
DEALLOCATE PREPARE app_user_referrer_user_id_stmt;

SET @app_user_referrer_bound_at_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND COLUMN_NAME = 'referrer_bound_at'
);
SET @app_user_referrer_bound_at_sql = IF(@app_user_referrer_bound_at_count = 0, 'ALTER TABLE app_user ADD COLUMN referrer_bound_at TIMESTAMP NULL', 'SELECT 1');
PREPARE app_user_referrer_bound_at_stmt FROM @app_user_referrer_bound_at_sql;
EXECUTE app_user_referrer_bound_at_stmt;
DEALLOCATE PREPARE app_user_referrer_bound_at_stmt;

SET @app_user_referrer_index_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND INDEX_NAME = 'idx_app_user_referrer_user_id'
);
SET @app_user_referrer_index_sql = IF(@app_user_referrer_index_count = 0, 'CREATE INDEX idx_app_user_referrer_user_id ON app_user(referrer_user_id)', 'SELECT 1');
PREPARE app_user_referrer_index_stmt FROM @app_user_referrer_index_sql;
EXECUTE app_user_referrer_index_stmt;
DEALLOCATE PREPARE app_user_referrer_index_stmt;

CREATE TABLE IF NOT EXISTS user_points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    points BIGINT NOT NULL DEFAULT 0,
    total_earned BIGINT NOT NULL DEFAULT 0,
    total_spent BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_user_points_user (user_id)
);

CREATE TABLE IF NOT EXISTS user_supplement_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_user_supplement_item_user_type_name (user_id, item_type, name),
    KEY idx_user_supplement_item_user_type (user_id, item_type, updated_at)
);

CREATE TABLE IF NOT EXISTS user_custom_vaccine (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    baby_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL DEFAULT 'paid',
    disease VARCHAR(128),
    dose_label VARCHAR(32),
    age_label VARCHAR(64),
    due_days INT,
    due_weeks INT,
    due_months INT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_user_custom_vaccine_baby_name (user_id, baby_id, name),
    KEY idx_user_custom_vaccine_user_baby (user_id, baby_id, updated_at)
);

CREATE TABLE IF NOT EXISTS sign_in_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sign_date DATE NOT NULL,
    sign_time TIMESTAMP NOT NULL,
    continuous_days INT NOT NULL,
    cycle_day INT NOT NULL,
    reward_points INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_sign_in_user_date (user_id, sign_date),
    KEY idx_sign_in_user_date (user_id, sign_date)
);

CREATE TABLE IF NOT EXISTS task_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    task_key VARCHAR(64) NOT NULL,
    related_user_id BIGINT,
    points INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'completed',
    payload_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    KEY idx_task_record_user_type (user_id, task_type),
    UNIQUE KEY uk_task_record_type_related_user (task_type, related_user_id)
);

CREATE TABLE IF NOT EXISTS lottery_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    cover_image VARCHAR(500),
    start_time TIMESTAMP NOT NULL,
    draw_time TIMESTAMP NOT NULL,
    cost_points INT NOT NULL DEFAULT 30,
    participant_count INT NOT NULL DEFAULT 0,
    winner_count INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    KEY idx_lottery_activity_status_draw (status, draw_time)
);

CREATE TABLE IF NOT EXISTS lottery_prize (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    prize_name VARCHAR(128) NOT NULL,
    prize_intro VARCHAR(500),
    prize_image VARCHAR(500),
    prize_count INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    KEY idx_lottery_prize_activity (activity_id)
);

CREATE TABLE IF NOT EXISTS lottery_participation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    cost_points INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'joined',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_lottery_participation_activity_user (activity_id, user_id),
    KEY idx_lottery_participation_user (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS discussion_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(64) NOT NULL,
    images_json TEXT,
    comment_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    KEY idx_discussion_post_category_time (category, created_at),
    KEY idx_discussion_post_user_time (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS discussion_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    KEY idx_discussion_comment_post_time (post_id, created_at),
    KEY idx_discussion_comment_user_time (user_id, created_at)
);

INSERT INTO lottery_activity (
    title, description, cover_image, start_time, draw_time, cost_points,
    participant_count, winner_count, status, created_at, updated_at
)
SELECT
    '每日积分抽奖',
    '使用积分参与成长礼包抽奖，开奖后可在历史抽奖中查看结果。',
    '/image/logo.png',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    30,
    0,
    3,
    'active',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM lottery_activity WHERE status = 'active'
);

INSERT INTO lottery_prize (
    activity_id, prize_name, prize_intro, prize_image, prize_count, created_at, updated_at
)
SELECT
    la.id,
    '亲子成长礼包',
    '含成长记录周边与宝宝护理小礼品，适合宝妈日常使用。',
    '/image/logo.png',
    la.winner_count,
    NOW(),
    NOW()
FROM lottery_activity la
WHERE la.status = 'active'
  AND NOT EXISTS (
      SELECT 1 FROM lottery_prize lp WHERE lp.activity_id = la.id
  )
ORDER BY la.id DESC
LIMIT 1;

CREATE TABLE IF NOT EXISTS baby_growth_stage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    start_day INT NOT NULL,
    end_day INT NOT NULL,
    stage_name VARCHAR(64) NOT NULL,
    subtitle VARCHAR(128) NOT NULL,
    physical_changes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_salt VARCHAR(128) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    display_name VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    token VARCHAR(256),
    token_expire_at TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_admin_account_username (username),
    KEY idx_admin_account_token (token)
);

CREATE TABLE IF NOT EXISTS admin_lottery_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(1000),
    cover_image VARCHAR(500),
    start_time TIMESTAMP NOT NULL,
    draw_time TIMESTAMP NOT NULL,
    cost_points INT NOT NULL DEFAULT 0,
    winner_count INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    publish_time TIMESTAMP NULL,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    KEY idx_admin_lottery_status_time (status, draw_time),
    KEY idx_admin_lottery_updated_at (updated_at)
);

CREATE TABLE IF NOT EXISTS admin_lottery_prize (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    prize_name VARCHAR(128) NOT NULL,
    prize_intro VARCHAR(500),
    prize_image VARCHAR(500),
    prize_count INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    KEY idx_admin_lottery_prize_activity (activity_id)
);

INSERT INTO baby_growth_stage (start_day, end_day, stage_name, subtitle, physical_changes) VALUES
(0,   7,   '新生儿适应期', '正在适应新世界~', '出生后体重暂时下降5%-10%，约7-10天恢复出生体重。脐带尚未脱落，需保持干燥。大部分时间在睡觉（16-20小时/天），有抓握反射和吸吮反射，能短暂抬头，视力模糊只能看清20-30cm内的物体。'),
(7,   28,  '新生儿感知期', '正在感知周围的一切~', '脐带在1-2周内脱落。开始出现社会性微笑（非反射性），视线开始追踪移动物体，对声音有反应，俯卧时能短暂抬头数秒，四肢活动增多，开始有昼夜节律。'),
(28,  60,  '抬头生长期', '正在努力抬头看世界~', '俯卧时能抬头45度，会发出"咕咕"声回应大人，会追视移动的人脸和物体，开始有社交微笑，睡眠时间减少到14-17小时/天，手脚活动更协调，可能开始流口水。'),
(60,  90,  '翻身准备期', '正在蓄力准备翻身~', '俯卧时能抬头90度，能笑出声，开始牙牙学语发出元音，能认出熟悉的家人，对陌生人有不同反应，手开始能张开不再总是握拳，能抓握摇铃等小玩具。'),
(90,  120, '翻身探索期', '正在翻身探索新姿势~', '多数宝宝在这个阶段学会从俯卧翻到仰卧，部分能从仰卧翻到俯卧。能用整个手抓东西往嘴里送，开始对镜子里的自己感兴趣，会大声笑，睡眠时间约14-15小时/天。'),
(120, 150, '抓握口欲期', '正在用嘴巴认识世界~', '翻身更加自如，开始有支撑地坐一会儿。手眼协调能力增强，能准确地伸手抓玩具，所有东西都往嘴里放（口欲期）。可能开始萌出第一颗乳牙（下门牙），流口水增多，开始认生。'),
(150, 180, '学坐萌牙期', '正在学坐和迎接小牙~', '能独立坐稳（不需支撑），开始腹部贴地匍匐爬行。下门牙通常已萌出或正在萌出，上门牙开始萌出。能模仿简单的声音和表情，会玩躲猫猫，开始添加辅食（铁强化米粉、菜泥等）。'),
(180, 210, '爬行探索期', '正在爬行征服每个角落~', '独坐更加稳定，能从趴着自己坐起来。多数宝宝开始手膝爬行，探索欲望强烈。能用拇指和食指捏起小物品（钳形抓握），会叫"爸爸""妈妈"但可能还不会对号入座，开始理解"不"的意思。'),
(210, 240, '扶站冒险期', '正在扶站开启新视野~', '爬行越来越熟练且速度快，能扶着家具自己站起来，部分宝宝开始扶着家具横着走（扶走）。会拍手、挥手拜拜等手势，分离焦虑高峰期（妈妈离开会哭），能用杯子喝水（需大人辅助），手指食物吃得越来越好。'),
(240, 270, '独立站初期', '正在练习独立站立~', '能扶着家具自如地走，部分宝宝能短暂独立站立几秒。能理解简单指令如"给我""过来"，会模仿大人的动作（打电话、梳头等），能有意识地叫爸爸妈妈，开始出现自主意识，对不想要的东西会摇头推开。'),
(270, 300, '学步启蒙期', '正在迈出人生第一步~', '能独立站得比较稳，牵着大人的手能往前走。部分宝宝开始独立走几步（10-12个月是学步关键期）。会用手指想要的物品，能说1-2个有意义的词，会用简单手势表达需求，开始自己用勺子吃饭的尝试。'),
(300, 330, '行走初期', '正在摇摇晃晃走起来~', '独立行走越来越稳，能从蹲下自己站起来。手部精细动作进一步发展，会搭2-3块积木，能自己翻绘本。会说2-5个词，能听懂更多指令，会模仿动物叫声，自我意识增强，开始有自己的小脾气。'),
(330, 365, '学步说话期', '正在学步和牙牙学语~', '走路更加自信，部分宝宝能小跑几步。语言快速发育，会说5-10个词，能用单字或叠词表达需求（"抱""水水"等）。会自己用杯子喝水、拿勺子吃饭（虽然会弄得到处都是），开始出现假想游戏（假装喂娃娃等）。'),
(365, 9999,'幼儿探索期', '正在探索更大的世界~', '1岁以后进入幼儿期，大运动能力持续发展（跑、跳、爬楼梯），语言进入爆发期，社交能力增强，开始有独立意识，能听懂复杂指令，精细动作和认知能力快速发展。每个宝宝的发育节奏不同，以上仅为大致的参考阶段。');
