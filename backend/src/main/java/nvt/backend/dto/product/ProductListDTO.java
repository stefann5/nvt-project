package nvt.backend.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.product.Product;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListDTO implements Serializable {
    private Long id;
    private String name;
    private String sku;
    private BigDecimal price;
    private BigDecimal weight;
    private String category;
    private String unit;
    private boolean forSale;
    private boolean active;
    private Integer totalQuantity;
    private String imageUrl;
    private int factoryCount;

    public static ProductListDTO fromEntity(Product product, Integer totalQuantity) {
        String imageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageUrl = product.getImages().iterator().next().getMinioPath();
        }

        return ProductListDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .price(product.getPrice())
                .weight(product.getWeight())
                .category(product.getCategory())
                .unit(product.getUnit())
                .forSale(product.isForSale())
                .active(product.isActive())
                .totalQuantity(totalQuantity)
                .imageUrl(imageUrl)
                .factoryCount(product.getFactories() != null ? product.getFactories().size() : 0)
                .build();
    }
}
