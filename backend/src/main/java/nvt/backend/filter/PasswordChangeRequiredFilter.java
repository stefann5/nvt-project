package nvt.backend.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nvt.backend.model.user.Manager;
import nvt.backend.model.user.User;
import nvt.backend.repositories.user.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public PasswordChangeRequiredFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Allow password change and logout endpoints without restriction
        if (isAllowedEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if user is authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() 
                || "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get the username from the authentication
        String username = authentication.getName();
        
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // Only check for Manager users
            if (user instanceof Manager) {
                Manager manager = (Manager) user;
                
                if (manager.isMustChangePassword()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "PASSWORD_CHANGE_REQUIRED");
                    errorResponse.put("message", "You must change your password before accessing the application");
                    errorResponse.put("changePasswordUrl", "/api/v1/auth/change-password");
                    
                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    response.getWriter().flush();
                    return;
                }

                // Check if manager is blocked
                if (manager.isBlocked()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "ACCOUNT_BLOCKED");
                    errorResponse.put("message", "Your account has been blocked. Please contact the administrator.");
                    
                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    response.getWriter().flush();
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedEndpoint(String uri) {
        return uri.contains("/api/v1/auth/change-password") 
                || uri.contains("/api/v1/auth/logout")
                || uri.contains("/api/v1/auth/login")
                || uri.contains("/api/v1/auth/register")
                || uri.contains("/api/v1/auth/activate")
                || uri.contains("/api/v1/auth/refresh_token");
    }
}
