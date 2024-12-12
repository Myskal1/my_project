package com.example.internship.controllers;

import com.example.internship.entities.User;
import com.example.internship.repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/secured")
public class UserController {
    @GetMapping("/user")
    public String userAccess(Principal principal){
        if (principal == null)
            return null;
        return principal.getName();
    }
}
