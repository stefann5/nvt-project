package nvt.backend.controllers.location;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.repositories.common.CityRepository;
import nvt.backend.repositories.common.CountryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Location", description = "Location data endpoints for countries and cities")
public class LocationController {

    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    @Operation(
            summary = "Get all countries",
            description = "Retrieves a list of all available countries"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of countries retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Country.class))))
    })
    @GetMapping("/countries")
    public List<Country> getCountries() {
        return countryRepository.findAll();
    }

    @Operation(
            summary = "Get cities by country",
            description = "Retrieves all cities belonging to a specific country"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of cities retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = City.class))))
    })
    @GetMapping("/countries/{id}/cities")
    public List<City> getCities(
            @Parameter(description = "Country ID", required = true)
            @PathVariable Long id) {
        return cityRepository.findByCountryId(id);
    }
}
