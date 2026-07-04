package com.love.couplelife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.love.couplelife.common.BizException;
import com.love.couplelife.dto.PhotoCommentDTO;
import com.love.couplelife.entity.AlbumImage;
import com.love.couplelife.entity.CoupleRelation;
import com.love.couplelife.entity.PhotoComment;
import com.love.couplelife.entity.User;
import com.love.couplelife.mapper.AlbumImageMapper;
import com.love.couplelife.mapper.CoupleRelationMapper;
import com.love.couplelife.mapper.PhotoCommentMapper;
import com.love.couplelife.mapper.UserMapper;
import com.love.couplelife.service.AlbumService;
import com.love.couplelife.util.FileUploadUtil;
import com.love.couplelife.util.SecurityUtil;
import com.love.couplelife.vo.AlbumGroupVO;
import com.love.couplelife.vo.AlbumImageVO;
import com.love.couplelife.vo.PhotoCommentVO;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 情侣相册服务实现。
 *
 * <p>职责：提供情侣共享相册的上传、按拍摄日期分组浏览、对照片发表/查询评论等能力。
 * <p>协作组件：
 * <ul>
 *     <li>{@link AlbumImageMapper}：相册图片表 CRUD（软删除：deleted=1 表示已删除）</li>
 *     <li>{@link PhotoCommentMapper}：照片评论表 CRUD（同样使用软删除）</li>
 *     <li>{@link CoupleRelationMapper}：用于校验当前用户是否已绑定情侣，并取出 coupleId</li>
 *     <li>{@link UserMapper}：评论列表中渲染用户昵称、头像</li>
 *     <li>{@link FileUploadUtil}：图片实际落盘/上传到对象存储，返回访问 URL</li>
 *     <li>{@link SecurityUtil}：从安全上下文获取当前登录用户 id</li>
 * </ul>
 * <p>关键业务约束：
 * <ul>
 *     <li>所有相册操作必须先绑定情侣关系（status=1），否则抛出 {@link BizException}</li>
 *     <li>相册图片与评论均为软删除（deleted 字段），查询时显式过滤 deleted=0</li>
 *     <li>按 coupleId 进行数据隔离，跨情侣的照片/评论不可见也不可操作</li>
 * </ul>
 */
@Service
public class AlbumServiceImpl implements AlbumService {
    private static final Logger log = LoggerFactory.getLogger(AlbumServiceImpl.class);

    private final AlbumImageMapper albumImageMapper;
    private final PhotoCommentMapper photoCommentMapper;
    private final CoupleRelationMapper coupleRelationMapper;
    private final UserMapper userMapper;
    private final FileUploadUtil fileUploadUtil;

    public AlbumServiceImpl(AlbumImageMapper albumImageMapper, PhotoCommentMapper photoCommentMapper, CoupleRelationMapper coupleRelationMapper, UserMapper userMapper, FileUploadUtil fileUploadUtil) {
        this.albumImageMapper = albumImageMapper;
        this.photoCommentMapper = photoCommentMapper;
        this.coupleRelationMapper = coupleRelationMapper;
        this.userMapper = userMapper;
        this.fileUploadUtil = fileUploadUtil;
    }

    /**
     * 批量上传相册照片。
     *
     * <p>会逐张调用 {@link FileUploadUtil#uploadImage} 上传到 album 业务目录，并写入数据库；
     * 整体处于事务中，任一图片入库失败都会回滚（已上传的文件本身不回滚，由清理任务负责）。
     *
     * @param files       前端上传的多张图片，至少一张
     * @param shootDate   拍摄日期，用于浏览时按日期分组
     * @param title       照片标题（可空，整批共用）
     * @param description 照片描述（可空，整批共用）
     * @return 入库后的图片 VO 列表
     * @throws BizException 当未传入文件、当前用户未绑定情侣关系时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AlbumImageVO> upload(MultipartFile[] files, LocalDate shootDate, String title, String description) {
        if (files == null || files.length == 0) {
            throw new BizException("请选择要上传的照片");
        }
        Long userId = SecurityUtil.currentUserId();
        Long coupleId = requireCoupleId(userId);
        List<AlbumImageVO> result = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = fileUploadUtil.uploadImage(file, "album");
            AlbumImage image = new AlbumImage();
            image.setCoupleId(coupleId);
            image.setUploaderId(userId);
            image.setImageUrl(url);
            image.setShootDate(shootDate);
            image.setTitle(title);
            image.setDescription(description);
            image.setDeleted(0);
            albumImageMapper.insert(image);
            result.add(toAlbumVO(image));
        }
        return result;
    }

    /**
     * 列出当前情侣的全部相册照片，按拍摄日期降序分组。
     *
     * <p>使用 {@link LinkedHashMap} 保留排序后的分组顺序：拍摄日期越新，分组越靠前；
     * 同一日期内按主键 id 倒序（即上传越晚越靠前）。
     *
     * @return 按日期分组的照片 VO 列表
     * @throws BizException 当前用户未绑定情侣关系时
     */
    @Override
    public List<AlbumGroupVO> list() {
        Long coupleId = requireCoupleId(SecurityUtil.currentUserId());
        List<AlbumImage> images = albumImageMapper.selectList(new LambdaQueryWrapper<AlbumImage>()
                .eq(AlbumImage::getCoupleId, coupleId)
                .eq(AlbumImage::getDeleted, 0)
                .orderByDesc(AlbumImage::getShootDate)
                .orderByDesc(AlbumImage::getId));
        Map<LocalDate, List<AlbumImageVO>> groupMap = new LinkedHashMap<>();
        for (AlbumImage image : images) {
            // 同一拍摄日期归到同一个分组，保留迭代顺序保证整体日期降序
            groupMap.computeIfAbsent(image.getShootDate(), key -> new ArrayList<>()).add(toAlbumVO(image));
        }
        return groupMap.entrySet().stream().map(entry -> {
            AlbumGroupVO group = new AlbumGroupVO();
            group.setDate(entry.getKey());
            group.setPhotos(entry.getValue());
            return group;
        }).toList();
    }

    /**
     * 给指定照片添加一条评论。
     *
     * @param photoId 照片 id；必须属于当前情侣并未被软删除
     * @param dto     评论内容载体
     * @return 新评论的主键 id
     * @throws BizException 照片不存在、不属于当前情侣或当前用户未绑定情侣时
     */
    @Override
    public Map<String, Long> addComment(Long photoId, PhotoCommentDTO dto) {
        requirePhoto(photoId);
        PhotoComment comment = new PhotoComment();
        comment.setPhotoId(photoId);
        comment.setUserId(SecurityUtil.currentUserId());
        comment.setContent(dto.getContent());
        comment.setDeleted(0);
        photoCommentMapper.insert(comment);
        return Map.of("id", comment.getId());
    }

    /**
     * 列出指定照片下的全部评论，按创建时间升序。
     *
     * @param photoId 照片 id
     * @return 评论 VO 列表（含评论者昵称、头像）
     * @throws BizException 照片不存在或不属于当前情侣时
     */
    @Override
    public List<PhotoCommentVO> comments(Long photoId) {
        requirePhoto(photoId);
        return photoCommentMapper.selectList(new LambdaQueryWrapper<PhotoComment>()
                        .eq(PhotoComment::getPhotoId, photoId)
                        .eq(PhotoComment::getDeleted, 0)
                        .orderByAsc(PhotoComment::getCreateTime))
                .stream().map(this::toCommentVO).toList();
    }

    /**
     * 批量导出当前情侣相册中的指定照片为 ZIP。
     *
     * <p>流程：
     * <ol>
     *     <li>校验入参不空、当前用户已绑定情侣；</li>
     *     <li>按 id + coupleId + 未软删 过滤照片，至少存在一张才继续；</li>
     *     <li>设置响应头（{@code application/zip} + 附件文件名）；</li>
     *     <li>逐张读取本地文件、写入 {@link ZipOutputStream}，文件名以
     *         {@code <拍摄日期>_<id>.<ext>} 格式命名，重复时追加序号。</li>
     * </ol>
     * 校验失败时不会动响应头或输出流，由全局异常处理器返回标准 JSON 错误。
     */
    @Override
    public void exportPhotos(List<Long> photoIds, HttpServletResponse response) {
        if (photoIds == null || photoIds.isEmpty()) {
            throw new BizException("请选择要导出的照片");
        }
        Long coupleId = requireCoupleId(SecurityUtil.currentUserId());
        List<AlbumImage> images = albumImageMapper.selectList(new LambdaQueryWrapper<AlbumImage>()
                .in(AlbumImage::getId, photoIds)
                .eq(AlbumImage::getCoupleId, coupleId)
                .eq(AlbumImage::getDeleted, 0));
        if (images.isEmpty()) {
            throw new BizException("没有可导出的照片");
        }
        // 通过校验后再设置响应头，避免错误场景下污染响应（让 GlobalExceptionHandler 正常输出 JSON）
        String filename = "album_" + LocalDate.now() + ".zip";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        Set<String> usedNames = new HashSet<>();
        // 注意：不要把 response.getOutputStream() 也放进 try-with-resources：
        // ZipOutputStream 关闭时会自动关闭它包装的 OutputStream，重复关闭无意义。
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (AlbumImage image : images) {
                Path path = fileUploadUtil.resolveUrl(image.getImageUrl());
                if (!Files.exists(path)) {
                    // 数据库有记录但文件丢失：跳过单张，避免整个导出失败
                    log.warn("导出时文件缺失，跳过 imageId={}, url={}", image.getId(), image.getImageUrl());
                    continue;
                }
                zos.putNextEntry(new ZipEntry(buildEntryName(image, path, usedNames)));
                Files.copy(path, zos);
                zos.closeEntry();
            }
        } catch (IOException exception) {
            // 此时 response 大概率已经 committed（headers + 部分 zip 字节已发出），
            // 再抛 BizException 会让 GlobalExceptionHandler 试图追加 JSON 到已损坏的 zip 流，
            // 反而产生无法解析的混合响应。这里仅记日志并优雅终止：客户端会拿到一个截断但
            // 大部分有效的 zip（或解压失败提示），可重试。
            log.error("批量导出 ZIP 写入失败 photoIds={}", photoIds, exception);
        }
    }

    /**
     * 为 ZIP 内的图片构造唯一文件名：{@code <拍摄日期>_<照片id>.<原扩展名>}；
     * 出现重名时追加 {@code _1 / _2 ...} 序号。
     */
    private String buildEntryName(AlbumImage image, Path path, Set<String> used) {
        String fname = path.getFileName().toString();
        int dot = fname.lastIndexOf('.');
        String ext = dot > -1 ? fname.substring(dot) : "";
        String base = image.getShootDate() + "_" + image.getId();
        String name = base + ext;
        int i = 1;
        while (!used.add(name)) {
            name = base + "_" + i + ext;
            i++;
        }
        return name;
    }

    /**
     * 校验照片存在且属于当前情侣，否则抛业务异常。
     */
    private AlbumImage requirePhoto(Long photoId) {
        Long coupleId = requireCoupleId(SecurityUtil.currentUserId());
        AlbumImage image = albumImageMapper.selectById(photoId);
        if (image == null || image.getDeleted() == 1 || !image.getCoupleId().equals(coupleId)) {
            throw new BizException("照片不存在");
        }
        return image;
    }

    /**
     * 取得当前用户已生效的情侣关系对应的 coupleId；未绑定则抛 BizException。
     */
    private Long requireCoupleId(Long userId) {
        CoupleRelation relation = coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getStatus, 1)
                .last("limit 1"));
        if (relation == null) {
            throw new BizException("请先绑定情侣关系");
        }
        return relation.getCoupleId();
    }

    /** 将 {@link AlbumImage} 实体转换为前端展示用 VO（不含敏感字段）。 */
    private AlbumImageVO toAlbumVO(AlbumImage image) {
        AlbumImageVO vo = new AlbumImageVO();
        vo.setId(image.getId());
        vo.setImageUrl(image.getImageUrl());
        vo.setTitle(image.getTitle());
        vo.setDescription(image.getDescription());
        return vo;
    }

    /** 将 {@link PhotoComment} 转换为 VO，并补齐评论者昵称和头像（用户被删时回退为"未知用户"）。 */
    private PhotoCommentVO toCommentVO(PhotoComment comment) {
        User user = userMapper.selectById(comment.getUserId());
        PhotoCommentVO vo = new PhotoCommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setNickname(user == null ? "未知用户" : user.getNickname());
        vo.setAvatar(user == null ? null : user.getAvatar());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
