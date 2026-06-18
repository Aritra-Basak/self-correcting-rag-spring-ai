package com.ai.selfCorrectingRag.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class UserController {

    @GetMapping("/api/user")
    public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
        // Returns the user's GitHub login name if authenticated, otherwise returns null (401 handled by config)
        return principal != null ? Collections.singletonMap("name", principal.getAttribute("login")) : null;
    }
}