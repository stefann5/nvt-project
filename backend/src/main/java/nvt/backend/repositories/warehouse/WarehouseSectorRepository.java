package nvt.backend.repositories.warehouse;

import nvt.backend.model.warehouse.WarehouseSector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarehouseSectorRepository extends JpaRepository<WarehouseSector, Long> {

    List<WarehouseSector> findByWarehouseId(Long warehouseId);

    @Query("SELECT s FROM WarehouseSector s WHERE s.warehouse.id = :warehouseId AND s.name = :name")
    Optional<WarehouseSector> findByWarehouseIdAndName(@Param("warehouseId") Long warehouseId, @Param("name") String name);

    @Query("SELECT s FROM WarehouseSector s JOIN FETCH s.warehouse WHERE s.id = :id")
    Optional<WarehouseSector> findByIdWithWarehouse(@Param("id") Long id);

    void deleteByWarehouseId(Long warehouseId);
}
