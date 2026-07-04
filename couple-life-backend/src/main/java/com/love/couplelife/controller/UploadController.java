package com.love.couplelife.controller;

import com.love.couplelife.common.Result;
import com.love.couplelife.util.FileUploadUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 通用文件上传控制器。
 * <p>
 * 业务模块：通用图片上传入口，供头像、相册、旅行配图等多个模块复用，
 * 通过 {@code module} 参数区分文件归属目录。
 * </p>
 * <p>
 * 统一前缀路径：{@code /api/upload}<br>
 * 鉴权要求：所有接口均需登录（JWT 鉴权）。
 * </p>
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {
    private final FileUploadUtil fileUploadUtil;

    public UploadController(FileUploadUtil fileUploadUtil) {
        this.fileUploadUtil = fileUploadUtil;
    }

    /**
     * 上传一张图片，返回可访问的 URL。
     * <p>HTTP: {@code POST /api/upload/image}（multipart/form-data）</p>
     *
     * @param file   待上传的图片文件
     * @param module 业务模块名（如 {@code avatar}、{@code album}、{@code travel}），
     *               默认值 {@code common}，用于决定存储子目录
     * @return 含字段 {@code url} 的 Map，值为图片访问 URL
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam MultipartFile file,
                                                   @RequestParam(defaultValue = "common") String module) {
        String url = fileUploadUtil.uploadImage(file, module);
        return Result.success("上传成功", Map.of("url", url));
    }
}
