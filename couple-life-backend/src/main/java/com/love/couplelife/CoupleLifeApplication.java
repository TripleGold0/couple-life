package com.love.couplelife;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * couple-life（情侣空间）后端应用启动类。
 * <p>
 * 本类是整个 Spring Boot 应用的入口，负责装配自动配置、组件扫描以及启动内嵌容器。
 * </p>
 *
 * <h3>关键依赖与能力</h3>
 * <ul>
 *   <li><b>Spring Boot</b>：通过 {@link SpringBootApplication} 启用自动配置、组件扫描和配置属性绑定。</li>
 *   <li><b>Spring Security + JWT</b>：负责接口鉴权与权限控制，所有 {@code /api/**} 业务接口默认要求登录态，
 *       仅 {@code /api/auth/**} 等白名单接口允许匿名访问；登录成功后通过 JWT 进行无状态认证。</li>
 *   <li><b>MyBatis-Plus</b>：通过 {@link MapperScan} 扫描 {@code com.love.couplelife.mapper} 包下的 Mapper 接口，
 *       提供 ORM、条件构造器、逻辑删除等能力。</li>
 *   <li><b>统一返回结构</b>：所有 Controller 返回 {@code Result<T>} 包装体，便于前端统一处理。</li>
 * </ul>
 *
 * <p>启动方式：执行 {@link #main(String[])} 或通过 {@code java -jar} 运行打包产物。</p>
 */
@MapperScan("com.love.couplelife.mapper")
@SpringBootApplication
@EnableScheduling
public class CoupleLifeApplication {

    /**
     * 应用入口方法，委托 {@link SpringApplication#run} 启动 Spring 上下文与内嵌 Web 容器。
     *
     * @param args JVM 命令行参数，可用于覆盖配置（例如 {@code --server.port=8081}）
     */
    public static void main(String[] args) {
        SpringApplication.run(CoupleLifeApplication.class, args);
    }
}
