package nvt.backend.services.factory;

import nvt.backend.config.RabbitMQConfig;
import nvt.backend.dto.factory.FactoryHeartbeatDTO;
import nvt.backend.dto.factory.FactoryProductionDTO;
import nvt.backend.model.factory.Factory;
import nvt.backend.model.warehouse.Warehouse;
import nvt.backend.repositories.factory.FactoryRepository;
import nvt.backend.repositories.warehouse.InventoryRepository;
import nvt.backend.repositories.warehouse.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class FactoryMessageListener {

    private static final Logger log = LoggerFactory.getLogger(FactoryMessageListener.class);

    private final FactoryRepository factoryRepository;
    private final FactoryTelemetryService telemetryService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;

    public FactoryMessageListener(
            FactoryRepository factoryRepository,
            FactoryTelemetryService telemetryService,
            SimpMessagingTemplate messagingTemplate,
            WarehouseRepository warehouseRepository,
            InventoryRepository inventoryRepository) {
        this.factoryRepository = factoryRepository;
        this.telemetryService = telemetryService;
        this.messagingTemplate = messagingTemplate;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
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
                    // Record production telemetry
                    telemetryService.recordProduction(
                            production.getFactoryId(),
                            production.getFactoryName(),
                            item.getProductId(),
                            item.getProductName(),
                            item.getQuantity(),
                            production.getReportType()
                    );
                    log.debug("Recorded production for product {}: {} units", item.getProductName(), item.getQuantity());

                    // Store produced items in warehouse in the same city
                    storeProductInWarehouse(factory, item);
                }
            }

            messagingTemplate.convertAndSend("/topic/factory/" + production.getFactoryId() + "/production", production);

        } catch (Exception e) {
            log.error("Failed to process factory production message: {}", production, e);
        }
    }

    /**
     * Find a warehouse and store the produced items.
     * Priority:
     * 1. Warehouse in the same city that already has this product
     * 2. Any warehouse globally that has this product and has enough capacity
     * 3. Log error if nothing found
     */
    private void storeProductInWarehouse(Factory factory, FactoryProductionDTO.ProductionItem item) {
        Long cityId = factory.getCity().getId();
        Long productId = item.getProductId();
        Integer quantity = item.getQuantity();
        String cityName = factory.getCity().getName();

        // Try to find a warehouse in the same city that already stores this product
        Optional<Warehouse> warehouseInCity = warehouseRepository.findByCityAndProduct(cityId, productId);

        if (warehouseInCity.isPresent()) {
            Warehouse warehouse = warehouseInCity.get();
            int updated = inventoryRepository.addQuantity(warehouse.getId(), productId, quantity);
            
            if (updated > 0) {
                log.info("Added {} units of product '{}' (ID: {}) to warehouse '{}' (ID: {}) in same city '{}'",
                        quantity, item.getProductName(), productId, 
                        warehouse.getName(), warehouse.getId(), cityName);
                return;
            }
        }

        // Try to find any warehouse globally that has this product and has capacity
        List<Warehouse> warehousesWithProduct = warehouseRepository.findByProductWithAvailableCapacity(productId, quantity);
        
        if (!warehousesWithProduct.isEmpty()) {
            Warehouse warehouse = warehousesWithProduct.get(0); // Take first available
            int updated = inventoryRepository.addQuantity(warehouse.getId(), productId, quantity);
            
            if (updated > 0) {
                log.info("Added {} units of product '{}' (ID: {}) to warehouse '{}' (ID: {}) in city '{}' (different from factory city '{}')",
                        quantity, item.getProductName(), productId,
                        warehouse.getName(), warehouse.getId(), 
                        warehouse.getCity() != null ? warehouse.getCity().getName() : "Unknown",
                        cityName);
                return;
            }
        }

        // Nothing found - log error
        log.error("CANNOT STORE PRODUCTION: No warehouse found for product '{}' (ID: {}). " +
                  "Factory '{}' produced {} units but there is no warehouse with this product in inventory.",
                  item.getProductName(), productId, factory.getName(), quantity);
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
