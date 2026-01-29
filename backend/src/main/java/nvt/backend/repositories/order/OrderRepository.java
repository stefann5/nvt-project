package nvt.backend.repositories.order;

import nvt.backend.model.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.company c " +
            "LEFT JOIN FETCH c.country " +
            "LEFT JOIN FETCH c.city " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.product " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT o.id FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    Page<Long> findIdsByCustomerId(@Param("customerId") Integer customerId, Pageable pageable);

    @Query("SELECT o.id FROM Order o ORDER BY o.createdAt DESC")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.company c " +
            "LEFT JOIN FETCH c.country " +
            "LEFT JOIN FETCH c.city " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.product " +
            "WHERE o.id IN :ids " +
            "ORDER BY o.createdAt DESC")
    List<Order> findAllByIds(@Param("ids") List<Long> ids);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    List<Order> findByCustomerId(@Param("customerId") Integer customerId);

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);
}
