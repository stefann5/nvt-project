package nvt.backend.repositories.warehouse;

import nvt.backend.model.warehouse.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query("SELECT i FROM Inventory i " +
            "JOIN FETCH i.product " +
            "JOIN FETCH i.warehouse " +
            "WHERE i.product.id = :productId")
    List<Inventory> findByProductId(@Param("productId") Long productId);

    @Query("SELECT i FROM Inventory i " +
            "JOIN FETCH i.product " +
            "JOIN FETCH i.warehouse " +
            "WHERE i.warehouse.id = :warehouseId")
    List<Inventory> findByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT COALESCE(SUM(i.quantity - i.reservedQuantity), 0) FROM Inventory i WHERE i.product.id = :productId")
    Integer getTotalAvailableQuantityByProductId(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.product.id = :productId")
    Integer getTotalQuantityByProductId(@Param("productId") Long productId);

    /**
     * Batch query to get available quantities for multiple products in a single query.
     * Returns Object[] with [productId, availableQuantity]
     * This avoids N+1 queries when loading product lists.
     */
    @Query("SELECT i.product.id, COALESCE(SUM(i.quantity - i.reservedQuantity), 0) " +
            "FROM Inventory i WHERE i.product.id IN :productIds " +
            "GROUP BY i.product.id")
    List<Object[]> getTotalAvailableQuantitiesByProductIds(@Param("productIds") List<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId")
    Optional<Inventory> findByProductIdAndWarehouseIdForUpdate(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    Optional<Inventory> findFirstByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND (i.quantity - i.reservedQuantity) > 0 ORDER BY i.quantity DESC")
    List<Inventory> findAvailableInventoryForProduct(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :quantity WHERE i.id = :inventoryId AND i.quantity >= :quantity")
    int decreaseQuantity(@Param("inventoryId") Long inventoryId, @Param("quantity") Integer quantity);

    /**
     * Increase inventory quantity for a specific product in a warehouse.
     * Used when factory produces items and stores them in warehouse.
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :quantity, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.warehouse.id = :warehouseId AND i.product.id = :productId")
    int addQuantity(@Param("warehouseId") Long warehouseId, 
                    @Param("productId") Long productId, 
                    @Param("quantity") Integer quantity);

    /**
     * Find inventory by warehouse and product with sector info.
     */
    @Query("SELECT i FROM Inventory i " +
           "LEFT JOIN FETCH i.sector " +
           "WHERE i.warehouse.id = :warehouseId AND i.product.id = :productId")
    Optional<Inventory> findByWarehouseIdAndProductIdWithSector(
            @Param("warehouseId") Long warehouseId, 
            @Param("productId") Long productId);
}
