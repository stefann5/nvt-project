package nvt.backend.repositories.user;

import nvt.backend.model.user.Manager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Integer>{

    @Query("SELECT m FROM Manager m WHERE " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.surname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Manager> searchManagers(@Param("query") String query, Pageable pageable);
}