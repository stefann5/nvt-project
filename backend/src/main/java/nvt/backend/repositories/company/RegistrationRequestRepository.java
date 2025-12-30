package nvt.backend.repositories.company;

import nvt.backend.model.company.RegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {

    @Query("SELECT r FROM RegistrationRequest r JOIN r.images img WHERE img.id = :imageId")
    Optional<RegistrationRequest> findByImageId(@Param("imageId") Long imageId);

    @Query("SELECT r FROM RegistrationRequest r JOIN r.documents doc WHERE doc.id = :documentId")
    Optional<RegistrationRequest> findByDocumentId(@Param("documentId") Long documentId);
}