package com.platform.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.platform.backend.entity.User;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    /** Used for login/registration so accounts created before e-mails were lower-cased still work. */
    Optional<User> findByEmailIgnoreCase(String email);
}