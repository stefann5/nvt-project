package nvt.backend.model.warehouse;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "warehouse_images")
@Data
public class WarehouseImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;
    private String minioPath;
    private String minioBucket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Warehouse warehouse;
}
