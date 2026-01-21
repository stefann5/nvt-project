package nvt.backend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.order.Order;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListDTO implements Serializable {
    private Long id;
    private String orderNumber;
    private String companyName;
    private String status;
    private BigDecimal totalAmount;
    private int itemCount;
    private LocalDateTime createdAt;

    public static OrderListDTO fromEntity(Order order) {
        return OrderListDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .companyName(order.getCompany().getName())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems().size())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
