package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);
}
