package nvt.backend.controllers.vehicle;

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
import nvt.backend.dto.vehicle.*;
import nvt.backend.model.vehicle.VehicleBrand;
import nvt.backend.model.vehicle.VehicleModel;
import nvt.backend.services.vehicle.VehicleService;
import nvt.backend.services.vehicle.VehicleTelemetryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicle", description = "Vehicle management and telemetry endpoints")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleTelemetryService telemetryService;

    @Operation(
            summary = "Create a new vehicle",
            description = "Creates a new vehicle with the provided data and images. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehicle created successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid vehicle data or missing images"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> create(
            @Parameter(description = "Vehicle data", required = true)
            @RequestPart("data") CreateVehicleDTO dto,
            @Parameter(description = "Vehicle images (at least one required)", required = true)
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

    @Operation(
            summary = "Get all vehicles (paginated)",
            description = "Retrieves vehicles with pagination and sorting. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicles retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<VehicleListDTO>> getAllPaged(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(vehicleService.getAllPaged(page, size, sortBy, sortDir));
    }

    @Operation(
            summary = "Get vehicle by ID",
            description = "Retrieves a specific vehicle by its ID. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle retrieved successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getById(
            @Parameter(description = "Vehicle ID", required = true)
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(vehicleService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Search vehicles (paginated)",
            description = "Searches vehicles by query string with pagination. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/search/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<VehicleListDTO>> searchPaged(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(vehicleService.searchPaged(query, page, size));
    }

    @Operation(
            summary = "Update a vehicle",
            description = "Updates an existing vehicle with new data and optional new images. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle updated successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid update data"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> update(
            @Parameter(description = "Vehicle ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated vehicle data", required = true)
            @RequestPart("data") UpdateVehicleDTO dto,
            @Parameter(description = "New vehicle images (optional)")
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages) {
        try {
            VehicleResponseDTO response = vehicleService.update(id, dto, newImages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Delete a vehicle",
            description = "Deletes a vehicle by ID. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vehicle deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> delete(
            @Parameter(description = "Vehicle ID", required = true)
            @PathVariable Long id) {
        try {
            vehicleService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get all vehicle brands",
            description = "Retrieves all available vehicle brands. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brands retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = VehicleBrand.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/brands")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleBrand>> getAllBrands() {
        return ResponseEntity.ok(vehicleService.getAllBrands());
    }

    @Operation(
            summary = "Get vehicle models by brand",
            description = "Retrieves all vehicle models for a specific brand. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Models retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = VehicleModel.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/brands/{brandId}/models")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleModel>> getModelsByBrand(
            @Parameter(description = "Brand ID", required = true)
            @PathVariable Long brandId) {
        return ResponseEntity.ok(vehicleService.getModelsByBrand(brandId));
    }

    @Operation(
            summary = "Get vehicle last known location",
            description = "Retrieves the last known GPS location of a vehicle. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Location retrieved successfully",
                    content = @Content(schema = @Schema(implementation = VehicleLocationDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found or no location data available"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}/location")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getLastLocation(
            @Parameter(description = "Vehicle ID", required = true)
            @PathVariable Long id) {
        try {
            VehicleLocationDTO location = vehicleService.getLastLocation(id);
            if (location == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No location data available for this vehicle");
            }
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get vehicle distance statistics",
            description = "Retrieves distance statistics for a vehicle within a date range. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Distance statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DistanceStatisticsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range or period"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}/distance/stats")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getDistanceStatistics(
            @Parameter(description = "Vehicle ID", required = true)
            @PathVariable Long id,
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

            DistanceStatisticsDTO stats = vehicleService.getDistanceStatistics(id, start, end);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get vehicle availability statistics",
            description = "Retrieves availability statistics for a vehicle within a time range. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AvailabilityStatisticsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid time range or period"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}/availability/stats")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getAvailabilityStatistics(
            @Parameter(description = "Vehicle ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Time period (1h, 3h, 12h, 24h, week, month, 3months, year)")
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
                    case "week" -> end.minus(7, ChronoUnit.DAYS);
                    case "month" -> end.minus(30, ChronoUnit.DAYS);
                    case "3months" -> end.minus(90, ChronoUnit.DAYS);
                    case "year" -> end.minus(365, ChronoUnit.DAYS);
                    default -> throw new IllegalArgumentException("Invalid period. Use: 1h, 3h, 12h, 24h, week, month, 3months, year");
                };
            } else if (startTime != null && endTime != null) {
                if (endTime.isBefore(startTime)) {
                    return ResponseEntity.badRequest().body("End time must be after start time");
                }
                long daysBetween = ChronoUnit.DAYS.between(startTime, endTime);
                if (daysBetween > 365) {
                    return ResponseEntity.badRequest().body("Date range cannot exceed one year");
                }
                start = startTime;
                end = endTime;
            } else {
                start = end.minus(24, ChronoUnit.HOURS);
            }

            AvailabilityStatisticsDTO stats = vehicleService.getAvailabilityStatistics(id, start, end);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
