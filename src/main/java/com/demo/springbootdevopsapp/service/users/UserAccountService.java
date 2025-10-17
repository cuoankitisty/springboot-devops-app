package com.demo.springbootdevopsapp.service.users;

import com.demo.springbootdevopsapp.data.domain.UserAccount;
import com.demo.springbootdevopsapp.dto.request.CreateUserRequest;
import com.demo.springbootdevopsapp.repo.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    @Transactional
    public Long createUser(CreateUserRequest createUserRequest) {
        UserAccount user = UserAccount.builder().username(createUserRequest.username()).password(createUserRequest.password()).build();
        UserAccount saved = userAccountRepository.save(user);
        return saved.getId();
    }
}
