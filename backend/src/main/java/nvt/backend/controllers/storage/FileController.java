package nvt.backend.controllers.storage;

import lombok.RequiredArgsConstructor;
import nvt.backend.model.company.CompanyDocument;
import nvt.backend.model.company.CompanyImage;
import nvt.backend.repositories.company.RegistrationRequestRepository;
import nvt.backend.services.storage.MinioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioService minioService;
    private final RegistrationRequestRepository registrationRequestRepository;

    @GetMapping("/image/{imageId}/url")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER')")
    public ResponseEntity<Map<String, String>> getImageUrl(@PathVariable Long imageId) {
        var request = registrationRequestRepository.findByImageId(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        CompanyImage image = request.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image not found"));

        // Generate presigned URL valid for 60 minutes
        String url = minioService.getPresignedUrl(
                image.getMinioBucket(),
                image.getMinioPath(),
                60
        );

        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("originalName", image.getOriginalName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/document/{documentId}/url")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER')")
    public ResponseEntity<Map<String, String>> getDocumentUrl(@PathVariable Long documentId) {
        var request = registrationRequestRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        CompanyDocument document = request.getDocuments().stream()
                .filter(doc -> doc.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Document not found"));

        String url = minioService.getPresignedUrl(
                document.getMinioBucket(),
                document.getMinioPath(),
                60
        );

        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("originalName", document.getOriginalName());
        response.put("contentType", document.getContentType());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/request/{requestId}/files")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER')")
    public ResponseEntity<Map<String, Object>> getRequestFiles(@PathVariable Long requestId) {
        var request = registrationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Map<String, Object> response = new HashMap<>();

        // Generate URLs for all images
        var imageUrls = request.getImages().stream()
                .map(img -> Map.of(
                        "id", img.getId(),
                        "originalName", img.getOriginalName(),
                        "url", minioService.getPresignedUrl(img.getMinioBucket(), img.getMinioPath(), 60)
                ))
                .toList();

        // Generate URLs for all documents
        var documentUrls = request.getDocuments().stream()
                .map(doc -> Map.of(
                        "id", doc.getId(),
                        "originalName", doc.getOriginalName(),
                        "contentType", doc.getContentType(),
                        "url", minioService.getPresignedUrl(doc.getMinioBucket(), doc.getMinioPath(), 60)
                ))
                .toList();

        response.put("images", imageUrls);
        response.put("documents", documentUrls);

        return ResponseEntity.ok(response);
    }
}