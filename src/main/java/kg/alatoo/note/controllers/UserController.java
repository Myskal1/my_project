package kg.alatoo.note.controllers;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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
