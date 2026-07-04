package com.love.couplelife.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 邮件验证码发送器：把 6 位验证码以 HTML 邮件形式发到用户邮箱。
 *
 * <p>仅当 {@code spring.mail.username} 非空（区别于"存在但为空字符串"）时由 Spring 实例化，
 * 这样在没配置 SMTP 凭证的开发机上不会启动失败、也不会无意义地真去连 smtp。
 * {@link AuthServiceImpl} 通过 {@code ObjectProvider} 可选注入拿到本类实例。
 *
 * <p>注意：不能用 {@code @ConditionalOnProperty(name = "spring.mail.username")}，因为
 * {@code application.yml} 中 {@code username: ${MAIL_USERNAME:}} 在环境变量未设置时
 * 会解析成空字符串 ""，而 ConditionalOnProperty 默认把"存在且非 false"视为匹配，
 * 空串也会通过判定，导致装配出无凭证的 sender。
 *
 * <p>发送失败不抛异常（只记日志），原因：验证码已经入库，邮件偶发失败时由前端的"未收到？重新发送"
 * 流程兜底比让整个接口 5xx 更友好。
 */
@Component
@ConditionalOnExpression("'${spring.mail.username:}'.length() > 0")
public class MailCaptchaSender {

    private static final Logger log = LoggerFactory.getLogger(MailCaptchaSender.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.captcha.mail-from-name:情侣生活}")
    private String fromName;

    public MailCaptchaSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void init() {
        log.info("MailCaptchaSender 已启用，发件人={}", fromAddress);
    }

    /** 判断字符串是否为邮箱格式，调用方据此决定走邮件还是短信路径。 */
    public static boolean isEmail(String account) {
        return account != null && EMAIL_PATTERN.matcher(account).matches();
    }

    /**
     * 发送验证码邮件。失败仅记日志，不抛异常。
     *
     * @param to   收件人邮箱
     * @param code 验证码明文（6 位数字）
     * @return 是否发送成功
     */
    public boolean send(String to, String code) {
        // 双保险：理论上 ConditionalOnExpression 已确保非空，但避免配置错位时发出无效 From
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("跳过邮件发送：spring.mail.username 为空");
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            try {
                helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(fromAddress);
            }
            helper.setTo(to);
            helper.setSubject("【" + fromName + "】您的验证码");
            helper.setText(buildHtml(code), true);
            mailSender.send(message);
            log.info("验证码邮件发送成功 to={}", maskEmail(to));
            return true;
        } catch (Exception e) {
            log.error("验证码邮件发送失败 to={}, error={}", maskEmail(to), e.getMessage());
            return false;
        }
    }

    private String buildHtml(String code) {
        return "<div style=\"font-family:-apple-system,'PingFang SC',sans-serif;max-width:480px;margin:0 auto;"
                + "padding:32px 24px;background:#fff5f8;border-radius:12px;color:#333;\">"
                + "<h2 style=\"color:#c2185b;margin:0 0 16px;\">验证码</h2>"
                + "<p style=\"margin:0 0 16px;line-height:1.6;\">您正在进行身份验证，验证码为：</p>"
                + "<div style=\"font-size:32px;font-weight:bold;letter-spacing:8px;color:#ff6f9f;"
                + "background:#fff;padding:16px;text-align:center;border-radius:8px;margin:0 0 16px;\">"
                + code + "</div>"
                + "<p style=\"margin:0;color:#888;font-size:13px;line-height:1.6;\">"
                + "验证码 5 分钟内有效，请勿告知他人。如非本人操作，请忽略此邮件。</p>"
                + "</div>";
    }

    private String maskEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String prefix = at <= 2 ? "*" : email.charAt(0) + "***";
        return prefix + email.substring(at);
    }
}
