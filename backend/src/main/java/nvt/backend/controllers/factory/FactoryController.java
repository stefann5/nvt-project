package nvt.backend.controllers.factory;

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
import nvt.backend.dto.factory.*;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.services.factory.FactoryService;
import nvt.backend.services.factory.FactoryTelemetryService;
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
@RequestMapping("/api/v1/factories")
@RequiredArgsConstructor
@Tag(name = "Factory", description = "Factory management endpoints")
public class FactoryController {

    private final FactoryService factoryService;
    private final FactoryTelemetryService telemetryService;

    @Operation(
            summary = "Create a new factory",
            description = "Creates a new factory with the provided data and images. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Factory created successfully",
                    content = @Content(schema = @Schema(implementation = FactoryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid factory data or missing images"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> create(
            @Parameter(description = "Factory data", required = true)
            @RequestPart("data") CreateFactoryDTO dto,
            @Parameter(description = "Factory images (at least one required)", required = true)
            @RequestPart("images") List<MultipartFile> images) {
        try {
            if (images == null || images.isEmpty()) {
                return ResponseEntity.badRequest().body("At least one image is required");
            }
            FactoryResponseDTO response = factoryService.create(dto, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get all factories",
            description = "Retrieves all factories. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of factories retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FactoryListDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<FactoryListDTO>> getAll() {
        return ResponseEntity.ok(factoryService.getAll());
    }

    @Operation(
            summary = "Get all factories (simplified)",
            description = "Retrieves all factories with minimal data. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of factories retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FactorySimpleDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/simple")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<FactorySimpleDTO>> getAllSimple() {
        return ResponseEntity.ok(factoryService.getAllSimple());
    }

    @Operation(
            summary = "Get all factories with pagination",
            description = "Retrieves factories with pagination and sorting. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of factories retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<FactoryListDTO>> getAllPaged(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(factoryService.getAllPaged(page, size, sortBy, sortDir));
    }

    @Operation(
            summary = "Get factory by ID",
            description = "Retrieves a specific factory by its ID. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Factory retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FactoryResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Factory not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getById(
            @Parameter(description = "Factory ID", required = true)
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(factoryService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Search factories",
            description = "Searches factories by query string. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FactoryListDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<FactoryListDTO>> search(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query) {
        return ResponseEntity.ok(factoryService.search(query));
    }

    @Operation(
            summary = "Search factories with pagination",
            description = "Searches factories by query string with pagination. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated search results retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/search/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<FactoryListDTO>> searchPaged(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(factoryService.searchPaged(query, page, size));
    }

    @Operation(
            summary = "Filter factories",
            description = "Filters factories by various criteria with pagination. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filtered results retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<FactoryListDTO>> filterFactories(
            @Parameter(description = "Factory name filter")
            @RequestParam(required = false) String name,
            @Parameter(description = "Country ID filter")
            @RequestParam(required = false) Long countryId,
            @Parameter(description = "City ID filter")
            @RequestParam(required = false) Long cityId,
            @Parameter(description = "Online status filter")
            @RequestParam(required = false) Boolean online,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(factoryService.filterFactories(name, countryId, cityId, online, page, size));
    }

    @Operation(
            summary = "Update a factory",
            description = "Updates an existing factory with new data and optional new images. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Factory updated successfully",
                    content = @Content(schema = @Schema(implementation = FactoryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid update data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Factory not found")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> update(
            @Parameter(description = "Factory ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated factory data", required = true)
            @RequestPart("data") UpdateFactoryDTO dto,
            @Parameter(description = "New factory images (optional)")
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages) {
        try {
            FactoryResponseDTO response = factoryService.update(id, dto, newImages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Delete a factory",
            description = "Deletes a factory by ID. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Factory deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Factory not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> delete(
            @Parameter(description = "Factory ID", required = true)
            @PathVariable Long id) {
        try {
            factoryService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get all countries",
            description = "Retrieves all countries for factory location selection. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of countries retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Country.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/countries")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Country>> getAllCountries() {
        return ResponseEntity.ok(factoryService.getAllCountries());
    }

    @Operation(
            summary = "Get cities by country",
            description = "Retrieves all cities for a specific country. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of cities retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = City.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/countries/{countryId}/cities")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<City>> getCitiesByCountry(
            @Parameter(description = "Country ID", required = true)
            @PathVariable Long countryId) {
        return ResponseEntity.ok(factoryService.getCitiesByCountry(countryId));
    }

    @Operation(
            summary = "Get production statistics",
            description = "Retrieves production statistics for a specific product in a factory within a date range. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Production statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FactoryProductionStatisticsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range or period"),
            @ApiResponse(responseCode = "404", description = "Factory or product not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{factoryId}/products/{productId}/production/stats")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getProductionStatistics(
            @Parameter(description = "Factory ID", required = true)
            @PathVariable Long factoryId,
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId,
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

            FactoryProductionStatisticsDTO stats = factoryService.getProductionStatistics(factoryId, productId, start, end);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get factory availability statistics",
            description = "Retrieves availability statistics for a factory within a date range. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FactoryAvailabilityStatisticsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range or period"),
            @ApiResponse(responseCode = "404", description = "Factory not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{factoryId}/availability/stats")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getAvailabilityStatistics(
            @Parameter(description = "Factory ID", required = true)
            @PathVariable Long factoryId,
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

            FactoryAvailabilityStatisticsDTO stats = factoryService.getAvailabilityStatistics(factoryId, start, end);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
