package nvt.backend.controllers.vehicle;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Vehicle Tracking", description = "Real-time vehicle tracking and telemetry endpoints")
public class VehicleTrackingController {

    private final VehicleTrackingService trackingService;
    private final VehicleTelemetryService telemetryService;

    public VehicleTrackingController(
            VehicleTrackingService trackingService,
            VehicleTelemetryService telemetryService) {
        this.trackingService = trackingService;
        this.telemetryService = telemetryService;
    }

    @Operation(
            summary = "Get status of all vehicles",
            description = "Retrieves the current status of all vehicles including location and online status. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle statuses retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = VehicleStatusDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleStatusDTO>> getAllVehicleStatus() {
        return ResponseEntity.ok(trackingService.getAllVehicleStatus());
    }

    @Operation(
            summary = "Get status of a specific vehicle",
            description = "Retrieves the current status of a specific vehicle. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = VehicleStatusDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/status/{vehicleId}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<VehicleStatusDTO> getVehicleStatus(
            @Parameter(description = "Vehicle ID", required = true)
            @PathVariable Long vehicleId) {
        return ResponseEntity.ok(trackingService.getVehicleStatus(vehicleId));
    }

    @Operation(
            summary = "Get online vehicles",
            description = "Retrieves all currently online vehicles. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Online vehicles retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = VehicleStatusDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/online")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleStatusDTO>> getOnlineVehicles() {
        return ResponseEntity.ok(trackingService.getOnlineVehicles());
    }

    @Operation(
            summary = "Get vehicle availability history",
            description = "Retrieves availability history for a vehicle within a specified time range. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{vehicleId}/availability")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAvailabilityHistory(
            @Parameter(description = "Vehicle ID", required = true)
            @PathVariable Long vehicleId,
            @Parameter(description = "Time range (e.g., -24h, -7d, -30d)", example = "-24h")
            @RequestParam(defaultValue = "-24h") String range) {
        return ResponseEntity.ok(telemetryService.getAvailabilityHistory(vehicleId, range));
    }

    @Operation(
            summary = "Get vehicle distance history",
            description = "Retrieves distance traveled history for a vehicle within a specified time range. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Distance history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{vehicleId}/distance")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getDistanceHistory(
            @Parameter(description = "Vehicle ID", required = true)
            @PathVariable Long vehicleId,
            @Parameter(description = "Time range (e.g., -24h, -7d, -30d)", example = "-24h")
            @RequestParam(defaultValue = "-24h") String range) {
        return ResponseEntity.ok(telemetryService.getDistanceHistory(vehicleId, range));
    }
}
