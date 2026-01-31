package nvt.backend.dto.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.factory.Factory;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactorySimpleDTO implements Serializable {
    private Long id;
    private String name;

    public static FactorySimpleDTO fromEntity(Factory factory) {
        return FactorySimpleDTO.builder()
                .id(factory.getId())
                .name(factory.getName())
                .build();
    }
}
