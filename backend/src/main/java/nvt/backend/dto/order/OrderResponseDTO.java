package nvt.backend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.order.Order;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO implements Serializable {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private Long companyId;
    private String companyName;
    private String companyAddress;
    private String status;
    private BigDecimal totalAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private List<OrderItemResponseDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponseDTO implements Serializable {
        private Long id;
        private Long productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    public static OrderResponseDTO fromEntity(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId((long)order.getCustomer().getId())
                .customerName(order.getCustomer().getName() + " " + order.getCustomer().getSurname())
                .companyId(order.getCompany().getId())
                .companyName(order.getCompany().getName())
                .companyAddress(buildCompanyAddress(order))
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .processedAt(order.getProcessedAt())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponseDTO.builder()
                                .id(item.getId())
                                .productId(item.getProduct().getId())
                                .productName(item.getProduct().getName())
                                .productSku(item.getProduct().getSku())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .toList())
                .build();
    }

    private static String buildCompanyAddress(Order order) {
        StringBuilder sb = new StringBuilder();
        if (order.getCompany().getStreet() != null) {
            sb.append(order.getCompany().getStreet());
            if (order.getCompany().getStreetNumber() != null) {
                sb.append(" ").append(order.getCompany().getStreetNumber());
            }
        }
        if (order.getCompany().getCity() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(order.getCompany().getCity().getName());
        }
        if (order.getCompany().getCountry() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(order.getCompany().getCountry().getName());
        }
        return sb.toString();
    }
}
