package nvt.backend.controllers.warehouse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Warehouse", description = "Warehouse management and telemetry endpoints")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(
            summary = "Create a new warehouse",
            description = "Creates a new warehouse with the provided data and images. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Warehouse created successfully",
                    content = @Content(schema = @Schema(implementation = WarehouseResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid warehouse data or missing images"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> create(
            @Parameter(description = "Warehouse data", required = true)
            @RequestPart("data") CreateWarehouseDTO dto,
            @Parameter(description = "Warehouse images (at least one required)", required = true)
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

    @Operation(
            summary = "Get all warehouses",
            description = "Retrieves all warehouses. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Warehouses retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WarehouseListDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WarehouseListDTO>> getAll() {
        return ResponseEntity.ok(warehouseService.getAll());
    }

    @Operation(
            summary = "Get all warehouses (paginated)",
            description = "Retrieves warehouses with pagination and sorting. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Warehouses retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<WarehouseListDTO>> getAllPaged(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(warehouseService.getAllPaged(page, size, sortBy, sortDir));
    }

    @Operation(
            summary = "Get warehouse by ID",
            description = "Retrieves a specific warehouse by its ID. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Warehouse retrieved successfully",
                    content = @Content(schema = @Schema(implementation = WarehouseResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Warehouse not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getById(
            @Parameter(description = "Warehouse ID", required = true)
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(warehouseService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Search warehouses",
            description = "Searches warehouses by query string. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WarehouseListDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WarehouseListDTO>> search(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query) {
        return ResponseEntity.ok(warehouseService.search(query));
    }

    @Operation(
            summary = "Search warehouses (paginated)",
            description = "Searches warehouses by query string with pagination. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated search results retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/search/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<WarehouseListDTO>> searchPaged(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(warehouseService.searchPaged(query, page, size));
    }

    @Operation(
            summary = "Update a warehouse",
            description = "Updates an existing warehouse with new data and optional new images. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Warehouse updated successfully",
                    content = @Content(schema = @Schema(implementation = WarehouseResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid update data"),
            @ApiResponse(responseCode = "404", description = "Warehouse not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> update(
            @Parameter(description = "Warehouse ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated warehouse data", required = true)
            @RequestPart("data") UpdateWarehouseDTO dto,
            @Parameter(description = "New warehouse images (optional)")
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages) {
        try {
            WarehouseResponseDTO response = warehouseService.update(id, dto, newImages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Delete a warehouse",
            description = "Deletes a warehouse by ID. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Warehouse deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Warehouse not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> delete(
            @Parameter(description = "Warehouse ID", required = true)
            @PathVariable Long id) {
        try {
            warehouseService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get all countries",
            description = "Retrieves all countries for warehouse location selection. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Countries retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Country.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/countries")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Country>> getAllCountries() {
        return ResponseEntity.ok(warehouseService.getAllCountries());
    }

    @Operation(
            summary = "Get cities by country",
            description = "Retrieves all cities for a specific country. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cities retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = City.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/countries/{countryId}/cities")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<City>> getCitiesByCountry(
            @Parameter(description = "Country ID", required = true)
            @PathVariable Long countryId) {
        return ResponseEntity.ok(warehouseService.getCitiesByCountry(countryId));
    }

    @Operation(
            summary = "Get sector with current temperature",
            description = "Retrieves a warehouse sector with its current temperature reading. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sector retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Warehouse or sector not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{warehouseId}/sectors/{sectorId}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getSectorWithCurrentTemperature(
            @Parameter(description = "Warehouse ID", required = true)
            @PathVariable Long warehouseId,
            @Parameter(description = "Sector ID", required = true)
            @PathVariable Long sectorId) {
        try {
            return ResponseEntity.ok(warehouseService.getSectorWithCurrentTemperature(warehouseId, sectorId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get sector temperature statistics",
            description = "Retrieves temperature statistics for a warehouse sector within a date range. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Temperature statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TemperatureStatisticsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range or period"),
            @ApiResponse(responseCode = "404", description = "Warehouse or sector not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{warehouseId}/sectors/{sectorId}/temperature/stats")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getTemperatureStatistics(
            @Parameter(description = "Warehouse ID", required = true)
            @PathVariable Long warehouseId,
            @Parameter(description = "Sector ID", required = true)
            @PathVariable Long sectorId,
            @Parameter(description = "Time period (week, month, 3months, 6months, year)")
            @RequestParam(required = false) String period,
            @Parameter(description = "Start date (ISO format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (ISO format)")
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

    @Operation(
            summary = "Get warehouse availability statistics",
            description = "Retrieves availability statistics for a warehouse within a time range. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = WarehouseAvailabilityStatisticsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid time range or period"),
            @ApiResponse(responseCode = "404", description = "Warehouse not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{warehouseId}/availability/stats")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getAvailabilityStatistics(
            @Parameter(description = "Warehouse ID", required = true)
            @PathVariable Long warehouseId,
            @Parameter(description = "Time period (1h, 3h, 12h, 24h, 7d, 30d, 3months, year)")
            @RequestParam(required = false) String period,
            @Parameter(description = "Start time (ISO format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time (ISO format)")
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
