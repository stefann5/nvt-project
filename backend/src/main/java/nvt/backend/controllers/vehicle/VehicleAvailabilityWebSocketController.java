package nvt.backend.controllers.vehicle;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nvt.backend.services.vehicle.VehicleAvailabilityWebSocketService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for real-time vehicle availability updates.
 *
 * <p>This controller handles STOMP WebSocket messaging for subscribing to and
 * unsubscribing from vehicle availability updates.</p>
 *
 * <h3>WebSocket Endpoints:</h3>
 * <ul>
 *   <li><strong>Subscribe:</strong> /app/vehicle/{vehicleId}/availability/subscribe
 *       <br>Subscribes to real-time availability updates for a specific vehicle</li>
 *   <li><strong>Unsubscribe:</strong> /app/vehicle/{vehicleId}/availability/unsubscribe
 *       <br>Unsubscribes from availability updates for a specific vehicle</li>
 * </ul>
 *
 * <h3>Subscription Topic:</h3>
 * <p>Updates are broadcast to: /topic/vehicle/{vehicleId}/availability</p>
 */
@Controller
@RequiredArgsConstructor
@Tag(name = "Vehicle Availability WebSocket", description = "WebSocket endpoints for real-time vehicle availability updates")
public class VehicleAvailabilityWebSocketController {

    private final VehicleAvailabilityWebSocketService availabilityWebSocketService;

    /**
     * Subscribe to real-time availability updates for a specific vehicle.
     *
     * @param vehicleId the ID of the vehicle to subscribe to
     */
    @MessageMapping("/vehicle/{vehicleId}/availability/subscribe")
    public void subscribeToAvailability(@DestinationVariable Long vehicleId) {
        availabilityWebSocketService.subscribeToVehicle(vehicleId);
    }

    /**
     * Unsubscribe from availability updates for a specific vehicle.
     *
     * @param vehicleId the ID of the vehicle to unsubscribe from
     */
    @MessageMapping("/vehicle/{vehicleId}/availability/unsubscribe")
    public void unsubscribeFromAvailability(@DestinationVariable Long vehicleId) {
        availabilityWebSocketService.unsubscribeFromVehicle(vehicleId);
    }
}
