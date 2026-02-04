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

    /**
     * Fast pagination query by ID ascending - uses primary key index
     */
    @Query(value = "SELECT w.id FROM warehouses w ORDER BY w.id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findIdsByPageIdAsc(@Param("offset") long offset, @Param("limit") int limit);

    @Query(value = "SELECT w.id FROM warehouses w ORDER BY w.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findIdsByPageIdDesc(@Param("offset") long offset, @Param("limit") int limit);

    @Query(value = "SELECT w.id FROM warehouses w ORDER BY w.name ASC, w.id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findIdsByPageNameAsc(@Param("offset") long offset, @Param("limit") int limit);

    @Query(value = "SELECT w.id FROM warehouses w ORDER BY w.name DESC, w.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findIdsByPageNameDesc(@Param("offset") long offset, @Param("limit") int limit);

    @Query(value = "SELECT w.id FROM warehouses w ORDER BY w.created_at ASC, w.id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findIdsByPageCreatedAtAsc(@Param("offset") long offset, @Param("limit") int limit);

    @Query(value = "SELECT w.id FROM warehouses w ORDER BY w.created_at DESC, w.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findIdsByPageCreatedAtDesc(@Param("offset") long offset, @Param("limit") int limit);

    /**
     * Helper method to get IDs by page with dynamic sort
     */
    default List<Long> findIdsByPage(long offset, int limit, String sortBy, boolean descending) {
        return switch (sortBy.toLowerCase()) {
            case "name" -> descending ? findIdsByPageNameDesc(offset, limit) : findIdsByPageNameAsc(offset, limit);
            case "createdat", "created_at" -> descending ? findIdsByPageCreatedAtDesc(offset, limit) : findIdsByPageCreatedAtAsc(offset, limit);
            default -> descending ? findIdsByPageIdDesc(offset, limit) : findIdsByPageIdAsc(offset, limit);
        };
    }

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
            "WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(ci.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(w.street) LIKE LOWER(CONCAT('%', :search, '%'))",
            countQuery = "SELECT COUNT(w.id) FROM warehouses w " +
            "LEFT JOIN countries c ON c.id = w.country_id " +
            "LEFT JOIN cities ci ON ci.id = w.city_id " +
            "WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(ci.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(w.street) LIKE LOWER(CONCAT('%', :search, '%'))",
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

    /**
     * Find warehouse in the same city that stores a specific product.
     * Used when factory produces items and needs to store them in a nearby warehouse.
     */
    @Query("SELECT DISTINCT w FROM Warehouse w " +
           "JOIN w.sectors s " +
           "JOIN s.inventory i " +
           "WHERE w.city.id = :cityId " +
           "AND i.product.id = :productId " +
           "AND w.active = true")
    Optional<Warehouse> findByCityAndProduct(@Param("cityId") Long cityId, 
                                              @Param("productId") Long productId);

    /**
     * Find any active warehouse in a specific city.
     * Fallback when no warehouse with the product is found.
     */
    @Query("SELECT w FROM Warehouse w " +
           "WHERE w.city.id = :cityId " +
           "AND w.active = true " +
           "ORDER BY w.id ASC")
    List<Warehouse> findActiveByCityId(@Param("cityId") Long cityId);

    /**
     * Find any warehouse (globally) that stores a specific product and has available capacity.
     * Returns warehouses ordered by available capacity (most space first).
     */
    @Query("SELECT DISTINCT w FROM Warehouse w " +
           "JOIN w.sectors s " +
           "JOIN s.inventory i " +
           "WHERE i.product.id = :productId " +
           "AND w.active = true " +
           "AND (s.capacity IS NULL OR s.capacity >= (SELECT COALESCE(SUM(inv.quantity), 0) FROM Inventory inv WHERE inv.sector.id = s.id) + :requiredQuantity)")
    List<Warehouse> findByProductWithAvailableCapacity(@Param("productId") Long productId, 
                                                        @Param("requiredQuantity") Integer requiredQuantity);
}
