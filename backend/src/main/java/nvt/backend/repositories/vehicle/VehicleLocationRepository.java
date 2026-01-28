package nvt.backend.repositories.vehicle;

import nvt.backend.model.vehicle.VehicleLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, Long> {

    @Query("SELECT vl FROM VehicleLocation vl JOIN FETCH vl.vehicle WHERE vl.vehicle.id = :vehicleId")
    Optional<VehicleLocation> findByVehicleId(@Param("vehicleId") Long vehicleId);

    @Query("SELECT vl FROM VehicleLocation vl " +
            "JOIN FETCH vl.vehicle v " +
            "LEFT JOIN FETCH v.brand " +
            "LEFT JOIN FETCH v.model " +
            "WHERE vl.online = true")
    List<VehicleLocation> findAllOnline();

    @Query("SELECT vl FROM VehicleLocation vl " +
            "JOIN FETCH vl.vehicle v " +
            "LEFT JOIN FETCH v.brand " +
            "LEFT JOIN FETCH v.model " +
            "WHERE vl.online = true AND vl.lastHeartbeat < :threshold")
    List<VehicleLocation> findStaleOnlineVehicles(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT vl FROM VehicleLocation vl " +
            "JOIN FETCH vl.vehicle v " +
            "LEFT JOIN FETCH v.brand " +
            "LEFT JOIN FETCH v.model")
    List<VehicleLocation> findAllWithVehicleDetails();

    @Query("SELECT vl FROM VehicleLocation vl " +
            "JOIN FETCH vl.vehicle v " +
            "LEFT JOIN FETCH v.brand " +
            "LEFT JOIN FETCH v.model " +
            "WHERE v.id IN :vehicleIds")
    List<VehicleLocation> findByVehicleIds(@Param("vehicleIds") List<Long> vehicleIds);

    @Modifying
    @Query("UPDATE VehicleLocation vl SET vl.online = false WHERE vl.online = true AND vl.lastHeartbeat < :threshold")
    int markOfflineByHeartbeatThreshold(@Param("threshold") LocalDateTime threshold);
}
