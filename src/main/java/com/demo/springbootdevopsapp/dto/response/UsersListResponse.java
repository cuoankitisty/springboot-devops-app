package com.demo.springbootdevopsapp.dto.response;

import java.util.List;

public record UsersListResponse(List<UserSummaryResponse> users) {}
