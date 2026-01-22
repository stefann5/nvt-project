package nvt.backend.controllers.warehouse;

import lombok.RequiredArgsConstructor;
import nvt.backend.services.warehouse.WarehouseAvailabilityWebSocketService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WarehouseAvailabilityWebSocketController {

    private final WarehouseAvailabilityWebSocketService availabilityWebSocketService;

    @MessageMapping("/warehouse/{warehouseId}/availability/subscribe")
    public void subscribeToAvailability(@DestinationVariable Long warehouseId) {
        availabilityWebSocketService.subscribeToWarehouse(warehouseId);
    }

    @MessageMapping("/warehouse/{warehouseId}/availability/unsubscribe")
    public void unsubscribeFromAvailability(@DestinationVariable Long warehouseId) {
        availabilityWebSocketService.unsubscribeFromWarehouse(warehouseId);
    }
}
