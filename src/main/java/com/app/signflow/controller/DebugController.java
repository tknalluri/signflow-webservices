package com.app.signflow.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/debug")
public class DebugController {

    @GetMapping("/auth")
    public String checkAuth() {
        String auth = SecurityContextHolder.getContext().getAuthentication().getName();
        return "Authenticated as: " + auth;
    }
}
