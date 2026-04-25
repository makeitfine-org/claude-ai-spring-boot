package pl.piomin.services.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WorkRequest(
        String title,
        String description,
        LocalDate endDate,
        BigDecimal price,
        LocalDate payDate,
        Long assignedWorkerId,
        List<Long> additionalWorkerIds) {
}
