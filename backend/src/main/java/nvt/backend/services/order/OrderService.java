package nvt.backend.services.order;

import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.order.CreateOrderDTO;
import nvt.backend.dto.order.OrderListDTO;
import nvt.backend.dto.order.OrderResponseDTO;
import nvt.backend.exceptions.ConcurrencyException;
import nvt.backend.exceptions.InsufficientStockException;
import nvt.backend.model.company.RegistrationRequest;
import nvt.backend.model.order.Order;
import nvt.backend.model.order.OrderItem;
import nvt.backend.model.product.Product;
import nvt.backend.model.user.User;
import nvt.backend.model.warehouse.Inventory;
import nvt.backend.repositories.company.RegistrationRequestRepository;
import nvt.backend.repositories.order.OrderRepository;
import nvt.backend.repositories.product.ProductRepository;
import nvt.backend.repositories.user.UserRepository;
import nvt.backend.repositories.warehouse.InventoryRepository;
import nvt.backend.services.storage.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final RegistrationRequestRepository companyRepository;
    private final UserRepository userRepository;
    private final InvoiceService invoiceService;
    private final JavaMailSender mailSender;
    private final MinioService minioService;

    @Value("${minio.bucket.invoices:invoices}")
    private String invoicesBucket;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "ordersPage", allEntries = true),
        @CacheEvict(value = "customerOrders", allEntries = true)
    })
    @Retryable(
        retryFor = {OptimisticLockException.class, OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public OrderResponseDTO createOrder(CreateOrderDTO dto, Integer customerId) {
        log.debug("Creating order for customer {}", customerId);

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        RegistrationRequest company = companyRepository.findByIdWithDetails(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        if (company.getStatus() != RegistrationRequest.Status.APPROVED) {
            throw new RuntimeException("Company is not approved");
        }

        if (!(company.getOwner().getId() == customerId)) {
            throw new RuntimeException("Company does not belong to this customer");
        }

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("Order must have at least one item");
        }

        for (CreateOrderDTO.OrderItemDTO itemDto : dto.getItems()) {
            Integer availableQuantity = inventoryRepository.getTotalAvailableQuantityByProductId(itemDto.getProductId());
            if (availableQuantity == null || availableQuantity < itemDto.getQuantity()) {
                Product product = productRepository.findById(itemDto.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + itemDto.getProductId()));
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName() +
                        ". Available: " + (availableQuantity != null ? availableQuantity : 0) + ", Requested: " + itemDto.getQuantity());
            }
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customer);
        order.setCompany(company);
        order.setNotes(dto.getNotes());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderDTO.OrderItemDTO itemDto : dto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemDto.getProductId()));

            try {
                decreaseInventory(product.getId(), itemDto.getQuantity());
            } catch (OptimisticLockException | OptimisticLockingFailureException e) {
                log.warn("Concurrency conflict while decreasing inventory for product {}, retrying...", product.getId());
                throw e;
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));

            order.addItem(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }

        order.setTotalAmount(totalAmount);
        order.setProcessedAt(LocalDateTime.now());

        order = orderRepository.save(order);
        log.info("Order {} created successfully", order.getOrderNumber());

        try {
            String invoicePath = invoiceService.generateAndUploadInvoice(order);
            order.setInvoicePath(invoicePath);
            order = orderRepository.save(order);

            sendOrderConfirmationEmail(order, customer.getUsername());
        } catch (Exception e) {
            log.error("Failed to generate invoice or send email for order {}: {}", order.getOrderNumber(), e.getMessage());
        }

        return OrderResponseDTO.fromEntity(order);
    }

    private void decreaseInventory(Long productId, Integer quantity) {
        List<Inventory> inventories = inventoryRepository.findAvailableInventoryForProduct(productId);

        if (inventories.isEmpty()) {
            throw new InsufficientStockException("No inventory available for product ID: " + productId);
        }

        int remainingToDecrease = quantity;

        for (Inventory inventory : inventories) {
            if (remainingToDecrease <= 0) break;

            int available = inventory.getAvailableQuantity();
            int toDecrease = Math.min(available, remainingToDecrease);

            inventory.setQuantity(inventory.getQuantity() - toDecrease);
            inventoryRepository.save(inventory);

            remainingToDecrease -= toDecrease;
        }

        if (remainingToDecrease > 0) {
            throw new InsufficientStockException("Failed to decrease inventory - insufficient stock. Remaining: " + remainingToDecrease);
        }
    }

    private String generateOrderNumber() {
        String prefix = "ORD";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + "-" + timestamp + "-" + random;
    }

    private void sendOrderConfirmationEmail(Order order, String customerEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@smartly.com");
            helper.setTo(customerEmail);
            helper.setSubject("Potvrda porudzbine - " + order.getOrderNumber());

            String htmlContent = buildOrderConfirmationHtml(order);
            helper.setText(htmlContent, true);

            if (order.getInvoicePath() != null) {
                byte[] invoicePdf = minioService.downloadFile(invoicesBucket, order.getInvoicePath());
                helper.addAttachment("Faktura-" + order.getOrderNumber() + ".pdf",
                        () -> new java.io.ByteArrayInputStream(invoicePdf),
                        "application/pdf");
            }

            mailSender.send(message);
            log.info("Order confirmation email sent to {} for order {}", customerEmail, order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email: {}", e.getMessage());
        }
    }

    private String buildOrderConfirmationHtml(Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            itemsHtml.append("<tr>")
                    .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(item.getProduct().getName()).append("</td>")
                    .append("<td style='padding: 8px; border-bottom: 1px solid #ddd; text-align: center;'>").append(item.getQuantity()).append("</td>")
                    .append("<td style='padding: 8px; border-bottom: 1px solid #ddd; text-align: right;'>").append(String.format("%.2f EUR", item.getUnitPrice())).append("</td>")
                    .append("<td style='padding: 8px; border-bottom: 1px solid #ddd; text-align: right;'>").append(String.format("%.2f EUR", item.getTotalPrice())).append("</td>")
                    .append("</tr>");
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px;">
                        Potvrda porudzbine
                    </h1>

                    <p>Postovani/a %s %s,</p>

                    <p>Vasa porudzbina je uspesno kreirana!</p>

                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p><strong>Broj porudzbine:</strong> %s</p>
                        <p><strong>Datum:</strong> %s</p>
                        <p><strong>Firma:</strong> %s</p>
                    </div>

                    <h3 style="color: #2c3e50;">Stavke porudzbine:</h3>
                    <table style="width: 100%%; border-collapse: collapse;">
                        <thead>
                            <tr style="background-color: #3498db; color: white;">
                                <th style="padding: 10px; text-align: left;">Proizvod</th>
                                <th style="padding: 10px; text-align: center;">Kolicina</th>
                                <th style="padding: 10px; text-align: right;">Jed. cena</th>
                                <th style="padding: 10px; text-align: right;">Ukupno</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>

                    <div style="text-align: right; margin-top: 20px; padding: 15px; background-color: #e8f4f8; border-radius: 5px;">
                        <h2 style="color: #2c3e50; margin: 0;">Ukupno: %s EUR</h2>
                    </div>

                    <p style="margin-top: 20px;">Faktura je prilozen uz ovaj email.</p>

                    <p>Hvala na poverenju!</p>

                    <hr style="margin-top: 30px; border: none; border-top: 1px solid #ddd;">
                    <p style="color: #666; font-size: 12px;">
                        Ovo je automatski generisana poruka. Molimo ne odgovarajte na ovaj email.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(
                order.getCustomer().getName(),
                order.getCustomer().getSurname(),
                order.getOrderNumber(),
                order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                order.getCompany().getName(),
                itemsHtml.toString(),
                String.format("%.2f", order.getTotalAmount())
        );
    }

    @Cacheable(value = "orderById", key = "#id")
    public OrderResponseDTO getById(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return OrderResponseDTO.fromEntity(order);
    }

    public OrderResponseDTO getByIdForCustomer(Long id, Integer customerId) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!(order.getCustomer().getId() == customerId)) {
            throw new RuntimeException("Access denied");
        }

        return OrderResponseDTO.fromEntity(order);
    }

    @Cacheable(value = "customerOrders", key = "#customerId + '-' + #page + '-' + #size")
    public PageResponseDTO<OrderListDTO> getOrdersByCustomer(Integer customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> idPage = orderRepository.findIdsByCustomerId(customerId, pageable);

        if (idPage.getContent().isEmpty()) {
            return PageResponseDTO.<OrderListDTO>builder()
                    .content(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        List<Order> orders = orderRepository.findAllByIds(idPage.getContent());

        return PageResponseDTO.<OrderListDTO>builder()
                .content(orders.stream().map(OrderListDTO::fromEntity).toList())
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    @Cacheable(value = "ordersPage", key = "#page + '-' + #size")
    public PageResponseDTO<OrderListDTO> getAllOrdersPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> idPage = orderRepository.findAllIds(pageable);

        if (idPage.getContent().isEmpty()) {
            return PageResponseDTO.<OrderListDTO>builder()
                    .content(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        List<Order> orders = orderRepository.findAllByIds(idPage.getContent());

        return PageResponseDTO.<OrderListDTO>builder()
                .content(orders.stream().map(OrderListDTO::fromEntity).toList())
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    public byte[] getInvoicePdf(Long orderId, Integer customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!(order.getCustomer().getId() == customerId)) {
            throw new RuntimeException("Access denied");
        }

        if (order.getInvoicePath() == null) {
            throw new RuntimeException("Invoice not available");
        }

        return invoiceService.getInvoicePdf(order.getInvoicePath());
    }

    public byte[] getInvoicePdfForManager(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getInvoicePath() == null) {
            throw new RuntimeException("Invoice not available");
        }

        return invoiceService.getInvoicePdf(order.getInvoicePath());
    }
}
