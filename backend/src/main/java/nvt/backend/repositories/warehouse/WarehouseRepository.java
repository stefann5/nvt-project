package nvt.backend.repositories.warehouse;

import nvt.backend.model.warehouse.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    @Query("SELECT DISTINCT w FROM Warehouse w " +
            "LEFT JOIN FETCH w.country " +
            "LEFT JOIN FETCH w.city " +
            "LEFT JOIN FETCH w.sectors " +
            "LEFT JOIN FETCH w.images " +
            "WHERE w.id = :id")
    Optional<Warehouse> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT w FROM Warehouse w " +
            "LEFT JOIN FETCH w.country " +
            "LEFT JOIN FETCH w.city " +
            "LEFT JOIN FETCH w.sectors " +
            "WHERE w.active = true")
    List<Warehouse> findAllActiveWithDetails();

    @Query("SELECT DISTINCT w FROM Warehouse w " +
            "LEFT JOIN FETCH w.country " +
            "LEFT JOIN FETCH w.city " +
            "LEFT JOIN FETCH w.sectors")
    List<Warehouse> findAllWithDetails();

    @Query("SELECT w.id FROM Warehouse w")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT DISTINCT w FROM Warehouse w " +
            "LEFT JOIN FETCH w.country " +
            "LEFT JOIN FETCH w.city " +
            "LEFT JOIN FETCH w.sectors " +
            "WHERE w.id IN :ids")
    List<Warehouse> findAllByIds(@Param("ids") List<Long> ids);

    @Query(value = "SELECT DISTINCT w.* FROM warehouses w " +
            "LEFT JOIN countries c ON c.id = w.country_id " +
            "LEFT JOIN cities ci ON ci.id = w.city_id " +
            "WHERE w.name ILIKE CONCAT('%', :search, '%') " +
            "OR c.name ILIKE CONCAT('%', :search, '%') " +
            "OR ci.name ILIKE CONCAT('%', :search, '%') " +
            "OR w.street ILIKE CONCAT('%', :search, '%')", nativeQuery = true)
    List<Warehouse> searchWarehouses(@Param("search") String search);

    @Query(value = "SELECT w.id FROM warehouses w " +
            "LEFT JOIN countries c ON c.id = w.country_id " +
            "LEFT JOIN cities ci ON ci.id = w.city_id " +
            "WHERE w.name ILIKE CONCAT('%', :search, '%') " +
            "OR c.name ILIKE CONCAT('%', :search, '%') " +
            "OR ci.name ILIKE CONCAT('%', :search, '%') " +
            "OR w.street ILIKE CONCAT('%', :search, '%')",
            countQuery = "SELECT COUNT(w.id) FROM warehouses w " +
            "LEFT JOIN countries c ON c.id = w.country_id " +
            "LEFT JOIN cities ci ON ci.id = w.city_id " +
            "WHERE w.name ILIKE CONCAT('%', :search, '%') " +
            "OR c.name ILIKE CONCAT('%', :search, '%') " +
            "OR ci.name ILIKE CONCAT('%', :search, '%') " +
            "OR w.street ILIKE CONCAT('%', :search, '%')",
            nativeQuery = true)
    Page<Long> searchWarehouseIds(@Param("search") String search, Pageable pageable);

    @Query("SELECT w FROM Warehouse w JOIN w.images img WHERE img.id = :imageId")
    Optional<Warehouse> findByImageId(@Param("imageId") Long imageId);

    boolean existsByName(String name);

    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Warehouse w " +
            "WHERE w.name = :name AND w.id != :warehouseId")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("warehouseId") Long warehouseId);

    List<Warehouse> findByActiveTrue();

    @Query("SELECT w FROM Warehouse w WHERE w.online = true AND w.lastHeartbeat < :threshold")
    List<Warehouse> findOnlineWarehousesWithOldHeartbeat(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("UPDATE Warehouse w SET w.online = false WHERE w.online = true AND w.lastHeartbeat < :threshold")
    int markWarehousesOffline(@Param("threshold") LocalDateTime threshold);
}
