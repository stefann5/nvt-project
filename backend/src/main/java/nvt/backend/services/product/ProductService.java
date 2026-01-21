package nvt.backend.services.product;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.product.ProductListDTO;
import nvt.backend.dto.product.ProductResponseDTO;
import nvt.backend.dto.product.ProductSearchDTO;
import nvt.backend.model.product.Product;
import nvt.backend.repositories.product.ProductRepository;
import nvt.backend.repositories.warehouse.InventoryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Cacheable(value = "productById", key = "#id")
    public ProductResponseDTO getById(Long id) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Integer totalQuantity = inventoryRepository.getTotalAvailableQuantityByProductId(id);
        return ProductResponseDTO.fromEntityWithQuantity(product, totalQuantity);
    }

    @Cacheable(value = "productsPage", key = "#page + '-' + #size + '-' + #sortBy + '-' + #sortDir")
    public PageResponseDTO<ProductListDTO> getAllPaged(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Long> idPage = productRepository.findAllActiveIds(pageable);
        if (idPage.getContent().isEmpty()) {
            return PageResponseDTO.<ProductListDTO>builder()
                    .content(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        List<Product> products = productRepository.findAllByIds(idPage.getContent());
        Map<Long, Integer> quantities = getQuantitiesForProducts(idPage.getContent());

        List<ProductListDTO> content = products.stream()
                .map(p -> ProductListDTO.fromEntity(p, quantities.getOrDefault(p.getId(), 0)))
                .toList();

        return PageResponseDTO.<ProductListDTO>builder()
                .content(content)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    @Cacheable(value = "productSearch", key = "#dto.toString()")
    public PageResponseDTO<ProductListDTO> search(ProductSearchDTO dto) {
        String sortBy = dto.getSortBy() != null ? dto.getSortBy() : "name";
        String sortDir = dto.getSortDir() != null ? dto.getSortDir() : "asc";
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(
                dto.getPage() != null ? dto.getPage() : 0,
                dto.getSize() != null ? dto.getSize() : 20,
                sort
        );

        String search = dto.getSearch() != null && !dto.getSearch().isBlank() ? dto.getSearch() : null;
        String category = dto.getCategory() != null && !dto.getCategory().isBlank() ? dto.getCategory() : null;

        Page<Long> idPage = productRepository.searchProductIds(
                search,
                category,
                dto.getMinPrice(),
                dto.getMaxPrice(),
                pageable
        );

        if (idPage.getContent().isEmpty()) {
            return PageResponseDTO.<ProductListDTO>builder()
                    .content(List.of())
                    .page(dto.getPage() != null ? dto.getPage() : 0)
                    .size(dto.getSize() != null ? dto.getSize() : 20)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        List<Product> products = productRepository.findAllByIds(idPage.getContent());
        Map<Long, Integer> quantities = getQuantitiesForProducts(idPage.getContent());

        List<ProductListDTO> content;
        if (Boolean.TRUE.equals(dto.getInStock())) {
            content = products.stream()
                    .filter(p -> quantities.getOrDefault(p.getId(), 0) > 0)
                    .map(p -> ProductListDTO.fromEntity(p, quantities.get(p.getId())))
                    .toList();
        } else {
            content = products.stream()
                    .map(p -> ProductListDTO.fromEntity(p, quantities.getOrDefault(p.getId(), 0)))
                    .toList();
        }

        return PageResponseDTO.<ProductListDTO>builder()
                .content(content)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    @Cacheable(value = "productCategories")
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    public Integer getAvailableQuantity(Long productId) {
        return inventoryRepository.getTotalAvailableQuantityByProductId(productId);
    }

    private Map<Long, Integer> getQuantitiesForProducts(List<Long> productIds) {
        return productIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> inventoryRepository.getTotalAvailableQuantityByProductId(id)
                ));
    }
}
