package spring.eshwar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.eshwar.dto.dashboard.AdminDashboardResponse;
import spring.eshwar.dto.user.UserResponse;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.User;
import spring.eshwar.service.AdminDashboardService;
import spring.eshwar.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AdminDashboardService adminDashboardService;

    public AdminController(UserService userService,
                           AdminDashboardService adminDashboardService) {
        this.userService = userService;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboardStats(
            @RequestParam(name = "period", required = false, defaultValue = "This Week") String period) {
        return ResponseEntity.ok(adminDashboardService.getDashboardData(period));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUserResponses());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long id,
                                                       @RequestParam Role role) {
        User user = userService.getUserById(id);
        user.setRole(role);
        User updated = userService.updateUser(id, user);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
