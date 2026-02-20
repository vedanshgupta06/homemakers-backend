package com.homemakers.homemakers.scheduler;

import com.homemakers.homemakers.service.attendance.AttendanceGenerationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AttendanceScheduler {

    private final AttendanceGenerationService attendanceGenerationService;

    public AttendanceScheduler(
            AttendanceGenerationService attendanceGenerationService
    ) {
        this.attendanceGenerationService = attendanceGenerationService;
    }

    /**
     * Runs daily after midnight.
     * Generates attendance for the PREVIOUS day.
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void generateDailyAttendance() {
        LocalDate attendanceDate = LocalDate.now().minusDays(1);
        attendanceGenerationService.generateDailyAttendance(attendanceDate);
    }
}
