package com.flowwork.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.modules.auth.entity.SysUser;
import com.flowwork.modules.auth.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 启动时确保默认管理员可用：admin / 123456
 * （data.sql 中的 bcrypt 密文若与运行时编码器不匹配，此处会重置）
 * 并执行轻量 schema 迁移（新增表用 CREATE TABLE IF NOT EXISTS，兼容已有库）
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    /**
     * 启动时校验 admin 账号：不存在则创建，存在但密码不符则重置为 123456
     */
    @Bean
    public ApplicationRunner ensureAdmin(SysUserMapper userMapper, PasswordEncoder encoder) {
        return args -> {
            SysUser admin = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, "admin").last("LIMIT 1"));
            if (admin == null) {
                admin = new SysUser();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("123456"));
                admin.setNickname("管理员");
                admin.setEmail("admin@flowwork.dev");
                admin.setStatus(1);
                admin.setLoginFailCount(0);
                userMapper.insert(admin);
                userMapper.insertUserRole(admin.getId(), 1L);
                log.info("默认管理员已创建: admin / 123456");
            } else if (!encoder.matches("123456", admin.getPassword())) {
                admin.setPassword(encoder.encode("123456"));
                admin.setStatus(1);
                userMapper.updateById(admin);
                log.info("默认管理员密码已重置为: 123456");
            }
        };
    }

    /** 轻量迁移：notification 表（老库升级友好） */
    @Bean
    public ApplicationRunner notificationSchemaMigration(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS notification (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      user_id BIGINT NOT NULL,
                      type VARCHAR(50) NOT NULL,
                      title VARCHAR(100) NOT NULL,
                      content VARCHAR(500),
                      project_id BIGINT,
                      task_id BIGINT,
                      is_read TINYINT NOT NULL DEFAULT 0,
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      KEY idx_user_read (user_id, is_read),
                      KEY idx_task_dedup (task_id, type, created_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
        };
    }
}
