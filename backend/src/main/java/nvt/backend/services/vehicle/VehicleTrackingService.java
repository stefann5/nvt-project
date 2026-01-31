package nvt.backend.services.vehicle;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.vehicle.VehicleStatusDTO;
import nvt.backend.model.vehicle.Vehicle;
import nvt.backend.model.vehicle.VehicleLocation;
import nvt.backend.repositories.vehicle.VehicleLocationRepository;
import nvt.backend.repositories.vehicle.VehicleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleTrackingService {

    private final VehicleRepository vehicleRepository;
    private final VehicleLocationRepository locationRepository;

    /**
     * Get all vehicle statuses - optimized with batch loading and caching
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "vehicleStatusAll", key = "'all'")
    public List<VehicleStatusDTO> getAllVehicleStatus() {
        // Fetch all locations with vehicle details in ONE query (eliminates N+1)
        List<VehicleLocation> allLocations = locationRepository.findAllWithVehicleDetails();

        // Build a map for O(1) lookup by vehicle ID
        Map<Long, VehicleLocation> locationMap = allLocations.stream()
                .collect(Collectors.toMap(
                        loc -> loc.getVehicle().getId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        // Fetch all vehicles with details
        List<Vehicle> vehicles = vehicleRepository.findAllWithDetails();

        // Build DTOs using the map (no additional queries)
        return vehicles.stream()
                .map(vehicle -> buildStatusDTO(vehicle, locationMap.get(vehicle.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get status for a single vehicle
     */
    @Transactional(readOnly = true)
    public VehicleStatusDTO getVehicleStatus(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdWithDetails(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        VehicleLocation location = locationRepository.findByVehicleId(vehicleId).orElse(null);

        return buildStatusDTO(vehicle, location);
    }

    /**
     * Get only online vehicles - optimized with JOIN FETCH and caching
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "vehicleStatusOnline", key = "'online'")
    public List<VehicleStatusDTO> getOnlineVehicles() {
        // Fetches vehicle with JOIN FETCH
        List<VehicleLocation> onlineLocations = locationRepository.findAllOnline();

        return onlineLocations.stream()
                .map(location -> buildStatusDTO(location.getVehicle(), location))
                .collect(Collectors.toList());
    }

    /**
     * Build VehicleStatusDTO from vehicle and pre-fetched location (no additional DB queries)
     */
    private VehicleStatusDTO buildStatusDTO(Vehicle vehicle, VehicleLocation location) {
        VehicleStatusDTO.VehicleStatusDTOBuilder builder = VehicleStatusDTO.builder()
                .vehicleId(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .brandName(vehicle.getBrand() != null ? vehicle.getBrand().getName() : null)
                .modelName(vehicle.getModel() != null ? vehicle.getModel().getName() : null);

        if (location != null) {
            builder.online(location.isOnline())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .totalDistance(location.getTotalDistance())
                    .lastHeartbeat(location.getLastHeartbeat())
                    .lastTelemetry(location.getLastTelemetryUpdate())
                    .currentState(location.getCurrentState());
        } else {
            builder.online(false);
        }

        return builder.build();
    }
}
