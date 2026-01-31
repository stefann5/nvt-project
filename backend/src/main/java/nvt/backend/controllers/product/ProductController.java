package nvt.backend.controllers.product;

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
public class ProductController {

    private final ProductService productService;
    private final MinioService minioService;

    @Value("${minio.bucket.product-images:product-images}")
    private String productImagesBucket;

    // ==================== PUBLIC/CUSTOMER ENDPOINTS ====================

    @GetMapping("/paged")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<ProductListDTO>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getAllPaged(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<ProductListDTO>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean forSale,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
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

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Integer> getAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getAvailableQuantity(id));
    }

    @GetMapping("/images/")
    public ResponseEntity<String> getImageUrl(@RequestParam(required = true) String imagePath) {
        String presignedUrl = minioService.getPresignedUrl(productImagesBucket, imagePath, 60);
        return ResponseEntity.ok(presignedUrl);
    }

    // ==================== MANAGER ENDPOINTS ====================

    @GetMapping("/manager/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<ProductListDTO>> getAllPagedForManager(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(productService.getAllPagedForManager(page, size, sortBy, sortDir));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> create(
            @Valid @RequestPart("product") ProductCreateDTO productCreateDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(productCreateDTO, images));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestPart("product") ProductUpdateDTO productUpdateDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.ok(productService.update(id, productUpdateDTO, images));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-for-sale")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> toggleForSale(@PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleForSale(id));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleActive(id));
    }
}
