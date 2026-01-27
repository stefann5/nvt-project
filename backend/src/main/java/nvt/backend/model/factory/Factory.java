package nvt.backend.model.factory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.model.product.Product;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "factories", indexes = {
    @Index(name = "idx_factory_name", columnList = "name"),
    @Index(name = "idx_factory_country", columnList = "country_id"),
    @Index(name = "idx_factory_city", columnList = "city_id"),
    @Index(name = "idx_factory_online", columnList = "isOnline")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Factory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false)
    private String street;

    private String streetNumber;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private boolean isOnline = false;

    private LocalDateTime lastHeartbeat;

    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "factory", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FactoryImage> images = new HashSet<>();

    @ManyToMany(mappedBy = "factories")
    private Set<Product> products = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
