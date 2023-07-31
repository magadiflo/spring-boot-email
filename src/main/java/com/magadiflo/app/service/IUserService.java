package com.magadiflo.app.service;

import com.magadiflo.app.domain.User;

public interface IUserService {
    User saveUser(User user);
    Boolean verifyToken(String token);
}
