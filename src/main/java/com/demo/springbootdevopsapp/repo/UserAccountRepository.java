package com.demo.springbootdevopsapp.repo;

import com.demo.springbootdevopsapp.data.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByUsername(String username);
}
