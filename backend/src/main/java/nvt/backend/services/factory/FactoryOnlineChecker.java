package nvt.backend.services.factory;

import nvt.backend.model.factory.Factory;
import nvt.backend.repositories.factory.FactoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FactoryOnlineChecker {

    private static final Logger log = LoggerFactory.getLogger(FactoryOnlineChecker.class);

    private final FactoryRepository factoryRepository;
    private final FactoryTelemetryService telemetryService;
    private final SimpMessagingTemplate messagingTemplate;
    private final int heartbeatTimeoutSeconds;

    public FactoryOnlineChecker(
            FactoryRepository factoryRepository,
            FactoryTelemetryService telemetryService,
            SimpMessagingTemplate messagingTemplate,
            @Value("${factory.heartbeat.timeout:60}") int heartbeatTimeoutSeconds) {
        this.factoryRepository = factoryRepository;
        this.telemetryService = telemetryService;
        this.messagingTemplate = messagingTemplate;
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
    }

    @Scheduled(fixedRateString = "${factory.heartbeat.check-interval:15000}")
    @Transactional
    public void checkFactoryStatus() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);

        List<Factory> staleFactories = factoryRepository.findOnlineFactoriesWithOldHeartbeat(threshold);

        for (Factory factory : staleFactories) {
            log.debug("Factory {} marked as offline due to heartbeat timeout", factory.getId());

            factory.setOnline(false);
            factoryRepository.save(factory);

            // Record offline state in InfluxDB
            telemetryService.recordHeartbeat(
                    factory.getId(),
                    factory.getName(),
                    false
            );

            // Send WebSocket notification
            Map<String, Object> notification = new HashMap<>();
            notification.put("factoryId", factory.getId());
            notification.put("online", false);
            notification.put("reason", "HEARTBEAT_TIMEOUT");
            notification.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend(
                    "/topic/factory/" + factory.getId() + "/status",
                    notification
            );
        }

        if (!staleFactories.isEmpty()) {
            log.info("Marked {} factories as offline due to heartbeat timeout", staleFactories.size());
        }
    }
}
