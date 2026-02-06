package nvt.backend.controllers.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.product.*;
import nvt.backend.services.product.ProductService;
import nvt.backend.services.storage.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Product management endpoints")
public class ProductController {

    private final ProductService productService;
    private final MinioService minioService;

    @Value("${minio.bucket.product-images:product-images}")
    private String productImagesBucket;

    // ==================== PUBLIC/CUSTOMER ENDPOINTS ====================

    @Operation(
            summary = "Get all products (paginated)",
            description = "Retrieves products with pagination and sorting. Requires CUSTOMER, MANAGER, or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/paged")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<ProductListDTO>> getAllPaged(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "name")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getAllPaged(page, size, sortBy, sortDir));
    }

    @Operation(
            summary = "Get product by ID",
            description = "Retrieves a specific product by its ID. Requires CUSTOMER, MANAGER, or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> getById(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @Operation(
            summary = "Search products",
            description = "Searches products with various filters and pagination. Requires CUSTOMER, MANAGER, or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<ProductListDTO>> search(
            @Parameter(description = "Search query for product name/description")
            @RequestParam(required = false) String search,
            @Parameter(description = "Category filter")
            @RequestParam(required = false) String category,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "In stock filter")
            @RequestParam(required = false) Boolean inStock,
            @Parameter(description = "For sale filter")
            @RequestParam(required = false) Boolean forSale,
            @Parameter(description = "Active status filter")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "name")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {

        ProductSearchDTO searchDto = ProductSearchDTO.builder()
                .search(search)
                .category(category)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .inStock(inStock)
                .forSale(forSale)
                .active(active)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        return ResponseEntity.ok(productService.search(searchDto));
    }

    @Operation(
            summary = "Get all product categories",
            description = "Retrieves a list of all available product categories. Requires CUSTOMER, MANAGER, or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @Operation(
            summary = "Get product availability",
            description = "Retrieves the available quantity for a specific product. Requires CUSTOMER, MANAGER, or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Integer.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Integer> getAvailability(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(productService.getAvailableQuantity(id));
    }

    @Operation(
            summary = "Get product image URL",
            description = "Retrieves a presigned URL for a product image"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image URL retrieved successfully",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/images/")
    public ResponseEntity<String> getImageUrl(
            @Parameter(description = "Image path in storage", required = true)
            @RequestParam(required = true) String imagePath) {
        String presignedUrl = minioService.getPresignedUrl(productImagesBucket, imagePath, 60);
        return ResponseEntity.ok(presignedUrl);
    }

    // ==================== MANAGER ENDPOINTS ====================

    @Operation(
            summary = "Get all products for manager (paginated)",
            description = "Retrieves all products including inactive ones with pagination and sorting. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/manager/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<ProductListDTO>> getAllPagedForManager(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(productService.getAllPagedForManager(page, size, sortBy, sortDir));
    }

    @Operation(
            summary = "Create a new product",
            description = "Creates a new product with the provided data and optional images. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> create(
            @Parameter(description = "Product data", required = true)
            @Valid @RequestPart("product") ProductCreateDTO productCreateDTO,
            @Parameter(description = "Product images (optional)")
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(productCreateDTO, images));
    }

    @Operation(
            summary = "Update a product",
            description = "Updates an existing product with new data and optional new images. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> update(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated product data", required = true)
            @Valid @RequestPart("product") ProductUpdateDTO productUpdateDTO,
            @Parameter(description = "New product images (optional)")
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.ok(productService.update(id, productUpdateDTO, images));
    }

    @Operation(
            summary = "Delete a product",
            description = "Deletes a product by ID. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Toggle product for sale status",
            description = "Toggles whether a product is available for sale. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "For sale status toggled successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PatchMapping("/{id}/toggle-for-sale")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> toggleForSale(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleForSale(id));
    }

    @Operation(
            summary = "Toggle product active status",
            description = "Toggles whether a product is active. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active status toggled successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> toggleActive(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleActive(id));
    }
}
