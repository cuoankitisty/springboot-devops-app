package com.demo.springbootdevopsapp.controller;

import com.demo.springbootdevopsapp.dto.request.CreateUserRequest;
import com.demo.springbootdevopsapp.dto.response.CreateUserResponse;
import com.demo.springbootdevopsapp.dto.response.UsersListResponse;
import com.demo.springbootdevopsapp.service.users.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping
    ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        Long id = userAccountService.createUser(request);
        URI location = URI.create("/api/v1/users/" + id);
        return ResponseEntity.created(location)
                .body(new CreateUserResponse(id, "User created successfully"));
    }

    @GetMapping
    ResponseEntity<UsersListResponse> getUsers() {
        UsersListResponse response = userAccountService.getAllUsers();
        return ResponseEntity.ok(response);
    }
}
