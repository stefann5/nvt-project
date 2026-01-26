package nvt.backend.controllers.warehouse;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.warehouse.*;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.services.warehouse.WarehouseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> create(
            @RequestPart("data") CreateWarehouseDTO dto,
            @RequestPart("images") List<MultipartFile> images) {
        try {
            if (images == null || images.isEmpty()) {
                return ResponseEntity.badRequest().body("At least one image is required");
            }
            WarehouseResponseDTO response = warehouseService.create(dto, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WarehouseListDTO>> getAll() {
        return ResponseEntity.ok(warehouseService.getAll());
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<WarehouseListDTO>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(warehouseService.getAllPaged(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(warehouseService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WarehouseListDTO>> search(@RequestParam String query) {
        return ResponseEntity.ok(warehouseService.search(query));
    }

    @GetMapping("/search/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<WarehouseListDTO>> searchPaged(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(warehouseService.searchPaged(query, page, size));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestPart("data") UpdateWarehouseDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages) {
        try {
            WarehouseResponseDTO response = warehouseService.update(id, dto, newImages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            warehouseService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/countries")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Country>> getAllCountries() {
        return ResponseEntity.ok(warehouseService.getAllCountries());
    }

    @GetMapping("/countries/{countryId}/cities")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<City>> getCitiesByCountry(@PathVariable Long countryId) {
        return ResponseEntity.ok(warehouseService.getCitiesByCountry(countryId));
    }

    @GetMapping("/{warehouseId}/sectors/{sectorId}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getSectorWithCurrentTemperature(
            @PathVariable Long warehouseId,
            @PathVariable Long sectorId) {
        try {
            return ResponseEntity.ok(warehouseService.getSectorWithCurrentTemperature(warehouseId, sectorId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{warehouseId}/sectors/{sectorId}/temperature/stats")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getTemperatureStatistics(
            @PathVariable Long warehouseId,
            @PathVariable Long sectorId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LocalDate start;
            LocalDate end = LocalDate.now();

            if (period != null) {
                start = switch (period.toLowerCase()) {
                    case "week" -> end.minusWeeks(1);
                    case "month" -> end.minusMonths(1);
                    case "3months" -> end.minusMonths(3);
                    case "6months" -> end.minusMonths(6);
                    case "year" -> end.minusYears(1);
                    default -> throw new IllegalArgumentException("Invalid period. Use: week, month, 3months, 6months, year");
                };
            } else if (startDate != null && endDate != null) {
                if (endDate.isBefore(startDate)) {
                    return ResponseEntity.badRequest().body("End date must be after start date");
                }
                long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
                if (daysBetween > 365) {
                    return ResponseEntity.badRequest().body("Date range cannot exceed one year");
                }
                start = startDate;
                end = endDate;
            } else {
                start = end.minusWeeks(1);
            }

            TemperatureStatisticsDTO stats = warehouseService.getTemperatureStatistics(warehouseId, sectorId, start, end);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{warehouseId}/availability/stats")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getAvailabilityStatistics(
            @PathVariable Long warehouseId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        try {
            Instant start;
            Instant end = Instant.now();

            if (period != null) {
                start = switch (period.toLowerCase()) {
                    case "1h" -> end.minus(1, ChronoUnit.HOURS);
                    case "3h" -> end.minus(3, ChronoUnit.HOURS);
                    case "12h" -> end.minus(12, ChronoUnit.HOURS);
                    case "24h" -> end.minus(24, ChronoUnit.HOURS);
                    case "7d", "week" -> end.minus(7, ChronoUnit.DAYS);
                    case "30d", "month" -> end.minus(30, ChronoUnit.DAYS);
                    case "3months" -> end.minus(90, ChronoUnit.DAYS);
                    case "year" -> end.minus(365, ChronoUnit.DAYS);
                    default -> throw new IllegalArgumentException("Invalid period. Use: 1h, 3h, 12h, 24h, 7d, 30d, 3months, year");
                };
            } else if (startTime != null && endTime != null) {
                if (endTime.isBefore(startTime)) {
                    return ResponseEntity.badRequest().body("End time must be after start time");
                }
                long daysBetween = Duration.between(startTime, endTime).toDays();
                if (daysBetween > 365) {
                    return ResponseEntity.badRequest().body("Date range cannot exceed one year");
                }
                start = startTime;
                end = endTime;
            } else {
                start = end.minus(24, ChronoUnit.HOURS);
            }

            WarehouseAvailabilityStatisticsDTO stats = warehouseService.getAvailabilityStatistics(warehouseId, start, end);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
