package nvt.backend.repositories.factory;

import nvt.backend.model.factory.Factory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FactoryRepository extends JpaRepository<Factory, Long> {

    @Query("SELECT f FROM Factory f " +
            "LEFT JOIN FETCH f.country " +
            "LEFT JOIN FETCH f.city " +
            "LEFT JOIN FETCH f.images " +
            "WHERE f.id = :id")
    Optional<Factory> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT f.id FROM Factory f WHERE f.active = true")
    Page<Long> findAllActiveIds(Pageable pageable);

    @Query("SELECT DISTINCT f FROM Factory f " +
            "LEFT JOIN FETCH f.country " +
            "LEFT JOIN FETCH f.city " +
            "LEFT JOIN FETCH f.images " +
            "WHERE f.id IN :ids")
    List<Factory> findAllByIds(@Param("ids") List<Long> ids);

    @Query("SELECT f FROM Factory f WHERE f.id IN :ids")
    Set<Factory> findByIdIn(@Param("ids") List<Long> ids);

    @Query(value = "SELECT f.id FROM factories f " +
            "WHERE f.active = true " +
            "AND (:search IS NULL OR f.name ILIKE CONCAT('%', :search, '%'))",
            countQuery = "SELECT COUNT(f.id) FROM factories f " +
                    "WHERE f.active = true " +
                    "AND (:search IS NULL OR f.name ILIKE CONCAT('%', :search, '%'))",
            nativeQuery = true)
    Page<Long> searchFactoryIds(@Param("search") String search, Pageable pageable);

    @Query("SELECT f FROM Factory f WHERE f.active = true ORDER BY f.name")
    List<Factory> findAllActiveOrderByName();
}
