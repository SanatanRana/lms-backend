package com.lms.modules.live.scheduler;

import com.lms.common.enums.SessionStatus;
import com.lms.modules.live.entity.LiveSessionEntity;
import com.lms.modules.live.repository.LiveSessionRepository;
import com.lms.modules.live.service.LiveSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class LiveSessionCleanupScheduler {

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private LiveSessionService liveSessionService;

    /**
     * Sweeps active rooms every 5 minutes (300,000 milliseconds).
     * Ends sessions that have run for more than 1 hour past their scheduled end time.
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupOverdueSessions() {
        System.out.println("[Scheduler] Running inactivity sweeper for live sessions...");
        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(1);
            List<LiveSessionEntity> overdueSessions = liveSessionRepository.findByStatusAndEndTimeBefore(
                    SessionStatus.LIVE, threshold);

            for (LiveSessionEntity session : overdueSessions) {
                System.out.println("[Scheduler] Automatically ending overdue session: " + session.getTitle() + " (ID: " + session.getId() + ")");
                try {
                    liveSessionService.endSession(session.getId());
                } catch (Exception e) {
                    System.err.println("[Scheduler] Error ending session " + session.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Error in live session cleanup job: " + e.getMessage());
        }
    }
}
