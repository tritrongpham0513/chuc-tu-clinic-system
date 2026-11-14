package com.chuctupk.config;

import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Tự động nạp dữ liệu nền tảng (8 khung giờ 60p, cài đặt phòng khám, bác sĩ, tiếp tân)
 * khi kết nối vào bất kỳ Database mới nào (như Aiven hay Cloud MySQL).
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class DataSeederRunner implements ApplicationRunner {

    private final TimeSlotRepository timeSlotRepository;
    private final ClinicSettingRepository clinicSettingRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final ArticleCategoryRepository articleCategoryRepository;
    private final ArticleRepository articleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedTimeSlots();
        seedClinicSettings();
        seedStaffAndDoctor();
        seedArticles();
    }

    private void seedTimeSlots() {
        if (timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc().isEmpty()) {
            log.info("Bảng time_slots chưa có dữ liệu khả dụng. Đang tự động nạp 8 khung giờ chuẩn 60 phút...");
            List<TimeSlot> slots = List.of(
                    TimeSlot.builder().slotLabel("08:00 - 09:00").startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0)).period(Period.MORNING).maxPatients(6).sortOrder(1).isActive(true).build(),
                    TimeSlot.builder().slotLabel("09:00 - 10:00").startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).period(Period.MORNING).maxPatients(6).sortOrder(2).isActive(true).build(),
                    TimeSlot.builder().slotLabel("10:00 - 11:00").startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).period(Period.MORNING).maxPatients(6).sortOrder(3).isActive(true).build(),
                    TimeSlot.builder().slotLabel("11:00 - 12:00").startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(12, 0)).period(Period.MORNING).maxPatients(6).sortOrder(4).isActive(true).build(),
                    TimeSlot.builder().slotLabel("13:00 - 14:00").startTime(LocalTime.of(13, 0)).endTime(LocalTime.of(14, 0)).period(Period.AFTERNOON).maxPatients(6).sortOrder(5).isActive(true).build(),
                    TimeSlot.builder().slotLabel("14:00 - 15:00").startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(15, 0)).period(Period.AFTERNOON).maxPatients(6).sortOrder(6).isActive(true).build(),
                    TimeSlot.builder().slotLabel("15:00 - 16:00").startTime(LocalTime.of(15, 0)).endTime(LocalTime.of(16, 0)).period(Period.AFTERNOON).maxPatients(6).sortOrder(7).isActive(true).build(),
                    TimeSlot.builder().slotLabel("16:00 - 17:00").startTime(LocalTime.of(16, 0)).endTime(LocalTime.of(17, 0)).period(Period.AFTERNOON).maxPatients(6).sortOrder(8).isActive(true).build()
            );
            timeSlotRepository.saveAll(slots);
            log.info("Đã khởi tạo thành công 8 khung giờ khám 60 phút.");
        }
    }

    private void seedClinicSettings() {
        saveSettingIfMissing("slot_capacity", "6", "Số bệnh nhân tối đa trong 1 khung giờ");
        saveSettingIfMissing("seats", "5", "Số ghế khám thực tế trong phòng");
        saveSettingIfMissing("slot_minutes", "60", "Thời lượng mỗi khung giờ khám (phút)");
        saveSettingIfMissing("user_max_days", "30", "Số ngày người bệnh được phép đặt trước tối đa");
        saveSettingIfMissing("counter_max_days", "7", "Số ngày tiếp tân đặt hộ tại quầy tối đa");
        saveSettingIfMissing("priority_early_minutes", "15", "Số phút đến sớm trước khung giờ để được xếp nhóm Ưu tiên");
        saveSettingIfMissing("max_profiles", "10", "Số hồ sơ người thân tối đa được tạo trên 1 tài khoản");
        saveSettingIfMissing("auto_invite", "0", "Tự động mời người kế tiếp vào ghế khi có ghế trống (1: bật, 0: tắt)");
        saveSettingIfMissing("reception_open", "08:00", "Giờ mở cửa tiếp nhận");
        saveSettingIfMissing("reception_close", "16:30", "Giờ đóng cửa tiếp nhận");
        saveSettingIfMissing("no_show_cancel_at", "17:00", "Giờ hệ thống tự động hủy các lịch chưa đến trong ngày");
    }

    private void saveSettingIfMissing(String key, String val, String desc) {
        if (!clinicSettingRepository.existsById(key)) {
            clinicSettingRepository.save(ClinicSetting.builder().settingKey(key).settingValue(val).description(desc).build());
        }
    }

    private void seedStaffAndDoctor() {
        String defaultPwHash = passwordEncoder.encode("MatKhauAdmin@2026");

        // Tiếp tân
        if (userRepository.findByUsername("letan").isEmpty()) {
            userRepository.save(User.builder()
                    .username("letan")
                    .passwordHash(defaultPwHash)
                    .fullName("Lễ tân Chúc Từ")
                    .phone("0901234567")
                    .email("letan@chuctu.vn")
                    .role(Role.STAFF)
                    .isActive(true)
                    .mustChangePassword(false)
                    .build());
            log.info("Đã khởi tạo tài khoản tiếp tân 'letan'.");
        }

        // Bác sĩ
        if (userRepository.findByUsername("bacsi").isEmpty()) {
            User doctorUser = userRepository.save(User.builder()
                    .username("bacsi")
                    .passwordHash(defaultPwHash)
                    .fullName("Bác sĩ Nguyễn Văn Minh")
                    .phone("0911223344")
                    .email("bsnam@chuctu.vn")
                    .role(Role.DOCTOR)
                    .isActive(true)
                    .mustChangePassword(false)
                    .build());

            if (doctorRepository.count() == 0) {
                doctorRepository.save(Doctor.builder()
                        .user(doctorUser)
                        .fullName("Bác sĩ Nguyễn Văn Minh")
                        .title("Bác sĩ Y học cổ truyền")
                        .specialty("Cơ xương khớp & Châm cứu")
                        .experienceYears(15)
                        .bio("Bác sĩ Y học cổ truyền giàu kinh nghiệm bắt mạch và bốc thuốc nam")
                        .phone("0911223344")
                        .isActive(true)
                        .sortOrder(1)
                        .build());
            }
            log.info("Đã khởi tạo tài khoản bác sĩ 'bacsi'.");
        }
    }

    private void seedArticles() {
        if (articleCategoryRepository.count() == 0) {
            ArticleCategory c1 = articleCategoryRepository.save(ArticleCategory.builder().name("Y học cổ truyền").slug("y-hoc-co-truyen").description("Kiến thức châm cứu, bấm huyệt, thảo mộc chữa bệnh").sortOrder(1).build());
            ArticleCategory c2 = articleCategoryRepository.save(ArticleCategory.builder().name("Cơ xương khớp").slug("co-xuong-khop").description("Phòng và trị các bệnh thoái hóa khớp, đau lưng, vai gáy").sortOrder(2).build());
            ArticleCategory c3 = articleCategoryRepository.save(ArticleCategory.builder().name("Dưỡng sinh người cao tuổi").slug("duong-sinh-nguoi-cao-tuoi").description("Chế độ sinh hoạt, vận động và giấc ngủ cho người lớn tuổi").sortOrder(3).build());

            User adminUser = userRepository.findByUsername("admin").orElse(null);
            if (adminUser != null) {
                articleRepository.save(Article.builder()
                        .title("Châm cứu điều trị đau nhức xương khớp ở người cao tuổi")
                        .slug("cham-cuu-dieu-tri-dau-nhuc-xuong-khop-o-nguoi-cao-tuoi")
                        .summary("Châm cứu là phương pháp trị liệu không dùng thuốc an toàn, giúp đả thông kinh lạc, giảm đau nhức thoái hóa cột sống, khớp gối hiệu quả cho các cô bác lớn tuổi.")
                        .content("<p>Theo Y học cổ truyền, “thông bất thống, thống bất thông” — khí huyết lưu thông thì cơ thể không đau, khí huyết ứ trệ kinh lạc bế tắc sẽ sinh ra đau nhức.</p>")
                        .thumbnailUrl("https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?auto=format&fit=crop&w=600&q=80")
                        .category(c1)
                        .author(adminUser)
                        .isPublished(true)
                        .isPinned(true)
                        .publishedAt(LocalDateTime.now())
                        .viewCount(125)
                        .build());
            }
        }
    }
}
