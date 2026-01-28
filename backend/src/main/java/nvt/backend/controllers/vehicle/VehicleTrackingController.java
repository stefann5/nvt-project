package nvt.backend.controllers.vehicle;

import nvt.backend.dto.vehicle.VehicleStatusDTO;
import nvt.backend.services.vehicle.VehicleTelemetryService;
import nvt.backend.services.vehicle.VehicleTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicles/tracking")
public class VehicleTrackingController {

    private final VehicleTrackingService trackingService;
    private final VehicleTelemetryService telemetryService;

    public VehicleTrackingController(
            VehicleTrackingService trackingService,
            VehicleTelemetryService telemetryService) {
        this.trackingService = trackingService;
        this.telemetryService = telemetryService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleStatusDTO>> getAllVehicleStatus() {
        // Caching is handled in the service layer (caches List<DTO> not ResponseEntity)
        return ResponseEntity.ok(trackingService.getAllVehicleStatus());
    }

    @GetMapping("/status/{vehicleId}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<VehicleStatusDTO> getVehicleStatus(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(trackingService.getVehicleStatus(vehicleId));
    }

    @GetMapping("/online")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleStatusDTO>> getOnlineVehicles() {
        // Caching is handled in the service layer (caches List<DTO> not ResponseEntity)
        return ResponseEntity.ok(trackingService.getOnlineVehicles());
    }

    @GetMapping("/{vehicleId}/availability")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAvailabilityHistory(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "-24h") String range) {
        return ResponseEntity.ok(telemetryService.getAvailabilityHistory(vehicleId, range));
    }

    @GetMapping("/{vehicleId}/distance")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getDistanceHistory(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "-24h") String range) {
        return ResponseEntity.ok(telemetryService.getDistanceHistory(vehicleId, range));
    }
}
