package nvt.backend.dto.warehouse;

import lombok.Data;

import java.util.List;

@Data
public class CreateWarehouseDTO {
    private String name;
    private Long countryId;
    private Long cityId;
    private String street;
    private String streetNumber;
    private Double latitude;
    private Double longitude;
    private Double totalCapacity;
    private List<CreateWarehouseSectorDTO> sectors;
}
