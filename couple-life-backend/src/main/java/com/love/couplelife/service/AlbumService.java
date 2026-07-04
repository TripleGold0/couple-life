package com.love.couplelife.service;

import com.love.couplelife.dto.PhotoCommentDTO;
import com.love.couplelife.vo.AlbumGroupVO;
import com.love.couplelife.vo.AlbumImageVO;
import com.love.couplelife.vo.PhotoCommentVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 相册业务接口。
 * <p>所有方法都隐式作用于"当前登录用户所在的情侣空间"，未绑定情侣时按个人空间处理。</p>
 */
public interface AlbumService {

    /**
     * 批量上传照片。
     *
     * @param files       前端 multipart 文件数组
     * @param shootDate   拍摄日期，不传时默认当天
     * @param title       图片标题（可空，作用于全部新上传图片）
     * @param description 图片描述（可空）
     * @return 上传成功的图片 VO 列表
     */
    List<AlbumImageVO> upload(MultipartFile[] files, LocalDate shootDate, String title, String description);

    /**
     * 按拍摄日期倒序，分组返回相册照片。
     */
    List<AlbumGroupVO> list();

    /**
     * 给指定照片添加评论。
     *
     * @return 包含新评论 id 的 Map
     */
    Map<String, Long> addComment(Long photoId, PhotoCommentDTO dto);

    /**
     * 查询某张照片的全部评论（按时间正序）。
     */
    List<PhotoCommentVO> comments(Long photoId);

    /**
     * 批量导出指定照片为 ZIP 压缩包，写入到 HTTP 响应流中。
     * <p>会先校验照片归属当前情侣并未被软删除；通过校验后再设置 {@code Content-Type}
     * 与 {@code Content-Disposition} 响应头并开始流式输出，未通过校验时直接抛出
     * {@link com.love.couplelife.common.BizException}，由全局异常处理器返回 JSON。
     *
     * @param photoIds 要导出的照片 id 列表，至少一个
     * @param response 当前请求的 HTTP 响应
     */
    void exportPhotos(List<Long> photoIds, HttpServletResponse response);
}
