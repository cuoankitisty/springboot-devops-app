package com.demo.springbootdevopsapp.service.users;

import com.demo.springbootdevopsapp.data.domain.UserAccount;
import com.demo.springbootdevopsapp.dto.request.CreateUserRequest;
import com.demo.springbootdevopsapp.dto.response.UserSummaryResponse;
import com.demo.springbootdevopsapp.dto.response.UsersListResponse;
import com.demo.springbootdevopsapp.repo.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public UsersListResponse getAllUsers() {
        List<UserSummaryResponse> users = userAccountRepository.findAll()
                .stream()
                .map(u -> new UserSummaryResponse(u.getId(), u.getUsername()))
                .toList();
        return new UsersListResponse(users);
    }
}
