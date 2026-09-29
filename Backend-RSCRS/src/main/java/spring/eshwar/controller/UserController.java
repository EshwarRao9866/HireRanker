package spring.eshwar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.eshwar.dto.user.UserResponse;
import spring.eshwar.entity.User;
import spring.eshwar.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves the profile of the currently authenticated user (Candidate or Admin).
     * Strictly resolved from JWT principal. Never returns password.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    /**
     * Retrieves user information by ID.
     * Accessible by ADMIN, or by the user themselves for their own ID.
     * Prevents candidates from inspecting other users' administrative information.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            User currentUser = userService.getUserByEmail(authentication.getName());
            if (!currentUser.getId().equals(id)) {
                throw new AccessDeniedException("Candidates can only access their own profile");
            }
        }
        return ResponseEntity.ok(userService.getUserResponseById(id));
    }

    /**
     * Administrative endpoint to list all users.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUserResponses());
    }
}
