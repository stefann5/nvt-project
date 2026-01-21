package nvt.backend.services.warehouse;

import nvt.backend.config.RabbitMQConfig;
import nvt.backend.dto.warehouse.WarehouseHeartbeatDTO;
import nvt.backend.dto.warehouse.WarehouseTemperatureDTO;
import nvt.backend.model.warehouse.Warehouse;
import nvt.backend.model.warehouse.WarehouseSector;
import nvt.backend.repositories.warehouse.WarehouseRepository;
import nvt.backend.repositories.warehouse.WarehouseSectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class WarehouseMessageListener {

    private static final Logger log = LoggerFactory.getLogger(WarehouseMessageListener.class);

    private final WarehouseRepository warehouseRepository;
    private final WarehouseSectorRepository sectorRepository;
    private final WarehouseTelemetryService telemetryService;
    private final SimpMessagingTemplate messagingTemplate;

    public WarehouseMessageListener(
            WarehouseRepository warehouseRepository,
            WarehouseSectorRepository sectorRepository,
            WarehouseTelemetryService telemetryService,
            SimpMessagingTemplate messagingTemplate) {
        this.warehouseRepository = warehouseRepository;
        this.sectorRepository = sectorRepository;
        this.telemetryService = telemetryService;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.WAREHOUSE_HEARTBEAT_QUEUE)
    @Transactional
    public void handleHeartbeat(WarehouseHeartbeatDTO heartbeat) {
        try {
            log.info("Received heartbeat from warehouse {}: {}", heartbeat.getWarehouseId(), heartbeat.getStatus());

            Warehouse warehouse = warehouseRepository.findById(heartbeat.getWarehouseId())
                    .orElseThrow(() -> new RuntimeException("Warehouse not found: " + heartbeat.getWarehouseId()));

            warehouse.setLastHeartbeat(parseTimestamp(heartbeat.getTimestamp()));
            warehouse.setOnline(true);
            warehouseRepository.save(warehouse);

            telemetryService.recordHeartbeat(heartbeat.getWarehouseId(), heartbeat.getWarehouseName(), true);

            messagingTemplate.convertAndSend("/topic/warehouse/" + heartbeat.getWarehouseId() + "/heartbeat", heartbeat);

        } catch (Exception e) {
            log.error("Failed to process warehouse heartbeat message: {}", heartbeat, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.WAREHOUSE_TEMPERATURE_QUEUE)
    @Transactional
    public void handleTemperature(WarehouseTemperatureDTO temperature) {
        try {
            log.info("Received temperature data from warehouse {}: {} sectors",
                    temperature.getWarehouseId(), temperature.getSectors() != null ? temperature.getSectors().size() : 0);

            Warehouse warehouse = warehouseRepository.findById(temperature.getWarehouseId())
                    .orElseThrow(() -> new RuntimeException("Warehouse not found: " + temperature.getWarehouseId()));

            LocalDateTime updateTime = parseTimestamp(temperature.getTimestamp());

            if (temperature.getSectors() != null) {
                for (WarehouseTemperatureDTO.SectorTemperature sectorTemp : temperature.getSectors()) {
                    WarehouseSector sector = sectorRepository.findByWarehouseIdAndName(
                            temperature.getWarehouseId(), sectorTemp.getSectorName())
                            .orElse(null);

                    if (sector != null) {
                        sector.setCurrentTemperature(sectorTemp.getTemperature());
                        sector.setLastTemperatureUpdate(updateTime);
                        sectorRepository.save(sector);

                        telemetryService.recordTemperature(
                                temperature.getWarehouseId(),
                                temperature.getWarehouseName(),
                                sector.getId(),
                                sector.getName(),
                                sectorTemp.getTemperature()
                        );
                    } else {
                        log.warn("Sector '{}' not found for warehouse {}", sectorTemp.getSectorName(), temperature.getWarehouseId());
                    }
                }
            }

            messagingTemplate.convertAndSend("/topic/warehouse/" + temperature.getWarehouseId() + "/temperature", temperature);

        } catch (Exception e) {
            log.error("Failed to process warehouse temperature message: {}", temperature, e);
        }
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
