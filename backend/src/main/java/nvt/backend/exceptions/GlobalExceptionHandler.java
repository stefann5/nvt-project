package nvt.backend.exceptions;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({
        OptimisticLockException.class,
        OptimisticLockingFailureException.class,
        ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<Map<String, Object>> handleOptimisticLockException(Exception ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse(
                "Conflict",
                "OPTIMISTIC_LOCK_ERROR",
                "The data was modified by another user. Please refresh and try again."
        ));
    }

    @ExceptionHandler({
        PessimisticLockException.class,
        PessimisticLockingFailureException.class,
        CannotAcquireLockException.class
    })
    public ResponseEntity<Map<String, Object>> handlePessimisticLockException(Exception ex) {
        log.warn("Pessimistic locking conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse(
                "Conflict",
                "PESSIMISTIC_LOCK_ERROR",
                "The resource is currently being modified by another operation. Please try again shortly."
        ));
    }

    @ExceptionHandler(ConcurrencyException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrencyException(ConcurrencyException ex) {
        log.warn("Concurrency exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse(
                "Conflict",
                "CONCURRENCY_ERROR",
                ex.getMessage()
        ));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStockException(InsufficientStockException ex) {
        log.warn("Insufficient stock: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(
                "Insufficient Stock",
                "INSUFFICIENT_STOCK",
                ex.getMessage()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        String message = "Data integrity error occurred.";
        if (ex.getMessage() != null && ex.getMessage().contains("unique constraint")) {
            message = "A record with this data already exists.";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse(
                "Data Integrity Error",
                "DATA_INTEGRITY_ERROR",
                message
        ));
    }

    @ExceptionHandler(CannotCreateTransactionException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionException(CannotCreateTransactionException ex) {
        log.error("Transaction creation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(createErrorResponse(
                "Service Unavailable",
                "TRANSACTION_ERROR",
                "Unable to process the request. Please try again later."
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                "Internal Server Error",
                "INTERNAL_ERROR",
                ex.getMessage()
        ));
    }

    private Map<String, Object> createErrorResponse(String error, String code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", error);
        response.put("code", code);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}
