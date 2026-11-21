package com.chuctupk.repository.specification;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.Patient;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PatientSpecifications {

    private PatientSpecifications() {}

    public static Specification<Patient> buildSearchSpec(String q, Boolean locked, Long doctorUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Text / NationalId / YearOfBirth search
            if (q != null && !q.isBlank()) {
                String term = q.trim();
                List<Predicate> searchPredicates = new ArrayList<>();

                // Tên
                searchPredicates.add(cb.like(cb.lower(root.get("fullName")), "%" + term.toLowerCase() + "%"));
                // SĐT
                searchPredicates.add(cb.like(root.get("phone"), "%" + term + "%"));
                // Mã bệnh nhân
                searchPredicates.add(cb.like(cb.lower(root.get("patientCode")), "%" + term.toLowerCase() + "%"));

                boolean isDigits = term.matches("\\d+");
                if (isDigits) {
                    // Nếu q toàn số và độ dài 9–12 -> tìm thêm theo nationalId
                    if (term.length() >= 9 && term.length() <= 12) {
                        searchPredicates.add(cb.equal(root.get("nationalId"), term));
                    }
                    // Nếu q là 4 số (1900–2100) -> tìm thêm theo yearOfBirth
                    if (term.length() == 4) {
                        try {
                            int year = Integer.parseInt(term);
                            if (year >= 1900 && year <= 2100) {
                                searchPredicates.add(cb.equal(root.get("yearOfBirth"), year));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            // 2. Locked filter
            if (locked != null) {
                if (locked) {
                    predicates.add(cb.or(
                            cb.isFalse(root.get("isActive")),
                            cb.isNotNull(root.get("lockedAt"))
                    ));
                } else {
                    predicates.add(cb.and(
                            cb.isTrue(root.get("isActive")),
                            cb.isNull(root.get("lockedAt"))
                    ));
                }
            }

            // 3. Doctor scope filter
            if (doctorUserId != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Appointment> apptRoot = subquery.from(Appointment.class);
                subquery.select(apptRoot.get("patient").get("id"));
                subquery.where(cb.equal(apptRoot.get("doctor").get("user").get("id"), doctorUserId));
                predicates.add(root.get("id").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
