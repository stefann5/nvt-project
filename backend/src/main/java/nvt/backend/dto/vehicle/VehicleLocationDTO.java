package nvt.backend.dto.vehicle;

import lombok.Builder;
import lombok.Data;
import nvt.backend.model.vehicle.VehicleLocation;

import java.time.LocalDateTime;

@Data
@Builder
public class VehicleLocationDTO {
    private Long vehicleId;
    private String licensePlate;
    private Double latitude;
    private Double longitude;
    private Double totalDistance;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime lastTelemetryUpdate;
    private boolean online;
    private String currentState;

    public static VehicleLocationDTO fromEntity(VehicleLocation location) {
        return VehicleLocationDTO.builder()
                .vehicleId(location.getVehicle().getId())
                .licensePlate(location.getVehicle().getLicensePlate())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .totalDistance(location.getTotalDistance())
                .lastHeartbeat(location.getLastHeartbeat())
                .lastTelemetryUpdate(location.getLastTelemetryUpdate())
                .online(location.isOnline())
                .currentState(location.getCurrentState())
                .build();
    }
}
