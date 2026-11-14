package com.chuctupk.service;

import com.chuctupk.entity.Counter;
import com.chuctupk.repository.CounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Dịch vụ sinh mã số nguyên tử và an toàn thông qua bảng counters (sử dụng SELECT ... FOR UPDATE).
 * Đảm bảo định dạng chuẩn:
 * - Mã bệnh nhân: CT-BN-yy-xxxx (VD: CT-BN-26-0001)
 * - Mã phiếu khám: CT-PK-yy-ddMM-xxx (VD: CT-PK-26-1411-001)
 */
@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final CounterRepository counterRepository;
    private final Clock clock;

    private static final DateTimeFormatter DDMM_FORMATTER = DateTimeFormatter.ofPattern("ddMM");

    @Transactional
    public String nextPatientCode() {
        LocalDate today = LocalDate.now(clock);
        int yy = today.getYear() % 100;
        String counterKey = "PATIENT_CODE_" + yy;

        Counter counter = counterRepository.findByNameForUpdate(counterKey)
                .orElseGet(() -> Counter.builder().name(counterKey).value(0L).build());

        long nextVal = counter.getValue() + 1;
        counter.setValue(nextVal);
        counterRepository.save(counter);

        return String.format("CT-BN-%02d-%04d", yy, nextVal);
    }

    @Transactional
    public String nextAppointmentCode(LocalDate date) {
        LocalDate apptDate = date != null ? date : LocalDate.now(clock);
        int yy = apptDate.getYear() % 100;
        String ddMM = apptDate.format(DDMM_FORMATTER);
        String counterKey = "APPOINTMENT_CODE_" + yy + ddMM;

        Counter counter = counterRepository.findByNameForUpdate(counterKey)
                .orElseGet(() -> Counter.builder().name(counterKey).value(0L).build());

        long nextVal = counter.getValue() + 1;
        counter.setValue(nextVal);
        counterRepository.save(counter);

        return String.format("CT-PK-%02d-%s-%03d", yy, ddMM, nextVal);
    }

    @Transactional
    public int nextQueueNumber(LocalDate date) {
        LocalDate qDate = date != null ? date : LocalDate.now(clock);
        String counterKey = "QUEUE_NO_" + qDate.toString();

        Counter counter = counterRepository.findByNameForUpdate(counterKey)
                .orElseGet(() -> Counter.builder().name(counterKey).value(0L).build());

        long nextVal = counter.getValue() + 1;
        counter.setValue(nextVal);
        counterRepository.save(counter);

        return (int) nextVal;
    }
}
