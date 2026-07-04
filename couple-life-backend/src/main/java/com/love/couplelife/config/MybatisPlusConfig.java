package com.love.couplelife.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 * <p>目前仅启用分页插件（MySQL 方言）。如后续需要乐观锁、防全表更新等，可在此处追加 InnerInterceptor。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件。Mapper 层使用 IPage / Page 时需要此插件介入才能自动拼接 LIMIT 子句。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
