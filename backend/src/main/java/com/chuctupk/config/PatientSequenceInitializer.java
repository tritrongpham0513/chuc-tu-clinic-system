package com.chuctupk.config;

import com.chuctupk.entity.ClinicSetting;
import com.chuctupk.repository.ClinicSettingRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.service.impl.AppointmentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Khởi tạo sequence sinh mã hồ sơ bệnh nhân cho năm hiện tại nếu chưa có trong clinic_settings.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class PatientSequenceInitializer implements ApplicationRunner {

    private final ClinicSettingRepository clinicSettingRepository;
    private final PatientRepository patientRepository;
    private final Clock clock;

    @Override
    public void run(ApplicationArguments args) {
        int currentYear = LocalDate.now(clock).getYear();
        String seqKey = "patient_seq_" + currentYear;

        if (!clinicSettingRepository.existsById(seqKey)) {
            long currentCount = patientRepository.countByPatientCodeStartingWith("BN-" + currentYear + "-");
            clinicSettingRepository.save(ClinicSetting.builder()
                    .settingKey(seqKey)
                    .settingValue(String.valueOf(currentCount))
                    .description("Bộ đếm mã hồ sơ bệnh nhân năm " + currentYear)
                    .build());
            log.info("Đã khởi tạo bộ đếm mã bệnh nhân '{}' bắt đầu từ {}", seqKey, currentCount);
        }

        if (!clinicSettingRepository.existsById(AppointmentServiceImpl.KEY_PATIENT_SEQ_MUTEX)) {
            clinicSettingRepository.save(ClinicSetting.builder()
                    .settingKey(AppointmentServiceImpl.KEY_PATIENT_SEQ_MUTEX)
                    .settingValue("1")
                    .description("Khóa mutex nội bộ sinh mã bệnh nhân an toàn đồng thời")
                    .build());
        }
    }
}

