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
import java.util.regex.Pattern;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private final UserDAO userDAO = new UserDAO();

    // Email format regex
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username, @RequestParam String password,
                          HttpSession session, Model model) throws SQLException {
        boolean ok = userDAO.validateCredentials(username, password);
        if (ok) {
            Optional<User> u = userDAO.findByUsername(username);
            session.setAttribute("username", username);
            session.setAttribute("userID", u.map(User::getUserID).orElse(null));
            session.setAttribute("isGuest", false);
            session.setMaxInactiveInterval(30 * 60);
            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", "Invalid username or password.");
            return "login";
        }
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username, @RequestParam String email,
                             @RequestParam String password, @RequestParam String confirm,
                             Model model) throws SQLException {
        // Password length
        if (password == null || password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "register";
        }
        // Password match
        if (!password.equals(confirm)) {
            model.addAttribute("error", "Password and confirmation do not match.");
            return "register";
        }
        // Email format
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            model.addAttribute("error", "Please enter a valid email address.");
            return "register";
        }
        // Username taken
        if (userDAO.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already in use.");
            return "register";
        }
        // Email taken
        if (userDAO.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Email address already registered.");
            return "register";
        }

        User u = new User(UUID.randomUUID(), username, PasswordUtil.hash(password), email);
        boolean created = userDAO.register(u);
        if (created) return "redirect:/auth/login";
        model.addAttribute("error", "Registration failed.");
        return "register";
    }

    // Guest login
    @PostMapping("/guest")
    public String doGuest(@RequestParam String guestName, HttpSession session, Model model) {
        if (guestName == null || guestName.isBlank()) {
            model.addAttribute("error", "Please enter a name.");
            return "login";
        }
        session.setAttribute("username", guestName);
        session.setAttribute("userID", null);
        session.setAttribute("isGuest", true);
        session.setMaxInactiveInterval(30 * 60);
        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }
}