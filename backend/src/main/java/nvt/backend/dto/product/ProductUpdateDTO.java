package nvt.backend.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateDTO {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Weight must be positive")
    private BigDecimal weight;

    private String category;

    @Size(max = 20, message = "Unit cannot exceed 20 characters")
    private String unit;

    private Boolean forSale;

    private Boolean active;

    // IDs of factories where this product is manufactured
    private List<Long> factoryIds;

    // IDs of images to delete
    private List<Long> imagesToDelete;
}
