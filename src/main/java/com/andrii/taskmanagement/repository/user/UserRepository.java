package com.andrii.taskmanagement.repository.user;

import com.andrii.taskmanagement.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("""
            SELECT u
            FROM User u
            LEFT JOIN FETCH u.role
            WHERE u.email = :email
            """)
    Optional<User> findByEmail(String email);
}
