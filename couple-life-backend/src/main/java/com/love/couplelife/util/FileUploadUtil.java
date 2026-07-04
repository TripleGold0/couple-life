package com.love.couplelife.util;

import com.love.couplelife.common.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传工具类。
 * <p>
 * 落盘策略：{@code <upload-dir>/<module>/<yyyy-MM-dd>/<uuid>.<ext>}，
 * 按业务模块 + 日期分目录存储，便于后续清理与归档；返回的 URL 形如
 * {@code /uploads/<module>/<yyyy-MM-dd>/<uuid>.<ext>}，由 {@link com.love.couplelife.config.WebMvcConfig}
 * 暴露为静态资源。
 * <p>
 * 仅放行常见的图片 MIME 类型，文件名做白名单过滤防止路径穿越攻击。
 */
@Component
public class FileUploadUtil {
    private static final Logger log = LoggerFactory.getLogger(FileUploadUtil.class);
    /** 允许的 Content-Type 白名单 */
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    /** 上传根目录，由 application.yml 注入 */
    private final String uploadDir;

    public FileUploadUtil(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * 上传图片到指定业务模块目录。
     *
     * @param file   前端上传的文件
     * @param module 业务模块名（如 album、travel、avatar），用于分目录；非法字符会被过滤
     * @return 可直接用于前端访问的相对 URL
     * @throws BizException 文件为空、类型不合法或写盘失败
     */
    public String uploadImage(MultipartFile file, String module) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BizException("仅支持 JPG、PNG、WEBP、GIF 图片");
        }
        // 模块名仅保留字母数字、下划线、连字符，避免路径穿越
        String safeModule = module == null ? "common" : module.replaceAll("[^a-zA-Z0-9_-]", "");
        if (safeModule.isEmpty()) {
            safeModule = "common";
        }
        // 解析扩展名，未识别时默认 .jpg
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String suffix = ".jpg";
        int dotIndex = original.lastIndexOf('.');
        if (dotIndex > -1) {
            String ext = original.substring(dotIndex + 1).toLowerCase().replaceAll("[^a-z0-9]", "");
            if (Set.of("jpg", "jpeg", "png", "webp", "gif").contains(ext)) {
                suffix = "." + ext;
            }
        }
        // 用 UUID 重命名，避免重名覆盖与中文/特殊字符
        String filename = UUID.randomUUID() + suffix;
        String datePath = LocalDate.now().toString();
        Path targetDir = Path.of(uploadDir, safeModule, datePath);
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(filename).toAbsolutePath());
            String url = "/uploads/" + safeModule + "/" + datePath + "/" + filename;
            log.info("图片上传成功 module={}, original={}, size={}B, url={}", safeModule, original, file.getSize(), url);
            return url;
        } catch (IOException exception) {
            log.error("图片上传失败 module={}, original={}, size={}B", safeModule, original, file.getSize(), exception);
            throw new BizException("图片上传失败");
        }
    }

    /**
     * 把 {@link #uploadImage} 返回的相对 URL 解析回服务器本地磁盘路径。
     * <p>仅接受 {@code /uploads/} 前缀的 URL，并过滤路径穿越字符（{@code ..}），保证不会越过上传根目录。
     *
     * @param url 形如 {@code /uploads/<module>/<yyyy-MM-dd>/<uuid>.<ext>} 的访问 URL
     * @return 服务器本地绝对/相对路径
     * @throws BizException URL 为空或不在白名单前缀下
     */
    public Path resolveUrl(String url) {
        String prefix = "/uploads/";
        if (url == null || !url.startsWith(prefix)) {
            throw new BizException("非法图片地址");
        }
        String relative = url.substring(prefix.length());
        if (relative.contains("..")) {
            throw new BizException("非法图片地址");
        }
        return Path.of(uploadDir, relative);
    }
}
