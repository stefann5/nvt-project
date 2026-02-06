package nvt.backend.controllers.warehouse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nvt.backend.services.warehouse.WarehouseAvailabilityWebSocketService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for real-time warehouse availability updates.
 *
 * <p>This controller handles STOMP WebSocket messaging for subscribing to and
 * unsubscribing from warehouse availability updates.</p>
 *
 * <h3>WebSocket Endpoints:</h3>
 * <ul>
 *   <li><strong>Subscribe:</strong> /app/warehouse/{warehouseId}/availability/subscribe
 *       <br>Subscribes to real-time availability updates for a specific warehouse</li>
 *   <li><strong>Unsubscribe:</strong> /app/warehouse/{warehouseId}/availability/unsubscribe
 *       <br>Unsubscribes from availability updates for a specific warehouse</li>
 * </ul>
 *
 * <h3>Subscription Topic:</h3>
 * <p>Updates are broadcast to: /topic/warehouse/{warehouseId}/availability</p>
 */
@Controller
@RequiredArgsConstructor
@Tag(name = "Warehouse Availability WebSocket", description = "WebSocket endpoints for real-time warehouse availability updates")
public class WarehouseAvailabilityWebSocketController {

    private final WarehouseAvailabilityWebSocketService availabilityWebSocketService;

    /**
     * Subscribe to real-time availability updates for a specific warehouse.
     *
     * @param warehouseId the ID of the warehouse to subscribe to
     */
    @MessageMapping("/warehouse/{warehouseId}/availability/subscribe")
    public void subscribeToAvailability(@DestinationVariable Long warehouseId) {
        availabilityWebSocketService.subscribeToWarehouse(warehouseId);
    }

    /**
     * Unsubscribe from availability updates for a specific warehouse.
     *
     * @param warehouseId the ID of the warehouse to unsubscribe from
     */
    @MessageMapping("/warehouse/{warehouseId}/availability/unsubscribe")
    public void unsubscribeFromAvailability(@DestinationVariable Long warehouseId) {
        availabilityWebSocketService.unsubscribeFromWarehouse(warehouseId);
    }
}
