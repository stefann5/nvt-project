package nvt.backend.model.factory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "factory_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactoryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;

    private String minioPath;

    private String minioBucket;

    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Factory factory;
}
