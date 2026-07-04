package com.love.couplelife.controller;

import com.love.couplelife.common.Result;
import com.love.couplelife.dto.PhotoCommentDTO;
import com.love.couplelife.service.AlbumService;
import com.love.couplelife.vo.AlbumGroupVO;
import com.love.couplelife.vo.AlbumImageVO;
import com.love.couplelife.vo.PhotoCommentVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 相册控制器。
 * <p>
 * 业务模块：情侣共享相册，提供照片上传、按拍摄日期分组的照片列表查询，
 * 以及对单张照片的评论新增 / 查询能力。
 * </p>
 * <p>
 * 统一前缀路径：{@code /api/album}<br>
 * 鉴权要求：本控制器下所有接口均需登录（JWT 鉴权），并且通常要求当前用户已绑定情侣关系，
 * 照片归属当前情侣空间。
 * </p>
 */
@RestController
@RequestMapping("/api/album")
public class AlbumController {
    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    /**
     * 批量上传相册照片。
     * <p>HTTP: {@code POST /api/album/photos}（multipart/form-data）</p>
     *
     * @param files       照片文件数组，支持一次上传多张
     * @param shootDate   拍摄日期（格式 {@code yyyy-MM-dd}），用于按日期分组归档
     * @param title       可选，相册标题 / 主题
     * @param description 可选，相册描述
     * @return 上传成功后的照片列表 {@link AlbumImageVO}，含访问 URL
     */
    @PostMapping("/photos")
    public Result<List<AlbumImageVO>> upload(@RequestParam MultipartFile[] files,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shootDate,
                                             @RequestParam(required = false) String title,
                                             @RequestParam(required = false) String description) {
        return Result.success("上传成功", albumService.upload(files, shootDate, title, description));
    }

    /**
     * 查询当前情侣空间的全部相册照片，按拍摄日期分组返回。
     * <p>HTTP: {@code GET /api/album/photos}</p>
     *
     * @return 按日期分组的相册数据 {@link AlbumGroupVO} 列表
     */
    @GetMapping("/photos")
    public Result<List<AlbumGroupVO>> list() {
        return Result.success(albumService.list());
    }

    /**
     * 为指定照片新增评论。
     * <p>HTTP: {@code POST /api/album/photos/{photoId}/comments}</p>
     *
     * @param photoId 照片 ID
     * @param dto     评论内容封装（含评论文本等字段）
     * @return 新生成评论的 ID，键名固定为 {@code id}
     */
    @PostMapping("/photos/{photoId}/comments")
    public Result<Map<String, Long>> addComment(@PathVariable Long photoId, @Valid @RequestBody PhotoCommentDTO dto) {
        return Result.success("评论成功", albumService.addComment(photoId, dto));
    }

    /**
     * 查询指定照片下的全部评论。
     * <p>HTTP: {@code GET /api/album/photos/{photoId}/comments}</p>
     *
     * @param photoId 照片 ID
     * @return 评论列表 {@link PhotoCommentVO}
     */
    @GetMapping("/photos/{photoId}/comments")
    public Result<List<PhotoCommentVO>> comments(@PathVariable Long photoId) {
        return Result.success(albumService.comments(photoId));
    }

    /**
     * 批量导出相册照片为 ZIP 压缩包。
     * <p>HTTP: {@code POST /api/album/photos/export}（请求体为照片 id 数组 JSON）</p>
     * <p>响应：{@code application/zip}，作为附件下载；校验失败时由全局异常处理器返回 JSON。</p>
     *
     * @param photoIds 待导出的照片 id 列表，至少一项；非当前情侣或已软删的会被过滤掉
     * @param response Servlet 响应对象，由服务层在校验通过后设置头并写入压缩流
     */
    @PostMapping("/photos/export")
    public void exportPhotos(@RequestBody List<Long> photoIds, HttpServletResponse response) {
        albumService.exportPhotos(photoIds, response);
    }
}
