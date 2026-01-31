package nvt.backend.services.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.product.*;
import nvt.backend.exceptions.ResourceNotFoundException;
import nvt.backend.exceptions.DuplicateResourceException;
import nvt.backend.model.factory.Factory;
import nvt.backend.model.product.Product;
import nvt.backend.model.product.ProductImage;
import nvt.backend.repositories.factory.FactoryRepository;
import nvt.backend.repositories.product.ProductImageRepository;
import nvt.backend.repositories.product.ProductRepository;
import nvt.backend.repositories.warehouse.InventoryRepository;
import nvt.backend.services.storage.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final FactoryRepository factoryRepository;
    private final InventoryRepository inventoryRepository;
    private final MinioService minioService;

    @Value("${minio.bucket.product-images:product-images}")
    private String productImagesBucket;

    @Transactional(readOnly = true)
    @Cacheable(value = "productById", key = "#id")
    public ProductResponseDTO getById(Long id) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Integer totalQuantity = inventoryRepository.getTotalAvailableQuantityByProductId(id);
        return ProductResponseDTO.fromEntityWithQuantity(product, totalQuantity);
    }

    /**
     * Get total active product count with caching - expensive COUNT query cached separately
     */
    @Cacheable(value = "productCount")
    public long getTotalActiveProductCount() {
        return productRepository.countActiveProducts();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productsPage", key = "#page + '-' + #size + '-' + #sortBy + '-' + #sortDir")
    public PageResponseDTO<ProductListDTO> getAllPaged(int page, int size, String sortBy, String sortDir) {
        // Use cached total count to avoid expensive COUNT query on every request
        long totalElements = getTotalActiveProductCount();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Fetch only IDs for the current page (fast query)
        long offset = (long) page * size;
        List<Long> ids = productRepository.findActiveIdsByPage(offset, size, sortBy, sortDir.equalsIgnoreCase("desc"));

        if (ids.isEmpty()) {
            return PageResponseDTO.<ProductListDTO>builder()
                    .content(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .first(page == 0)
                    .last(true)
                    .build();
        }

        List<Product> products = productRepository.findAllByIds(ids);
        Map<Long, Integer> quantities = getQuantitiesForProductsBatch(ids);

        List<ProductListDTO> content = products.stream()
                .map(p -> ProductListDTO.fromEntity(p, quantities.getOrDefault(p.getId(), 0)))
                .toList();

        return PageResponseDTO.<ProductListDTO>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ProductListDTO> getAllPagedForManager(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Long> idPage = productRepository.findAllIds(pageable);
        return buildPageResponse(idPage, page, size);
    }

    @Transactional(readOnly = true)
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
        Boolean active = dto.getActive() != null ? dto.getActive() : true;

        Page<Long> idPage = productRepository.searchProductIds(
                search,
                dto.getCategory(),
                dto.getMinPrice(),
                dto.getMaxPrice(),
                dto.getForSale(),
                active,
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
        Map<Long, Integer> quantities = getQuantitiesForProductsBatch(idPage.getContent());

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

    @Transactional(readOnly = true)
    @Cacheable(value = "productCategories")
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    public Integer getAvailableQuantity(Long productId) {
        return inventoryRepository.getTotalAvailableQuantityByProductId(productId);
    }

    /**
     * Batch load quantities for multiple products in a single query.
     * This replaces N+1 queries with a single query for much better performance.
     */
    private Map<Long, Integer> getQuantitiesForProductsBatch(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> results = inventoryRepository.getTotalAvailableQuantitiesByProductIds(productIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "productsPage", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true),
            @CacheEvict(value = "productCategories", allEntries = true)
    })
    public ProductResponseDTO create(ProductCreateDTO dto, List<MultipartFile> images) {
        if (productRepository.existsBySku(dto.getSku())) {
            throw new DuplicateResourceException("Product with SKU " + dto.getSku() + " already exists");
        }

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setSku(dto.getSku());
        product.setPrice(dto.getPrice());
        product.setWeight(dto.getWeight());
        product.setUnit(dto.getUnit() != null ? dto.getUnit() : "kom");
        product.setForSale(dto.isForSale());
        product.setActive(true);
        product.setCategory(dto.getCategory());

        // Set factories
        if (dto.getFactoryIds() != null && !dto.getFactoryIds().isEmpty()) {
            Set<Factory> factories = factoryRepository.findByIdIn(dto.getFactoryIds());
            product.setFactories(factories);
        }

        product = productRepository.save(product);

        // Upload images
        if (images != null && !images.isEmpty()) {
            uploadImages(product, images);
        }

        return ProductResponseDTO.fromEntity(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "productById", key = "#id"),
            @CacheEvict(value = "productsPage", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    public ProductResponseDTO update(Long id, ProductUpdateDTO dto, List<MultipartFile> newImages) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (dto.getName() != null) {
            product.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }
        if (dto.getWeight() != null) {
            product.setWeight(dto.getWeight());
        }
        if (dto.getUnit() != null) {
            product.setUnit(dto.getUnit());
        }
        if (dto.getForSale() != null) {
            product.setForSale(dto.getForSale());
        }
        if (dto.getActive() != null) {
            product.setActive(dto.getActive());
        }
        if (dto.getCategory() != null) {
            product.setCategory(dto.getCategory());
        }

        // Update factories
        if (dto.getFactoryIds() != null) {
            Set<Factory> factories = factoryRepository.findByIdIn(dto.getFactoryIds());
            product.setFactories(factories);
        }

        // Delete marked images
        if (dto.getImagesToDelete() != null && !dto.getImagesToDelete().isEmpty()) {
            deleteImages(product, dto.getImagesToDelete());
        }

        // Upload new images
        if (newImages != null && !newImages.isEmpty()) {
            uploadImages(product, newImages);
        }

        product = productRepository.save(product);
        return ProductResponseDTO.fromEntity(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "productById", key = "#id"),
            @CacheEvict(value = "productsPage", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    public void delete(Long id) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Delete all images from MinIO
        for (ProductImage image : product.getImages()) {
            try {
                minioService.deleteFile(productImagesBucket, image.getMinioPath());
            } catch (Exception e) {
                log.error("Failed to delete image from MinIO: {}", image.getMinioPath(), e);
            }
        }

        productRepository.delete(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "productById", key = "#id"),
            @CacheEvict(value = "productsPage", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    public ProductResponseDTO toggleForSale(Long id) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setForSale(!product.isForSale());
        product = productRepository.save(product);
        return ProductResponseDTO.fromEntity(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "productById", key = "#id"),
            @CacheEvict(value = "productsPage", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    public ProductResponseDTO toggleActive(Long id) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setActive(!product.isActive());
        product = productRepository.save(product);
        return ProductResponseDTO.fromEntity(product);
    }

    // Helper methods
    private void uploadImages(Product product, List<MultipartFile> images) {
        for (MultipartFile image : images) {
            try {
                String minioPath = minioService.uploadFile(image, productImagesBucket, "products/" + product.getId());

                ProductImage productImage = new ProductImage();
                productImage.setOriginalName(image.getOriginalFilename());
                productImage.setMinioPath(minioPath);
                productImage.setMinioBucket(productImagesBucket);
                productImage.setContentType(image.getContentType());
                productImage.setProduct(product);

                product.getImages().add(productImage);
            } catch (Exception e) {
                log.error("Failed to upload image: {}", image.getOriginalFilename(), e);
            }
        }
    }

    private void deleteImages(Product product, List<Long> imageIds) {
        List<ProductImage> imagesToRemove = product.getImages().stream()
                .filter(img -> imageIds.contains(img.getId()))
                .toList();

        for (ProductImage image : imagesToRemove) {
            try {
                minioService.deleteFile(productImagesBucket, image.getMinioPath());
            } catch (Exception e) {
                log.error("Failed to delete image from MinIO: {}", image.getMinioPath(), e);
            }
            product.getImages().remove(image);
        }
    }

    private PageResponseDTO<ProductListDTO> buildPageResponse(Page<Long> idPage, int page, int size) {
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

    private Map<Long, Integer> getQuantitiesForProducts(List<Long> productIds) {
        return productIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> inventoryRepository.getTotalAvailableQuantityByProductId(id)
                ));
    }
}
