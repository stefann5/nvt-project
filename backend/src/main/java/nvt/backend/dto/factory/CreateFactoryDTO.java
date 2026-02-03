package nvt.backend.dto.factory;

import lombok.Data;

import java.util.List;

@Data
public class CreateFactoryDTO {
    private String name;
    private String description;
    private Long countryId;
    private Long cityId;
    private String street;
    private String streetNumber;
    private Double latitude;
    private Double longitude;
}
