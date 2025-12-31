package nvt.backend.repositories.vehicle;

import nvt.backend.model.vehicle.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @Query("SELECT DISTINCT v FROM Vehicle v " +
            "LEFT JOIN FETCH v.brand " +
            "LEFT JOIN FETCH v.model " +
            "LEFT JOIN FETCH v.images")
    List<Vehicle> findAllWithDetails();

    @Query("SELECT DISTINCT v FROM Vehicle v " +
            "LEFT JOIN FETCH v.brand " +
            "LEFT JOIN FETCH v.model " +
            "LEFT JOIN FETCH v.images " +
            "WHERE v.id = :id")
    Optional<Vehicle> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT v FROM Vehicle v " +
            "LEFT JOIN FETCH v.brand b " +
            "LEFT JOIN FETCH v.model m " +
            "LEFT JOIN FETCH v.images " +
            "WHERE LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Vehicle> searchVehicles(@Param("search") String search);

    @Query("SELECT v FROM Vehicle v JOIN v.images img WHERE img.id = :imageId")
    Optional<Vehicle> findByImageId(@Param("imageId") Long imageId);

    boolean existsByLicensePlate(String licensePlate);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Vehicle v " +
            "WHERE v.licensePlate = :licensePlate AND v.id != :vehicleId")
    boolean existsByLicensePlateAndIdNot(@Param("licensePlate") String licensePlate, @Param("vehicleId") Long vehicleId);
}