package nvt.backend.services.factory;

import nvt.backend.config.RabbitMQConfig;
import nvt.backend.dto.factory.FactoryHeartbeatDTO;
import nvt.backend.dto.factory.FactoryProductionDTO;
import nvt.backend.model.factory.Factory;
import nvt.backend.repositories.factory.FactoryRepository;
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
public class FactoryMessageListener {

    private static final Logger log = LoggerFactory.getLogger(FactoryMessageListener.class);

    private final FactoryRepository factoryRepository;
    private final FactoryTelemetryService telemetryService;
    private final SimpMessagingTemplate messagingTemplate;

    public FactoryMessageListener(
            FactoryRepository factoryRepository,
            FactoryTelemetryService telemetryService,
            SimpMessagingTemplate messagingTemplate) {
        this.factoryRepository = factoryRepository;
        this.telemetryService = telemetryService;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.FACTORY_HEARTBEAT_QUEUE)
    @Transactional
    public void handleHeartbeat(FactoryHeartbeatDTO heartbeat) {
        try {
            log.info("Received heartbeat from factory {}: {}", heartbeat.getFactoryId(), heartbeat.getStatus());

            Factory factory = factoryRepository.findById(heartbeat.getFactoryId())
                    .orElseThrow(() -> new RuntimeException("Factory not found: " + heartbeat.getFactoryId()));

            factory.setLastHeartbeat(parseTimestamp(heartbeat.getTimestamp()));
            factory.setOnline(true);
            factoryRepository.save(factory);

            telemetryService.recordHeartbeat(heartbeat.getFactoryId(), heartbeat.getFactoryName(), true);

            messagingTemplate.convertAndSend("/topic/factory/" + heartbeat.getFactoryId() + "/heartbeat", heartbeat);

        } catch (Exception e) {
            log.error("Failed to process factory heartbeat message: {}", heartbeat, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.FACTORY_PRODUCTION_QUEUE)
    @Transactional
    public void handleProduction(FactoryProductionDTO production) {
        try {
            log.info("Received production report from factory {}: {} products, type={}",
                    production.getFactoryId(), 
                    production.getProducts() != null ? production.getProducts().size() : 0,
                    production.getReportType());

            Factory factory = factoryRepository.findById(production.getFactoryId())
                    .orElseThrow(() -> new RuntimeException("Factory not found: " + production.getFactoryId()));

            if (production.getProducts() != null) {
                for (FactoryProductionDTO.ProductionItem item : production.getProducts()) {
                    telemetryService.recordProduction(
                            production.getFactoryId(),
                            production.getFactoryName(),
                            item.getProductId(),
                            item.getProductName(),
                            item.getQuantity(),
                            production.getReportType()
                    );
                    log.debug("Recorded production for product {}: {} units", item.getProductName(), item.getQuantity());
                }
            }

            messagingTemplate.convertAndSend("/topic/factory/" + production.getFactoryId() + "/production", production);

        } catch (Exception e) {
            log.error("Failed to process factory production message: {}", production, e);
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
