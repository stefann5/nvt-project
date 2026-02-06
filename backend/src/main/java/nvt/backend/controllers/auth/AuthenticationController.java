package nvt.backend.controllers.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nvt.backend.dto.user.auth.*;
import nvt.backend.exceptions.UserAuthenticationException;
import nvt.backend.services.auth.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
public class  AuthenticationController {

    private final AuthenticationService authService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and sends an activation email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid registration data")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User registration details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RegisterRequestDTO.class))
            )
            @RequestBody RegisterRequestDTO request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            summary = "User login",
            description = "Authenticates a user and returns access and refresh tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User login credentials",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequestDTO.class))
            )
            @RequestBody LoginRequestDTO request
    ) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @Operation(
            summary = "Activate user account",
            description = "Activates a user account using the activation token sent via email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account activated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired activation token")
    })
    @GetMapping("/activate")
    public ResponseEntity<String> activate(
            @Parameter(description = "Activation token from email", required = true)
            @RequestParam("token") String token) {
        try {
            String message = authService.activate(token);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Test endpoint",
            description = "Simple test endpoint to verify API connectivity"
    )
    @ApiResponse(responseCode = "200", description = "Test successful")
    @GetMapping("/test")
    public ResponseEntity<ResponseEntity<String>> test() {
        return ResponseEntity.ok(new ResponseEntity<String>("cao", HttpStatus.OK));
    }

    @Operation(
            summary = "Refresh access token",
            description = "Uses the refresh token to obtain a new access token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh_token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return authService.refreshToken(request, response);
    }

    @Operation(
            summary = "Change user password",
            description = "Allows a user to change their password by providing current and new password"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid current password or password requirements not met")
    })
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Password change details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChangePasswordDTO.class))
            )
            @RequestBody ChangePasswordDTO request) {
        try {
            authService.changePassword(request);
            return ResponseEntity.ok().body("{\"message\": \"Password changed successfully\"}");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @ExceptionHandler(UserAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(UserAuthenticationException e) {
        ErrorResponse error = new ErrorResponse(e.getMessage(), e.getErrorType());
        return ResponseEntity.status(401).body(error);
    }


    public class ErrorResponse {
        private String message;
        private String errorType;

        public ErrorResponse(String message, UserAuthenticationException.ErrorType errorType) {
            this.message = message;
            this.errorType = errorType.name();
        }

        // getters i setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
    }
}
