package com.andrii.taskmanagement.repository.user;

import com.andrii.taskmanagement.model.Role;
import com.andrii.taskmanagement.model.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
