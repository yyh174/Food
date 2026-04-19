package com.xl.can.controller;

import com.xl.can.common.Result;
import com.xl.can.context.UserContext;
import com.xl.can.dto.*;
import com.xl.can.entity.SysUser;
import com.xl.can.service.AuthService;
import com.xl.can.service.UserService;
import com.xl.can.vo.UserDetailVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/shop-manager")
    public Result<LoginResponse> shopManagerRegister(@Valid @RequestBody ShopManagerRegisterRequest request) {
        return authService.shopManagerRegister(request);
    }

    @PutMapping("/password/reset/{userId}")
    public Result<Void> resetPassword(@PathVariable Long userId) {
        return authService.resetPassword(userId);
    }

    @PutMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.getUserId();
        return authService.changePassword(userId, request);
    }

    @GetMapping("/current-user")
    public Result<UserDetailVO> getCurrentUser() {
        Long userId = UserContext.getUserId();
        return userService.getById(userId);
    }
}
