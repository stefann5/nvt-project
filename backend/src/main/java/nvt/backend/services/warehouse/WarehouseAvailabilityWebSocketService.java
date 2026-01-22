package nvt.backend.services.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import nvt.backend.dto.warehouse.WarehouseAvailabilityStatisticsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WarehouseAvailabilityWebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseAvailabilityWebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final WarehouseService warehouseService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Set<Long> subscribedWarehouses = ConcurrentHashMap.newKeySet();

    public void subscribeToWarehouse(Long warehouseId) {
        subscribedWarehouses.add(warehouseId);
        log.info("Warehouse {} subscribed for real-time availability updates", warehouseId);
    }

    public void unsubscribeFromWarehouse(Long warehouseId) {
        subscribedWarehouses.remove(warehouseId);
        log.info("Warehouse {} unsubscribed from real-time availability updates", warehouseId);
    }

    @Scheduled(fixedRate = 30000)
    public void pushAvailabilityUpdates() {
        log.debug("Checking for subscribed warehouses: {}", subscribedWarehouses.size());
        for (Long warehouseId : subscribedWarehouses) {
            try {
                Instant end = Instant.now();
                Instant start = end.minus(3, ChronoUnit.HOURS);

                WarehouseAvailabilityStatisticsDTO stats = warehouseService.getAvailabilityStatistics(warehouseId, start, end);

                messagingTemplate.convertAndSend(
                        "/topic/warehouse/" + warehouseId + "/availability",
                        stats
                );
                log.debug("Pushed availability update for warehouse {}", warehouseId);
            } catch (Exception e) {
                log.error("Failed to push availability update for warehouse {}", warehouseId, e);
            }
        }
    }

    public void notifyAvailabilityChange(Long warehouseId, boolean online) {
        if (subscribedWarehouses.contains(warehouseId)) {
            try {
                Map<String, Object> notification = Map.of(
                        "warehouseId", warehouseId,
                        "online", online,
                        "timestamp", Instant.now().toString()
                );
                messagingTemplate.convertAndSend(
                        "/topic/warehouse/" + warehouseId + "/availability/status",
                        notification
                );
            } catch (Exception e) {
                log.error("Failed to notify availability change for warehouse {}", warehouseId, e);
            }
        }
    }
}
