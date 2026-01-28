package nvt.backend.services.auth;

import nvt.backend.config.UserFactory;
import nvt.backend.repositories.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserDetailsServiceImp implements UserDetailsService {

    private final UserRepository repository;
    
    // In-memory cache for user details
    private final Map<String, UserDetails> userDetailsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> userDetailsCacheTimestamp = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 300000; // 5 minutes cache

    public UserDetailsServiceImp(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check cache first
        UserDetails cached = getCachedUserDetails(username);
        if (cached != null) {
            return cached;
        }
        
        UserDetails userDetails = UserFactory.create(repository.findByUsername(username)
                .orElseThrow(()-> new UsernameNotFoundException("User not found")));
        
        cacheUserDetails(username, userDetails);
        return userDetails;
    }
    
    private UserDetails getCachedUserDetails(String username) {
        Long timestamp = userDetailsCacheTimestamp.get(username);
        if (timestamp == null || System.currentTimeMillis() - timestamp > CACHE_TTL_MS) {
            userDetailsCache.remove(username);
            userDetailsCacheTimestamp.remove(username);
            return null;
        }
        return userDetailsCache.get(username);
    }
    
    private void cacheUserDetails(String username, UserDetails userDetails) {
        userDetailsCache.put(username, userDetails);
        userDetailsCacheTimestamp.put(username, System.currentTimeMillis());
    }
}
