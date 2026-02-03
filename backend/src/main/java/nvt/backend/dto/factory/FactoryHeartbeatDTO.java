package nvt.backend.dto.factory;

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
}
