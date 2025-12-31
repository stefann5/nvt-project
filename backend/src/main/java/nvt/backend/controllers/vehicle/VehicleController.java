package nvt.backend.controllers.vehicle;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.vehicle.CreateVehicleDTO;
import nvt.backend.dto.vehicle.UpdateVehicleDTO;
import nvt.backend.dto.vehicle.VehicleResponseDTO;
import nvt.backend.model.vehicle.VehicleBrand;
import nvt.backend.model.vehicle.VehicleModel;
import nvt.backend.services.vehicle.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> create(
            @RequestPart("data") CreateVehicleDTO dto,
            @RequestPart("images") List<MultipartFile> images) {
        try {
            if (images == null || images.isEmpty()) {
                return ResponseEntity.badRequest().body("At least one image is required");
            }
            VehicleResponseDTO response = vehicleService.create(dto, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleResponseDTO>> getAll() {
        return ResponseEntity.ok(vehicleService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(vehicleService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleResponseDTO>> search(@RequestParam String query) {
        return ResponseEntity.ok(vehicleService.search(query));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestPart("data") UpdateVehicleDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages) {
        try {
            VehicleResponseDTO response = vehicleService.update(id, dto, newImages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            vehicleService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/brands")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleBrand>> getAllBrands() {
        return ResponseEntity.ok(vehicleService.getAllBrands());
    }

    @GetMapping("/brands/{brandId}/models")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleModel>> getModelsByBrand(@PathVariable Long brandId) {
        return ResponseEntity.ok(vehicleService.getModelsByBrand(brandId));
    }
}