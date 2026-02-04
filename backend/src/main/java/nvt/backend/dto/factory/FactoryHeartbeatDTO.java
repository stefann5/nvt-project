package nvt.backend.dto.factory;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryHeartbeatDTO {
    private Long factoryId;
    private String factoryName;
    private String timestamp;
    private String status;

    @JsonSetter("factoryId")
    public void setFactoryId(Object factoryId) {
        if (factoryId instanceof Number) {
            this.factoryId = ((Number) factoryId).longValue();
        } else if (factoryId instanceof String) {
            this.factoryId = Long.parseLong((String) factoryId);
        }
    }
}
