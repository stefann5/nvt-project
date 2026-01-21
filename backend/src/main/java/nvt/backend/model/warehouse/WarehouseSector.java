package nvt.backend.model.warehouse;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @TableGenerator(
            name = "warehouse_sector_gen",
            table = "id_generator",
            pkColumnName = "sequence_name",
            valueColumnName = "next_val"
    )
    @GeneratedValue(strategy = GenerationType.TABLE,generator = "warehouse_sector_gen")
    private Long id;

    @Column(nullable = false)
    private String name;

    private Double minTemperature;

    private Double maxTemperature;

    private Double currentTemperature;

    private LocalDateTime lastTemperatureUpdate;

    private Double capacity;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "warehouse_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Warehouse warehouse;

    @OneToMany(
            mappedBy = "sector",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Inventory> inventory = new ArrayList<>();
}
