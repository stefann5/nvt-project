package nvt.backend.model.warehouse;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "warehouses", indexes = {
    @Index(name = "idx_warehouse_name", columnList = "name"),
    @Index(name = "idx_warehouse_country", columnList = "country_id"),
    @Index(name = "idx_warehouse_city", columnList = "city_id"),
    @Index(name = "idx_warehouse_online", columnList = "online")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {

    @Id
    @TableGenerator(
            name = "warehouse_gen",
            table = "id_generator",
            pkColumnName = "sequence_name",
            valueColumnName = "next_val"
    )
    @GeneratedValue(strategy = GenerationType.TABLE,generator = "warehouse_gen")
    private Long id;

    @Version
    @Column(columnDefinition = "bigint default 0")
    private Long version = 0L;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    private String street;

    private String streetNumber;

    private Double latitude;

    private Double longitude;

    private Double totalCapacity;

    private boolean active = true;

    @Column(nullable = false)
    private boolean online = false;

    private LocalDateTime lastHeartbeat;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<WarehouseSector> sectors = new HashSet<>();

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WarehouseImage> images = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
