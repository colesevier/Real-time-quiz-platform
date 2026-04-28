package com.quiz.controller;

import com.quiz.dao.UserDAO;
import com.quiz.model.User;
import com.quiz.util.PasswordUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private final UserDAO userDAO;

    public AuthController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        boolean ok = userDAO.validateCredentials(username, password);
        if (ok) {
            Optional<User> u = userDAO.findByUsername(username);
            session.setAttribute("username", username);
            session.setAttribute("userID", u.map(User::getUserID).orElse(null));
            session.setMaxInactiveInterval(30 * 60);
            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", "Invalid username or password.");
            return "login";
        }
    }

    @GetMapping("/register")
    public String register() { return "register"; }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username, @RequestParam String email, @RequestParam String password, @RequestParam String confirm, Model model) {
        // Always repopulate so the form isn't blank on error
        model.addAttribute("username", username);
        model.addAttribute("email", email);

        if (password == null || password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "register";
        }
        if (!password.equals(confirm)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }
        if (userDAO.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already in use.");
            return "register";
        }

        User u = new User(UUID.randomUUID(), username, PasswordUtil.hash(password), email);
        try {
            boolean created = userDAO.register(u);
            if (created) return "redirect:/auth/login";
            model.addAttribute("error", "Registration failed. Please try again.");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("error", "An account with that email already exists.");
        }
        return "register";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }

}