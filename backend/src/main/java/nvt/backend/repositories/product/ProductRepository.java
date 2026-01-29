package nvt.backend.repositories.product;

import nvt.backend.model.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.factories " +
            "WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.images ")
    List<Product> findAllWithDetails();

    @Query("SELECT p.id FROM Product p WHERE p.active = true")
    Page<Long> findAllActiveIds(Pageable pageable);

    /**
     * Count active products - cached separately for performance
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    long countActiveProducts();

    /**
     * Fast pagination queries by different sort columns - uses indexes
     */
    @Query(value = "SELECT p.id FROM products p WHERE p.active = true ORDER BY p.id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findActiveIdsByPageIdAsc(@Param("offset") long offset, @Param("limit") int limit);

    @Query(value = "SELECT p.id FROM products p WHERE p.active = true ORDER BY p.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findActiveIdsByPageIdDesc(@Param("offset") long offset, @Param("limit") int limit);

    @Query(value = "SELECT p.id FROM products p WHERE p.active = true ORDER BY p.name ASC, p.id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findActiveIdsByPageNameAsc(@Param("offset") long offset, @Param("limit") int limit);

    @Query(value = "SELECT p.id FROM products p WHERE p.active = true ORDER BY p.name DESC, p.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findActiveIdsByPageNameDesc(@Param("offset") long offset, @Param("limit") int limit);

    @Query(value = "SELECT p.id FROM products p WHERE p.active = true ORDER BY p.price ASC, p.id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findActiveIdsByPagePriceAsc(@Param("offset") long offset, @Param("limit") int limit);

    @Query(value = "SELECT p.id FROM products p WHERE p.active = true ORDER BY p.price DESC, p.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findActiveIdsByPagePriceDesc(@Param("offset") long offset, @Param("limit") int limit);

    default List<Long> findActiveIdsByPage(long offset, int limit, String sortBy, boolean descending) {
        return switch (sortBy.toLowerCase()) {
            case "name" -> descending ? findActiveIdsByPageNameDesc(offset, limit) : findActiveIdsByPageNameAsc(offset, limit);
            case "price" -> descending ? findActiveIdsByPagePriceDesc(offset, limit) : findActiveIdsByPagePriceAsc(offset, limit);
            default -> descending ? findActiveIdsByPageIdDesc(offset, limit) : findActiveIdsByPageIdAsc(offset, limit);
        };
    }

    @Query("SELECT p.id FROM Product p")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.factories " +
            "WHERE p.id IN :ids")
    List<Product> findAllByIds(@Param("ids") List<Long> ids);

    boolean existsBySku(String sku);

    Optional<Product> findBySku(String sku);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND p.active = true ORDER BY p.category")
    List<String> findAllCategories();

    @Query(value = "SELECT p.id FROM products p " +
            "WHERE p.active = true " +
            "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:forSale IS NULL OR p.for_sale = :forSale)",
            countQuery = "SELECT COUNT(p.id) FROM products p " +
                    "WHERE p.active = true " +
                    "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:category IS NULL OR p.category = :category) " +
                    "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                    "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
                    "AND (:forSale IS NULL OR p.for_sale = :forSale)",
            nativeQuery = true)
    Page<Long> searchProductIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("forSale") Boolean forSale,
            @Param("active") Boolean active,
            Pageable pageable);
}
