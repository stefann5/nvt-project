package nvt.backend.controllers.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nvt.backend.model.company.CompanyDocument;
import nvt.backend.model.company.CompanyImage;
import nvt.backend.model.factory.FactoryImage;
import nvt.backend.model.vehicle.VehicleImage;
import nvt.backend.model.warehouse.WarehouseImage;
import nvt.backend.repositories.company.RegistrationRequestRepository;
import nvt.backend.repositories.factory.FactoryRepository;
import nvt.backend.repositories.vehicle.VehicleRepository;
import nvt.backend.repositories.warehouse.WarehouseRepository;
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
@Tag(name = "File Storage", description = "File and image URL retrieval endpoints")
public class FileController {

    private final MinioService minioService;
    private final RegistrationRequestRepository registrationRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final WarehouseRepository warehouseRepository;
    private final FactoryRepository factoryRepository;

    @Operation(
            summary = "Get company image URL",
            description = "Retrieves a presigned URL for a company registration image. Requires ADMIN, CUSTOMER, or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image URL retrieved successfully",
                    content = @Content(schema = @Schema(example = "{\"url\": \"https://...\", \"originalName\": \"image.jpg\"}"))),
            @ApiResponse(responseCode = "404", description = "Image not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/image/{imageId}/url")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER', 'MANAGER')")
    public ResponseEntity<Map<String, String>> getImageUrl(
            @Parameter(description = "Image ID", required = true)
            @PathVariable Long imageId) {
        var request = registrationRequestRepository.findByImageId(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        CompanyImage image = request.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image not found"));

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

    @Operation(
            summary = "Get company document URL",
            description = "Retrieves a presigned URL for a company registration document. Requires ADMIN, CUSTOMER, or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document URL retrieved successfully",
                    content = @Content(schema = @Schema(example = "{\"url\": \"https://...\", \"originalName\": \"document.pdf\", \"contentType\": \"application/pdf\"}"))),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/document/{documentId}/url")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER', 'MANAGER')")
    public ResponseEntity<Map<String, String>> getDocumentUrl(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long documentId) {
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

    @Operation(
            summary = "Get all files for a registration request",
            description = "Retrieves presigned URLs for all images and documents of a registration request. Requires ADMIN, CUSTOMER, or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Request not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/request/{requestId}/files")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getRequestFiles(
            @Parameter(description = "Registration request ID", required = true)
            @PathVariable Long requestId) {
        var request = registrationRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Map<String, Object> response = new HashMap<>();

        var imageUrls = request.getImages().stream()
                .map(img -> Map.of(
                        "id", img.getId(),
                        "originalName", img.getOriginalName(),
                        "url", minioService.getPresignedUrl(img.getMinioBucket(), img.getMinioPath(), 60)
                ))
                .toList();

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

    @Operation(
            summary = "Get vehicle image URL",
            description = "Retrieves a presigned URL for a vehicle image. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image URL retrieved successfully",
                    content = @Content(schema = @Schema(example = "{\"url\": \"https://...\", \"originalName\": \"vehicle.jpg\"}"))),
            @ApiResponse(responseCode = "404", description = "Vehicle image not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/vehicle-image/{imageId}/url")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, String>> getVehicleImageUrl(
            @Parameter(description = "Image ID", required = true)
            @PathVariable Long imageId) {
        var vehicle = vehicleRepository.findByImageId(imageId)
                .orElseThrow(() -> new RuntimeException("Vehicle image not found"));

        VehicleImage image = vehicle.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image not found"));

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

    @Operation(
            summary = "Get all vehicle images",
            description = "Retrieves presigned URLs for all images of a vehicle. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle images retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/vehicle/{vehicleId}/images")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getVehicleImages(
            @Parameter(description = "Vehicle ID", required = true)
            @PathVariable Long vehicleId) {
        var vehicle = vehicleRepository.findByIdWithDetails(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        Map<String, Object> response = new HashMap<>();

        var imageUrls = vehicle.getImages().stream()
                .map(img -> Map.of(
                        "id", img.getId(),
                        "originalName", img.getOriginalName(),
                        "url", minioService.getPresignedUrl(img.getMinioBucket(), img.getMinioPath(), 60)
                ))
                .toList();

        response.put("images", imageUrls);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get warehouse image URL",
            description = "Retrieves a presigned URL for a warehouse image. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image URL retrieved successfully",
                    content = @Content(schema = @Schema(example = "{\"url\": \"https://...\", \"originalName\": \"warehouse.jpg\"}"))),
            @ApiResponse(responseCode = "404", description = "Warehouse image not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/warehouse-image/{imageId}/url")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, String>> getWarehouseImageUrl(
            @Parameter(description = "Image ID", required = true)
            @PathVariable Long imageId) {
        var warehouse = warehouseRepository.findByImageId(imageId)
                .orElseThrow(() -> new RuntimeException("Warehouse image not found"));

        WarehouseImage image = warehouse.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image not found"));

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

    @Operation(
            summary = "Get all warehouse images",
            description = "Retrieves presigned URLs for all images of a warehouse. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Warehouse images retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Warehouse not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/warehouse/{warehouseId}/images")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getWarehouseImages(
            @Parameter(description = "Warehouse ID", required = true)
            @PathVariable Long warehouseId) {
        var warehouse = warehouseRepository.findByIdWithDetails(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        Map<String, Object> response = new HashMap<>();

        var imageUrls = warehouse.getImages().stream()
                .map(img -> Map.of(
                        "id", img.getId(),
                        "originalName", img.getOriginalName(),
                        "url", minioService.getPresignedUrl(img.getMinioBucket(), img.getMinioPath(), 60)
                ))
                .toList();

        response.put("images", imageUrls);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get factory image URL",
            description = "Retrieves a presigned URL for a factory image. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image URL retrieved successfully",
                    content = @Content(schema = @Schema(example = "{\"url\": \"https://...\", \"originalName\": \"factory.jpg\"}"))),
            @ApiResponse(responseCode = "404", description = "Factory image not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/factory-image/{imageId}/url")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, String>> getFactoryImageUrl(
            @Parameter(description = "Image ID", required = true)
            @PathVariable Long imageId) {
        var factory = factoryRepository.findByImageId(imageId)
                .orElseThrow(() -> new RuntimeException("Factory image not found"));

        FactoryImage image = factory.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image not found"));

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

    @Operation(
            summary = "Get all factory images",
            description = "Retrieves presigned URLs for all images of a factory. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Factory images retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Factory not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/factory/{factoryId}/images")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getFactoryImages(
            @Parameter(description = "Factory ID", required = true)
            @PathVariable Long factoryId) {
        var factory = factoryRepository.findByIdWithDetails(factoryId)
                .orElseThrow(() -> new RuntimeException("Factory not found"));

        Map<String, Object> response = new HashMap<>();

        var imageUrls = factory.getImages().stream()
                .map(img -> Map.of(
                        "id", img.getId(),
                        "originalName", img.getOriginalName(),
                        "url", minioService.getPresignedUrl(img.getMinioBucket(), img.getMinioPath(), 60)
                ))
                .toList();

        response.put("images", imageUrls);

        return ResponseEntity.ok(response);
    }
}
