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
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private final UserDAO userDAO = new UserDAO();

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) throws SQLException {
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
    public String doRegister(@RequestParam String username, @RequestParam String email, @RequestParam String password, @RequestParam String confirm, Model model) throws SQLException {
        if (password == null || password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "register";
        }
        if (!password.equals(confirm)) {
            model.addAttribute("error", "Password and confirmation do not match.");
            return "register";
        }
        if (userDAO.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already in use.");
            return "register";
        }

        User u = new User(UUID.randomUUID(), username, PasswordUtil.hash(password), email);
        boolean created = userDAO.register(u);
        if (created) return "redirect:/auth/login";
        model.addAttribute("error", "Registration failed.");
        return "register";
    }
}
