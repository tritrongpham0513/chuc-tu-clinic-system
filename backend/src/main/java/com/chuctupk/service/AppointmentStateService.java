package com.chuctupk.service;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;

public interface AppointmentStateService {

    Appointment transition(Appointment appointment,
                           AppointmentStatus newStatus,
                           User actor,
                           String reason);
}
