package com.chuctupk.service.impl;

import com.chuctupk.common.PhoneValidator;
import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.dto.request.*;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.repository.*;
import com.chuctupk.service.AdminService;
import com.chuctupk.service.SequenceGeneratorService;
import com.chuctupk.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ArticleRepository articleRepository;
    private final ArticleCategoryRepository articleCategoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final Clock clock;

    @Lazy
    private final StaffService staffService;

    private static final Path UPLOADS_DIR = Paths.get("uploads").toAbsolutePath().normalize();
    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    // =========================================================================
    // 1. DASHBOARD TỔNG QUAN
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(clock);

        List<Appointment> todayAppts = appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(today);
        int apptTotal = todayAppts.size();
        int apptActive = (int) todayAppts.stream().filter(a -> a.getStatus() != AppointmentStatus.CANCELLED).count();

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        byStatus.put("BOOKED", (int) todayAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).count());
        byStatus.put("CHECKED_IN", (int) todayAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count());
        byStatus.put("IN_PROGRESS", (int) todayAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.IN_PROGRESS).count());
        byStatus.put("COMPLETED", (int) todayAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count());
        byStatus.put("CANCELLED", (int) todayAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count());

        long patientTotal = patientRepository.count();

        List<Article> allArticles = articleRepository.findAll();
        int publishedPosts = (int) allArticles.stream().filter(a -> Boolean.TRUE.equals(a.getIsPublished())).count();
        int draftPosts = (int) allArticles.stream().filter(a -> !Boolean.TRUE.equals(a.getIsPublished())).count();

        List<User> allUsers = userRepository.findAll();
        int userCount = (int) allUsers.stream().filter(u -> u.getRole() == Role.PATIENT).count();
        int staffCount = (int) allUsers.stream().filter(u -> u.getRole() == Role.STAFF).count();
        int doctorCount = (int) allUsers.stream().filter(u -> u.getRole() == Role.DOCTOR).count();
        int adminCount = (int) allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();
        Map<String, Integer> accounts = Map.of("USER", userCount, "RECEPTIONIST", staffCount, "DOCTOR", doctorCount, "ADMIN", adminCount);

        List<PostDto> drafts = allArticles.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsPublished()))
                .sorted(Comparator.comparing(Article::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(PostDto::from)
                .toList();

        QueueStateDto queue = staffService != null ? staffService.getQueueState() : null;

        return new AdminDashboardResponse(
                today.toString(),
                new AdminDashboardResponse.AdminDashboardAppointments(apptTotal, apptActive, byStatus),
                new AdminDashboardResponse.AdminDashboardPatients(patientTotal),
                new AdminDashboardResponse.AdminDashboardPosts(publishedPosts, draftPosts, 0),
                accounts,
                drafts,
                queue
        );
    }

    // =========================================================================
    // 2. QUẢN LÝ TÀI KHOẢN HỆ THỐNG
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public AdminAccountListResponse getAccounts(String q, String role, int page, int limit) {
        int pageSize = limit > 0 ? Math.min(limit, 100) : 10;
        List<User> all = userRepository.findAll();

        Map<String, Integer> roleCounts = new LinkedHashMap<>();
        roleCounts.put("USER", (int) all.stream().filter(u -> u.getRole() == Role.PATIENT).count());
        roleCounts.put("RECEPTIONIST", (int) all.stream().filter(u -> u.getRole() == Role.STAFF).count());
        roleCounts.put("DOCTOR", (int) all.stream().filter(u -> u.getRole() == Role.DOCTOR).count());
        roleCounts.put("ADMIN", (int) all.stream().filter(u -> u.getRole() == Role.ADMIN).count());

        List<User> filtered = all.stream()
                .filter(u -> {
                    if (role == null || role.isBlank()) return true;
                    String feRole = ContractMapper.toFeRole(u.getRole());
                    return role.equalsIgnoreCase(feRole);
                })
                .filter(u -> {
                    if (q == null || q.isBlank()) return true;
                    String query = q.trim().toLowerCase();
                    return (u.getFullName() != null && u.getFullName().toLowerCase().contains(query))
                            || (u.getPhone() != null && u.getPhone().contains(query))
                            || (u.getUsername() != null && u.getUsername().toLowerCase().contains(query))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query));
                })
                .sorted(Comparator.comparing(User::getId).reversed())
                .toList();

        int total = filtered.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int curPage = Math.max(1, Math.min(page, pages));
        int fromIndex = (curPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<AccountDto> items = fromIndex < total
                ? filtered.subList(fromIndex, toIndex).stream().map(AccountDto::from).toList()
                : List.of();

        return new AdminAccountListResponse(items, total, pages, curPage, pageSize, roleCounts);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto getAccount(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));
        return AccountDto.from(u);
    }

    @Override
    @Transactional
    public AccountDto createAccount(AdminCreateAccountRequest request) {
        String cleanPhone = PhoneValidator.validate(request.phone());
        if (userRepository.existsByPhone(cleanPhone)) {
            throw AppException.of(ErrorCode.USER_EXISTS, "Số điện thoại này đã được sử dụng");
        }

        Role dbRole = ContractMapper.toDbRole(request.role());

        boolean isStaff = dbRole == Role.STAFF || dbRole == Role.ADMIN || dbRole == Role.DOCTOR;
        if (isStaff) {
            if (request.email() == null || request.email().isBlank()) {
                throw AppException.badRequest("Tài khoản nội bộ bắt buộc phải có email");
            }
            if (request.tempPassword() == null || request.tempPassword().length() < 8) {
                throw AppException.badRequest("Mật khẩu tạm cần ít nhất 8 ký tự");
            }
        }

        String username = (isStaff ? (dbRole == Role.ADMIN ? "admin_" : "staff_") : "patient_") + cleanPhone;
        if (userRepository.existsByUsername(username)) {
            username = username + "_" + System.currentTimeMillis() % 1000;
        }

        String encodedPassword = request.tempPassword() != null && !request.tempPassword().isBlank()
                ? passwordEncoder.encode(request.tempPassword())
                : passwordEncoder.encode(UUID.randomUUID().toString());

        User user = User.builder()
                .username(username)
                .fullName(request.fullName().trim())
                .phone(cleanPhone)
                .email(request.email() != null && !request.email().isBlank() ? request.email().trim().toLowerCase() : "")
                .role(dbRole)
                .passwordHash(encodedPassword)
                .isActive(true)
                .mustChangePassword(isStaff)
                .build();

        User saved = userRepository.save(user);

        if (!isStaff) {
            int year = (request.birthYear() != null && request.birthYear() >= 1900 && request.birthYear() <= LocalDate.now(clock).getYear())
                    ? request.birthYear()
                    : 1970;
            Gender gender = request.gender() != null ? ContractMapper.toDbGender(request.gender()) : Gender.MALE;

            Patient p = Patient.builder()
                    .patientCode(sequenceGeneratorService.nextPatientCode())
                    .user(saved)
                    .fullName(saved.getFullName())
                    .phone(saved.getPhone())
                    .yearOfBirth(year)
                    .gender(gender)
                    .relationship("BAN_THAN")
                    .isPrimary(true)
                    .isActive(true)
                    .infoUpdatedAt(LocalDateTime.now(clock))
                    .build();
            patientRepository.save(p);
        }

        return AccountDto.from(saved);
    }

    @Override
    @Transactional
    public AccountDto updateAccount(Long id, AdminUpdateAccountRequest request) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));

        if (request.expectedUpdatedAt() != null && !request.expectedUpdatedAt().isBlank()) {
            String current = u.getUpdatedAt() != null ? ContractMapper.formatIso(u.getUpdatedAt()) : "";
            if (!current.equals(request.expectedUpdatedAt())) {
                throw AppException.of(ErrorCode.STALE, "Dữ liệu đã được người khác cập nhật trước đó. Vui lòng tải lại trang");
            }
        }

        if (request.fullName() != null && !request.fullName().isBlank()) {
            u.setFullName(request.fullName().trim());
        }

        if (request.phone() != null && !request.phone().isBlank()) {
            String clean = PhoneValidator.validate(request.phone());
            if (!clean.equals(u.getPhone()) && userRepository.existsByPhone(clean)) {
                throw AppException.of(ErrorCode.USER_EXISTS, "Số điện thoại này đã được sử dụng bởi tài khoản khác");
            }
            u.setPhone(clean);
        }

        if (request.email() != null) {
            u.setEmail(request.email().trim().toLowerCase());
        }

        u.setUpdatedAt(LocalDateTime.now(clock));
        User saved = userRepository.save(u);

        // Đồng bộ hồ sơ BAN_THAN nếu có
        patientRepository.findByUserIdAndIsPrimaryTrue(saved.getId()).ifPresent(p -> {
            p.setFullName(saved.getFullName());
            p.setPhone(saved.getPhone());
            patientRepository.save(p);
        });

        return AccountDto.from(saved);
    }

    @Override
    @Transactional
    public AccountDto changeRole(Long id, AdminChangeRoleRequest request) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));

        Role targetRole = ContractMapper.toDbRole(request.role());

        boolean upgradingToStaff = targetRole == Role.STAFF || targetRole == Role.ADMIN || targetRole == Role.DOCTOR;
        if (upgradingToStaff) {
            boolean hasEmail = (u.getEmail() != null && !u.getEmail().isBlank())
                    || (request.email() != null && !request.email().isBlank());
            if (!hasEmail) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Tài khoản nội bộ bắt buộc phải có email", Map.of("code", "EMAIL_REQUIRED"));
            }

            boolean hasPassword = (u.getPasswordHash() != null && !u.getPasswordHash().isBlank())
                    || (request.tempPassword() != null && !request.tempPassword().isBlank());
            if (!hasPassword) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Tài khoản nội bộ bắt buộc phải có mật khẩu tạm", Map.of("code", "PASSWORD_REQUIRED"));
            }

            if (request.email() != null && !request.email().isBlank()) {
                u.setEmail(request.email().trim().toLowerCase());
            }

            if (request.tempPassword() != null && !request.tempPassword().isBlank()) {
                if (request.tempPassword().length() < 8) {
                    throw AppException.badRequest("Mật khẩu tạm cần ít nhất 8 ký tự");
                }
                u.setPasswordHash(passwordEncoder.encode(request.tempPassword()));
                u.setMustChangePassword(true);
            }
        }

        u.setRole(targetRole);
        u.setUpdatedAt(LocalDateTime.now(clock));
        User saved = userRepository.save(u);

        return AccountDto.from(saved);
    }

    @Override
    @Transactional
    public AccountDto setAccountActive(User adminUser, Long id, boolean active) {
        if (!active && adminUser != null && adminUser.getId().equals(id)) {
            throw AppException.of(ErrorCode.CANNOT_LOCK_SELF, "Quản trị viên không thể tự khóa tài khoản của chính mình");
        }

        User u = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));

        u.setIsActive(active);
        u.setUpdatedAt(LocalDateTime.now(clock));
        User saved = userRepository.save(u);

        return AccountDto.from(saved);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, AdminResetPasswordRequest request) {
        if (request.tempPassword() == null || request.tempPassword().length() < 8) {
            throw AppException.badRequest("Mật khẩu tạm cần ít nhất 8 ký tự");
        }

        User u = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));

        u.setPasswordHash(passwordEncoder.encode(request.tempPassword()));
        u.setMustChangePassword(true);
        u.setUpdatedAt(LocalDateTime.now(clock));
        userRepository.save(u);
    }

    @Override
    @Transactional
    public PatientDto setPatientActive(Long patientId, boolean active) {
        Patient p = patientRepository.findById(patientId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy hồ sơ người bệnh"));

        p.setIsActive(active);
        p.setLockedAt(active ? null : LocalDateTime.now(clock));
        p.setInfoUpdatedAt(LocalDateTime.now(clock));
        Patient saved = patientRepository.save(p);

        return PatientDto.from(saved);
    }

    // =========================================================================
    // 3. QUẢN LÝ BÀI VIẾT (CMS)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public AdminPostListResponse getAdminPosts(String q, String category, String status, int page, int limit) {
        int pageSize = limit > 0 ? Math.min(limit, 100) : 10;
        List<Article> all = articleRepository.findAll();

        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        statusCounts.put("PUBLISHED", (int) all.stream().filter(a -> Boolean.TRUE.equals(a.getIsPublished())).count());
        statusCounts.put("DRAFT", (int) all.stream().filter(a -> !Boolean.TRUE.equals(a.getIsPublished())).count());
        statusCounts.put("HIDDEN", 0);

        List<Article> filtered = all.stream()
                .filter(a -> {
                    if (status == null || status.isBlank()) return true;
                    boolean isPub = Boolean.TRUE.equals(a.getIsPublished());
                    return "PUBLISHED".equalsIgnoreCase(status) ? isPub : !isPub;
                })
                .filter(a -> {
                    if (category == null || category.isBlank()) return true;
                    return a.getCategory() != null && category.equalsIgnoreCase(a.getCategory().getSlug());
                })
                .filter(a -> {
                    if (q == null || q.isBlank()) return true;
                    String query = q.trim().toLowerCase();
                    return (a.getTitle() != null && a.getTitle().toLowerCase().contains(query))
                            || (a.getSummary() != null && a.getSummary().toLowerCase().contains(query));
                })
                .sorted(Comparator.comparing(Article::getIsPinned, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Article::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int total = filtered.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int curPage = Math.max(1, Math.min(page, pages));
        int fromIndex = (curPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<PostDto> items = fromIndex < total
                ? filtered.subList(fromIndex, toIndex).stream().map(PostDto::from).toList()
                : List.of();

        return new AdminPostListResponse(items, total, pages, curPage, pageSize, statusCounts);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto getAdminPost(Long id) {
        Article a = articleRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bài viết"));
        return PostDto.from(a);
    }

    @Override
    @Transactional
    public PostDto createAdminPost(User author, AdminSavePostRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw AppException.badRequest("Vui lòng nhập tiêu đề bài viết");
        }

        String content = sanitizeHtml(request.content());
        String summary = request.excerpt() != null && !request.excerpt().isBlank()
                ? request.excerpt().trim()
                : (request.summary() != null ? request.summary().trim() : "");

        ArticleCategory cat = resolveCategory(request.category());
        String slug = toSlug(request.title().trim());

        if (articleRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        boolean pinned = Boolean.TRUE.equals(request.pinned());
        if (pinned) {
            unpinAllOtherArticles(null);
        }

        boolean published = "PUBLISHED".equalsIgnoreCase(request.status());

        User writer = author != null ? author : userRepository.findByUsername("admin").orElse(null);
        if (writer == null) {
            writer = userRepository.findAll().stream().findFirst().orElseThrow(() -> AppException.notFound("Không tìm thấy tác giả"));
        }

        Article article = Article.builder()
                .title(request.title().trim())
                .slug(slug)
                .summary(summary)
                .content(content)
                .thumbnailUrl(request.coverImage())
                .category(cat)
                .author(writer)
                .isPublished(published)
                .isPinned(pinned)
                .publishedAt(published ? LocalDateTime.now(clock) : null)
                .viewCount(0)
                .build();

        Article saved = articleRepository.save(article);
        return PostDto.from(saved);
    }

    @Override
    @Transactional
    public PostDto updateAdminPost(Long id, AdminSavePostRequest request) {
        Article a = articleRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bài viết"));

        if (request.title() != null && !request.title().isBlank()) {
            a.setTitle(request.title().trim());
        }

        if (request.content() != null) {
            a.setContent(sanitizeHtml(request.content()));
        }

        if (request.excerpt() != null) {
            a.setSummary(request.excerpt().trim());
        } else if (request.summary() != null) {
            a.setSummary(request.summary().trim());
        }

        if (request.coverImage() != null) {
            a.setThumbnailUrl(request.coverImage());
        }

        if (request.category() != null) {
            a.setCategory(resolveCategory(request.category()));
        }

        if (request.pinned() != null) {
            if (request.pinned()) {
                unpinAllOtherArticles(a.getId());
            }
            a.setIsPinned(request.pinned());
        }

        if (request.status() != null && !request.status().isBlank()) {
            boolean published = "PUBLISHED".equalsIgnoreCase(request.status());
            if (published && !Boolean.TRUE.equals(a.getIsPublished())) {
                a.setPublishedAt(LocalDateTime.now(clock));
            }
            a.setIsPublished(published);
        }

        Article saved = articleRepository.save(a);
        return PostDto.from(saved);
    }

    @Override
    @Transactional
    public PostDto setPostPin(Long id, boolean pin) {
        Article a = articleRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bài viết"));

        if (pin) {
            unpinAllOtherArticles(id);
        }
        a.setIsPinned(pin);
        Article saved = articleRepository.save(a);
        return PostDto.from(saved);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        if (!articleRepository.existsById(id)) {
            throw AppException.notFound("Không tìm thấy bài viết để xoá");
        }
        articleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicPostListResponse getPublicPosts(String category, int page, int limit, boolean includeFeatured) {
        int pageSize = limit > 0 ? Math.min(limit, 50) : 9;
        List<Article> published = articleRepository.findAll().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsPublished()))
                .filter(a -> category == null || category.isBlank() || (a.getCategory() != null && category.equalsIgnoreCase(a.getCategory().getSlug())))
                .sorted(Comparator.comparing(Article::getIsPinned, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Article::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Article featuredArt = null;
        if (includeFeatured && !published.isEmpty()) {
            featuredArt = published.stream().filter(a -> Boolean.TRUE.equals(a.getIsPinned())).findFirst()
                    .orElse(published.get(0));
        }

        final Long featuredId = featuredArt != null ? featuredArt.getId() : null;
        List<Article> rest = published.stream()
                .filter(a -> featuredId == null || !a.getId().equals(featuredId))
                .toList();

        int total = rest.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int curPage = Math.max(1, Math.min(page, pages));
        int fromIndex = (curPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<PostDto> items = fromIndex < total
                ? rest.subList(fromIndex, toIndex).stream().map(PostDto::from).toList()
                : List.of();

        return new PublicPostListResponse(items, PostDto.from(featuredArt), total, pages, curPage, pageSize);
    }

    @Override
    @Transactional
    public PostDto getPublicPost(String slugOrId) {
        Article a = null;
        try {
            Long id = Long.valueOf(slugOrId);
            a = articleRepository.findById(id).orElse(null);
        } catch (NumberFormatException ignored) {}

        if (a == null) {
            a = articleRepository.findBySlug(slugOrId).orElse(null);
        }

        if (a == null || !Boolean.TRUE.equals(a.getIsPublished())) {
            throw AppException.notFound("Không tìm thấy bài viết");
        }

        a.setViewCount(a.getViewCount() != null ? a.getViewCount() + 1 : 1);
        Article saved = articleRepository.save(a);
        return PostDto.from(saved);
    }

    // =========================================================================
    // 4. LƯU TRỮ VÀ TẢI ẢNH MEDIA (≤ 3MB, WHITELIST MIME)
    // =========================================================================

    @Override
    public Map<String, Object> saveUpload(String contentType, byte[] data) {
        if (data == null || data.length == 0) {
            throw AppException.badRequest("Tệp tải lên không được rỗng");
        }

        if (data.length > 3 * 1024 * 1024) {
            throw AppException.badRequest("Ảnh quá lớn. Vui lòng chọn ảnh dưới 3 MB");
        }

        String mime = contentType != null ? contentType.toLowerCase().split(";")[0].trim() : "image/jpeg";
        if (!ALLOWED_MIME.contains(mime)) {
            throw AppException.badRequest("Định dạng ảnh không được hỗ trợ. Chỉ chấp nhận JPG, PNG, WEBP hoặc GIF");
        }

        String ext = switch (mime) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "jpg";
        };

        String filename = UUID.randomUUID().toString() + "." + ext;

        try {
            if (!Files.exists(UPLOADS_DIR)) {
                Files.createDirectories(UPLOADS_DIR);
            }
            Path targetFile = UPLOADS_DIR.resolve(filename).normalize();
            if (!targetFile.startsWith(UPLOADS_DIR)) {
                throw AppException.badRequest("Đường dẫn tệp không hợp lệ");
            }
            Files.write(targetFile, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Lỗi khi ghi tệp tải lên", e);
            throw AppException.internal("Không thể lưu tệp tải lên, vui lòng thử lại sau");
        }

        return Map.of("id", filename, "url", "/api/v1/uploads/" + filename);
    }

    @Override
    public byte[] getUpload(String id) {
        Path target = UPLOADS_DIR.resolve(id).normalize();
        if (!target.startsWith(UPLOADS_DIR) || !Files.exists(target)) {
            throw AppException.notFound("Không tìm thấy tệp ảnh");
        }

        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw AppException.notFound("Không thể đọc tệp ảnh");
        }
    }

    @Override
    public String getUploadContentType(String id) {
        String lower = id.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void unpinAllOtherArticles(Long excludeId) {
        List<Article> pinned = articleRepository.findByIsPinnedTrue();
        for (Article p : pinned) {
            if (excludeId == null || !p.getId().equals(excludeId)) {
                p.setIsPinned(false);
                articleRepository.save(p);
            }
        }
    }

    private ArticleCategory resolveCategory(String catSlug) {
        if (catSlug != null && !catSlug.isBlank()) {
            return articleCategoryRepository.findBySlug(catSlug.trim())
                    .orElseGet(() -> articleCategoryRepository.findAll().stream().findFirst().orElse(null));
        }
        return articleCategoryRepository.findAll().stream().findFirst().orElse(null);
    }

    private String sanitizeHtml(String html) {
        if (html == null) return "";
        if (html.length() > 100_000) {
            throw AppException.badRequest("Nội dung bài viết không được vượt quá 100.000 ký tự");
        }

        // Lọc các thẻ độc hại script, iframe, object, embed và event handlers
        return html
                .replaceAll("(?i)<script[^>]*>[\\s\\S]*?</script>", "")
                .replaceAll("(?i)<iframe[^>]*>[\\s\\S]*?</iframe>", "")
                .replaceAll("(?i)<object[^>]*>[\\s\\S]*?</object>", "")
                .replaceAll("(?i)<embed[^>]*>[\\s\\S]*?</embed>", "")
                .replaceAll("(?i)\\s*on[a-zA-Z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", "")
                .replaceAll("(?i)javascript:", "");
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
