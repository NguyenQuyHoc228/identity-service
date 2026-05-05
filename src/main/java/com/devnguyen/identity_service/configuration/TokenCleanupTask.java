package com.devnguyen.identity_service.configuration;

import com.devnguyen.identity_service.repository.InvalidatedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupTask {

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    /*
     * @Scheduled(cron = "0 0 * * * *"):
     * Cron expression gồm 6 phần: giây phút giờ ngày tháng thứ
     * "0 0 * * * *" = vào đầu mỗi giờ (phút 0, giây 0)
     *
     * Các ví dụ khác:
     * "0 0 0 * * *"  = mỗi ngày lúc 00:00
     * "0 0/30 * * * *" = mỗi 30 phút
     * "0 0 2 * * *"  = mỗi ngày lúc 2:00 AM (tốt cho production)
     *
     * Tại sao @Transactional?
     * → deleteByExpiryTimeBefore là write operation
     * → Cần transaction để đảm bảo atomic (xóa hết hoặc không xóa gì)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        Date now = new Date();

        // Đếm trước để log
        long count = invalidatedTokenRepository
                .countByExpiryTimeBefore(now);

        if (count > 0) {
            invalidatedTokenRepository.deleteByExpiryTimeBefore(now);
            log.info("Cleaned up {} expired tokens", count);
        } else {
            log.debug("No expired tokens to clean up");
        }
    }
}