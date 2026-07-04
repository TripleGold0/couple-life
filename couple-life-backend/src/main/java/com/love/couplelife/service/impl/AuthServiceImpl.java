package com.love.couplelife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.love.couplelife.common.BizException;
import com.love.couplelife.dto.CaptchaDTO;
import com.love.couplelife.dto.LoginDTO;
import com.love.couplelife.dto.RegisterDTO;
import com.love.couplelife.entity.LoginCaptcha;
import com.love.couplelife.entity.User;
import com.love.couplelife.mapper.LoginCaptchaMapper;
import com.love.couplelife.mapper.UserMapper;
import com.love.couplelife.service.AuthService;
import com.love.couplelife.util.JwtUtil;
import com.love.couplelife.vo.LoginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 鉴权服务实现：负责验证码下发、用户注册、密码/短信登录、JWT Token 颁发等。
 *
 * <p>协作组件：
 * <ul>
 *     <li>{@link UserMapper}：用户表 CRUD，登录时按用户名/手机号/邮箱多字段匹配</li>
 *     <li>{@link LoginCaptchaMapper}：验证码持久化（区分类型 LOGIN/REGISTER/IMAGE，含过期时间和 used 标记）</li>
 *     <li>{@link PasswordEncoder}：BCrypt 密码加密 / 校验</li>
 *     <li>{@link JwtUtil}：根据 userId 生成 Bearer Token</li>
 *     <li>{@link UserServiceImpl}：登录成功后用于把 {@link User} 转换为对外 VO</li>
 * </ul>
 *
 * <p>关键业务约束：
 * <ul>
 *     <li>验证码类型严格区分：短信登录(LOGIN) / 注册(REGISTER) / 图形(IMAGE)，不可串用</li>
 *     <li>同账号 + 类型存在冷却时间（默认 60s），避免接口被刷</li>
 *     <li>验证码一次性使用（used=1），过期时间 5 分钟（图形为 2 分钟）</li>
 *     <li>注册手机号、邮箱、用户名全局唯一</li>
 *     <li>短信登录支持"登录即注册"：首次手机号自动建号，profileCompleted=0 待完善</li>
 *     <li>账号 status=0 即被禁用，无法登录</li>
 *     <li>日志中所有账号都经过 {@link #maskAccount} 脱敏</li>
 * </ul>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String TYPE_LOGIN_SMS = "LOGIN";
    private static final String TYPE_REGISTER = "REGISTER";
    private static final String TYPE_IMAGE = "IMAGE";

    private static final String LOGIN_PASSWORD = "PASSWORD";
    private static final String LOGIN_SMS = "SMS_CODE";

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserMapper userMapper;
    private final LoginCaptchaMapper captchaMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserServiceImpl userService;
    /** 邮件发送器：仅当 spring.mail.username 已配置时才会被 Spring 装配，否则为 null。 */
    private final MailCaptchaSender mailCaptchaSender;

    @Value("${app.captcha.expose-code:false}")
    private boolean exposeCaptchaCode;

    @Value("${app.captcha.sms-cooldown-seconds:60}")
    private long smsCooldownSeconds;

    public AuthServiceImpl(UserMapper userMapper, LoginCaptchaMapper captchaMapper, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, UserServiceImpl userService,
                           ObjectProvider<MailCaptchaSender> mailCaptchaSenderProvider) {
        this.userMapper = userMapper;
        this.captchaMapper = captchaMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.mailCaptchaSender = mailCaptchaSenderProvider.getIfAvailable();
    }

    /**
     * 下发短信/邮件验证码（用于短信登录或注册）。
     *
     * <p>路由规则：
     * <ul>
     *     <li>账号是邮箱 → 调用 {@link MailCaptchaSender} 真实发送 HTML 邮件</li>
     *     <li>账号是手机号 → 当前未对接短信网关，仅记日志（生产需接入阿里云/腾讯云 SMS）</li>
     * </ul>
     * 当 {@code app.captcha.expose-code=true} 时，无论实际是否发送成功，都会在响应里回显验证码明文，
     * 便于 dev 联调或 SMTP 未配置场景。生产环境必须为 false。
     *
     * @param dto 包含账号（手机号/邮箱）和类型（LOGIN/REGISTER）
     * @return 开发环境下返回 captcha 明文；生产环境返回空 Map
     * @throws BizException 短信登录账号非手机号、或在冷却期内重复请求时
     */
    @Override
    public Map<String, String> sendCaptcha(CaptchaDTO dto) {
        // 账号格式校验：按业务类型路由
        // - 短信登录(LOGIN)：必须为手机号
        // - 注册(REGISTER)：手机号或邮箱二选一
        if (TYPE_LOGIN_SMS.equals(dto.getType())) {
            if (!PHONE_PATTERN.matcher(dto.getAccount()).matches()) {
                throw new BizException("短信登录验证码必须发送到手机号");
            }
        } else if (TYPE_REGISTER.equals(dto.getType())) {
            boolean isPhone = PHONE_PATTERN.matcher(dto.getAccount()).matches();
            boolean isEmail = EMAIL_PATTERN.matcher(dto.getAccount()).matches();
            if (!isPhone && !isEmail) {
                throw new BizException("请输入正确的手机号或邮箱");
            }
        } else {
            throw new BizException("不支持的验证码类型");
        }
        // 同账号 + 类型频控：上一条未过冷却期则拒绝
        LoginCaptcha last = captchaMapper.selectOne(new LambdaQueryWrapper<LoginCaptcha>()
                .eq(LoginCaptcha::getAccount, dto.getAccount())
                .eq(LoginCaptcha::getCaptchaType, dto.getType())
                .orderByDesc(LoginCaptcha::getId)
                .last("limit 1"));
        if (last != null && last.getCreateTime() != null
                && last.getCreateTime().plusSeconds(smsCooldownSeconds).isAfter(LocalDateTime.now())) {
            long wait = smsCooldownSeconds - java.time.Duration.between(last.getCreateTime(), LocalDateTime.now()).getSeconds();
            throw new BizException("操作过于频繁，请 " + Math.max(wait, 1) + " 秒后再试");
        }
        String code = String.valueOf(100000 + new Random().nextInt(900000));
        LoginCaptcha captcha = new LoginCaptcha();
        captcha.setAccount(dto.getAccount());
        captcha.setCaptchaType(dto.getType());
        captcha.setCaptchaCode(code);
        captcha.setExpireTime(LocalDateTime.now().plusMinutes(5));
        captcha.setUsed(0);
        captchaMapper.insert(captcha);
        log.info("验证码已生成 type={}, account={}", dto.getType(), maskAccount(dto.getAccount()));

        // 路由发送：邮箱走 SMTP，手机号目前仅日志（短信网关待接入）
        if (MailCaptchaSender.isEmail(dto.getAccount())) {
            if (mailCaptchaSender != null) {
                mailCaptchaSender.send(dto.getAccount(), code);
            } else {
                log.warn("邮箱验证码未真实发送：spring.mail.username 未配置，account={}", maskAccount(dto.getAccount()));
            }
        } else {
            // TODO: 对接短信网关（阿里云 dysmsapi / 腾讯云 SMS）
            log.warn("手机短信验证码未真实发送：短信网关未对接，account={}", maskAccount(dto.getAccount()));
        }

        // 生产环境（expose-code=false）不回显明文
        if (exposeCaptchaCode) {
            return Map.of("captcha", code);
        }
        return Map.of();
    }

    /**
     * 生成图形验证码（密码登录前置校验）。
     *
     * <p>使用随机生成的 captchaKey（UUID）作为账号字段持久化，避免与真实账号冲突；
     * 图片以 SVG + Base64 DataURI 形式直接返回，前端可直接 src 渲染，无需额外接口。
     *
     * @return captchaKey + captchaImage（data:image/svg+xml;base64,...）
     */
    @Override
    public Map<String, String> generateImageCaptcha() {
        String code = randomCode(4);
        String key = UUID.randomUUID().toString().replace("-", "");
        LoginCaptcha captcha = new LoginCaptcha();
        captcha.setAccount(key);
        captcha.setCaptchaType(TYPE_IMAGE);
        captcha.setCaptchaCode(code);
        captcha.setExpireTime(LocalDateTime.now().plusMinutes(2));
        captcha.setUsed(0);
        captchaMapper.insert(captcha);
        return Map.of(
                "captchaKey", key,
                "captchaImage", buildSvgDataUri(code)
        );
    }

    /** 生成指定长度的图形验证码字符（去掉了易混淆的 0/O/1/I）。 */
    private String randomCode(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 构造带干扰线和随机旋转的 SVG 图形验证码，转为 data URI 直接返回前端。
     */
    private String buildSvgDataUri(String code) {
        Random rnd = new Random();
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='120' height='40' viewBox='0 0 120 40'>");
        svg.append("<rect width='100%' height='100%' fill='#fff5f8'/>");
        // 干扰线
        for (int i = 0; i < 4; i++) {
            int x1 = rnd.nextInt(120), y1 = rnd.nextInt(40);
            int x2 = rnd.nextInt(120), y2 = rnd.nextInt(40);
            svg.append(String.format("<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='#ffb3c7' stroke-width='1'/>", x1, y1, x2, y2));
        }
        // 字符
        String[] colors = {"#ff6f9f", "#c2185b", "#7b1fa2", "#1976d2", "#388e3c"};
        for (int i = 0; i < code.length(); i++) {
            int x = 18 + i * 24;
            int y = 28 + rnd.nextInt(6) - 3;
            int rotate = rnd.nextInt(40) - 20;
            String color = colors[rnd.nextInt(colors.length)];
            svg.append(String.format(
                    "<text x='%d' y='%d' font-size='24' font-family='Arial' font-weight='bold' fill='%s' transform='rotate(%d %d %d)'>%s</text>",
                    x, y, color, rotate, x, y, code.charAt(i)));
        }
        svg.append("</svg>");
        String base64 = Base64.getEncoder().encodeToString(svg.toString().getBytes(StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + base64;
    }

    /**
     * 用户主动注册：手机号 / 邮箱二选一，需先调 {@link #sendCaptcha} 获取注册验证码。
     *
     * <p>事务内顺序：
     * <ol>
     *     <li>校验两次密码一致</li>
     *     <li>校验手机号或邮箱必填一项</li>
     *     <li>按"用户实际填写的账号"校验注册验证码（邮箱优先）</li>
     *     <li>校验用户名/手机号/邮箱唯一</li>
     *     <li>BCrypt 加密密码、生成全局唯一邀请码、入库</li>
     * </ol>
     *
     * @param dto 注册信息
     * @return 新用户主键 id
     * @throws BizException 校验失败时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BizException("两次输入的密码不一致");
        }
        // 手机号与邮箱二选一（至少填写一项）
        boolean hasPhone = dto.getPhone() != null && !dto.getPhone().isBlank();
        boolean hasEmail = dto.getEmail() != null && !dto.getEmail().isBlank();
        if (!hasPhone && !hasEmail) {
            throw new BizException("手机号和邮箱至少填写一项");
        }
        // 验证码发送至所填的账号（优先邮箱，否则手机号），与发送时使用的 account 一致
        String captchaAccount = hasEmail ? dto.getEmail() : dto.getPhone();
        verifyCaptcha(captchaAccount, TYPE_REGISTER, dto.getCaptcha());
        ensureUnique(dto);
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setGender(dto.getGender());
        user.setPhone(hasPhone ? dto.getPhone() : null);
        user.setEmail(hasEmail ? dto.getEmail() : null);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setInviteCode(generateInviteCode());
        user.setStatus(1);
        user.setProfileCompleted(1);
        user.setDeleted(0);
        userMapper.insert(user);
        log.info("用户注册成功 userId={}, username={}, account={}", user.getId(), dto.getUsername(), maskAccount(captchaAccount));
        return Map.of("userId", user.getId());
    }

    /**
     * 短信登录场景下手机号尚未注册：自动创建账号，资料待完善。
     * 仅写入手机号、随机用户名/昵称，密码、性别等留待 completeProfile 设置。
     */
    private User autoRegisterByPhone(String phone) {
        User user = new User();
        // 用户名以 phone 开头，避免与已有账号重复
        String username = "u_" + phone;
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, username))) {
            username = "u_" + phone + "_" + System.currentTimeMillis() % 10000;
        }
        user.setUsername(username);
        user.setNickname("用户" + phone.substring(7));
        user.setGender(0);
        user.setPhone(phone);
        // 邮箱、密码暂不填，待用户完善资料时设置
        user.setInviteCode(generateInviteCode());
        user.setStatus(1);
        user.setProfileCompleted(0);
        user.setDeleted(0);
        userMapper.insert(user);
        log.info("短信登录自动注册新用户 userId={}, phone={}", user.getId(), maskAccount(phone));
        return user;
    }

    /**
     * 生成全局唯一的 8 位 Base36 邀请码，用于情侣绑定。
     *
     * <p>最多重试 5 次，仍冲突则抛业务异常（极端情况下用户应重试）。
     */
    private String generateInviteCode() {
        for (int i = 0; i < 5; i++) {
            String code = Long.toString(Math.abs(new Random().nextLong()), 36).toUpperCase();
            if (code.length() > 8) {
                code = code.substring(0, 8);
            }
            if (!userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getInviteCode, code))) {
                return code;
            }
        }
        throw new BizException("邀请码生成失败，请重试");
    }

    /**
     * 登录入口，支持两种方式：
     * <ul>
     *     <li>{@code PASSWORD}：账号（用户名/手机号/邮箱任一）+ 密码 + 图形验证码</li>
     *     <li>{@code SMS_CODE}：手机号 + 短信验证码；账号不存在时自动注册（待完善资料）</li>
     * </ul>
     *
     * @param dto 登录请求
     * @return 包含 Bearer Token 与用户基础信息的 VO
     * @throws BizException 验证码错误、账号或密码错误、账号被禁用、登录方式不支持等场景
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        log.info("登录请求 loginType={}, account={}", dto.getLoginType(), maskAccount(dto.getAccount()));
        User user;
        if (LOGIN_PASSWORD.equalsIgnoreCase(dto.getLoginType())) {
            // 1. 校验图形验证码
            if (dto.getCaptchaKey() == null || dto.getCaptchaKey().isBlank()) {
                throw new BizException("图形验证码标识不能为空");
            }
            verifyCaptcha(dto.getCaptchaKey(), TYPE_IMAGE, dto.getCaptcha());
            // 2. 校验密码
            if (dto.getPassword() == null || dto.getPassword().isBlank()) {
                throw new BizException("密码不能为空");
            }
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, dto.getAccount())
                    .or()
                    .eq(User::getPhone, dto.getAccount())
                    .or()
                    .eq(User::getEmail, dto.getAccount())
                    .last("limit 1"));
            if (user == null) {
                throw new BizException("账号或密码错误");
            }
            // 短信登录自动建号但尚未完善资料的用户密码为 null，BCrypt#matches 不接受 null，统一按账号或密码错误处理
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new BizException("账号或密码错误");
            }
            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                throw new BizException("账号或密码错误");
            }
        } else if (LOGIN_SMS.equalsIgnoreCase(dto.getLoginType())) {
            // 短信登录：账号必须为手机号
            if (!PHONE_PATTERN.matcher(dto.getAccount()).matches()) {
                throw new BizException("请输入正确的手机号");
            }
            verifyCaptcha(dto.getAccount(), TYPE_LOGIN_SMS, dto.getCaptcha());
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, dto.getAccount())
                    .last("limit 1"));
            if (user == null) {
                // 短信登录即注册：自动创建账号，待完善资料
                user = autoRegisterByPhone(dto.getAccount());
            }
        } else {
            throw new BizException("不支持的登录方式");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("登录被拒：账号已禁用 userId={}", user.getId());
            throw new BizException("账号已被禁用");
        }
        LoginVO vo = new LoginVO();
        vo.setToken("Bearer " + jwtUtil.generateToken(user.getId()));
        vo.setUser(userService.toVO(user));
        log.info("登录成功 userId={}, loginType={}", user.getId(), dto.getLoginType());
        return vo;
    }

    /** 日志脱敏：手机号保留前 3 后 2，邮箱保留首字母 + 域名。 */
    private String maskAccount(String account) {
        if (account == null || account.isBlank()) {
            return "";
        }
        int len = account.length();
        if (len <= 3) {
            return "***";
        }
        if (account.contains("@")) {
            int at = account.indexOf('@');
            String prefix = at <= 2 ? "*" : account.charAt(0) + "***";
            return prefix + account.substring(at);
        }
        return account.substring(0, 3) + "****" + account.substring(Math.max(3, len - 2));
    }

    /**
     * 校验验证码：按 (账号, 类型, 验证码, used=0, 未过期) 取最新一条；通过后立即置 used=1 防重放。
     * <p>图形验证码不区分大小写（统一转大写比较）。
     */
    private void verifyCaptcha(String account, String type, String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("验证码不能为空");
        }
        LoginCaptcha captcha = captchaMapper.selectOne(new LambdaQueryWrapper<LoginCaptcha>()
                .eq(LoginCaptcha::getAccount, account)
                .eq(LoginCaptcha::getCaptchaType, type)
                .eq(LoginCaptcha::getCaptchaCode, TYPE_IMAGE.equals(type) ? code.toUpperCase() : code)
                .eq(LoginCaptcha::getUsed, 0)
                .ge(LoginCaptcha::getExpireTime, LocalDateTime.now())
                .orderByDesc(LoginCaptcha::getId)
                .last("limit 1"));
        if (captcha == null) {
            throw new BizException("验证码错误或已过期");
        }
        captcha.setUsed(1);
        captchaMapper.updateById(captcha);
    }

    /** 校验注册时用户名 / 手机号 / 邮箱三者唯一性，任一冲突即抛业务异常。 */
    private void ensureUnique(RegisterDTO dto) {
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()))) {
            throw new BizException("用户名已存在");
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank()
                && userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()))) {
            throw new BizException("手机号已注册");
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()))) {
            throw new BizException("邮箱已注册");
        }
    }
}
