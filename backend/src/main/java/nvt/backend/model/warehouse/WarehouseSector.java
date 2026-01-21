package nvt.backend.model.warehouse;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "warehouse_sectors", indexes = {
    @Index(name = "idx_sector_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_sector_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseSector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Double minTemperature;

    private Double maxTemperature;

    private Double currentTemperature;

    private Double capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;
}
