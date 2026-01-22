package nvt.backend.services.warehouse;

import nvt.backend.model.warehouse.Warehouse;
import nvt.backend.repositories.warehouse.WarehouseRepository;
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
public class WarehouseOnlineChecker {

    private static final Logger log = LoggerFactory.getLogger(WarehouseOnlineChecker.class);

    private final WarehouseRepository warehouseRepository;
    private final WarehouseTelemetryService telemetryService;
    private final SimpMessagingTemplate messagingTemplate;
    private final int heartbeatTimeoutSeconds;

    public WarehouseOnlineChecker(
            WarehouseRepository warehouseRepository,
            WarehouseTelemetryService telemetryService,
            SimpMessagingTemplate messagingTemplate,
            @Value("${warehouse.heartbeat.timeout:60}") int heartbeatTimeoutSeconds) {
        this.warehouseRepository = warehouseRepository;
        this.telemetryService = telemetryService;
        this.messagingTemplate = messagingTemplate;
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
    }

    @Scheduled(fixedRateString = "${warehouse.heartbeat.check-interval:15000}")
    @Transactional
    public void checkWarehouseStatus() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);

        List<Warehouse> staleWarehouses = warehouseRepository.findOnlineWarehousesWithOldHeartbeat(threshold);

        for (Warehouse warehouse : staleWarehouses) {
            log.info("Warehouse {} marked as offline due to heartbeat timeout", warehouse.getId());

            warehouse.setOnline(false);
            warehouseRepository.save(warehouse);

            // Record offline state in InfluxDB
            telemetryService.recordHeartbeat(
                    warehouse.getId(),
                    warehouse.getName(),
                    false
            );

            // Send WebSocket notification
            Map<String, Object> notification = new HashMap<>();
            notification.put("warehouseId", warehouse.getId());
            notification.put("online", false);
            notification.put("reason", "HEARTBEAT_TIMEOUT");
            notification.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend(
                    "/topic/warehouse/" + warehouse.getId() + "/status",
                    notification
            );
        }

        if (!staleWarehouses.isEmpty()) {
            log.info("Marked {} warehouses as offline due to heartbeat timeout", staleWarehouses.size());
        }
    }
}
