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
import org.springframework.web.util.HtmlUtils;
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
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "Username and password are required.");
            return "login";
        }
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

    @PostMapping("/guest")
    public String doGuest(@RequestParam(name = "guestName", required = false) String guestName,
                          HttpSession session,
                          Model model) {
        String g = guestName == null ? "" : guestName.trim();
        // Validate: non-empty and <= 24 chars (same as JoinController)
        if (g.isEmpty()) {
            model.addAttribute("guestError", "Please enter a display name.");
            model.addAttribute("guestName", "");
            return "login";
        }
        if (g.length() > 24) {
            model.addAttribute("guestError", "Display name must be 24 characters or fewer.");
            model.addAttribute("guestName", g);
            return "login";
        }

        // Basic sanitization to avoid HTML injection in views. Views should still escape output.
        String sanitized = HtmlUtils.htmlEscape(g);
        session.setAttribute("guestName", sanitized);
        return "redirect:/join";
    }

    private static final java.util.regex.Pattern EMAIL_PATTERN =
        java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String confirm,
                             Model model) {
        // Always repopulate so fields survive a validation error
        model.addAttribute("username", username == null ? "" : username.trim());
        model.addAttribute("email",    email    == null ? "" : email.trim());

        String u = username == null ? "" : username.trim();
        String e = email    == null ? "" : email.trim();
        String p = password == null ? "" : password;
        String c = confirm  == null ? "" : confirm;

        // 1. Empty field checks
        if (u.isEmpty() || e.isEmpty() || p.isEmpty() || c.isEmpty()) {
            model.addAttribute("error", "All fields are required.");
            return "register";
        }

        // 2. Username length (matches DB VARCHAR(50))
        if (u.length() > 50) {
            model.addAttribute("error", "Username must be 50 characters or fewer.");
            return "register";
        }

        // 3. Email format
        if (!EMAIL_PATTERN.matcher(e).matches()) {
            model.addAttribute("error", "Please enter a valid email address.");
            return "register";
        }

        // 4. Password length
        if (p.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "register";
        }

        // 5. Password confirmation
        if (!p.equals(c)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }

        // 6. Username uniqueness
        if (userDAO.findByUsername(u).isPresent()) {
            model.addAttribute("error", "Username already in use.");
            return "register";
        }

        // 7. Persist — catch duplicate email at DB level
        User newUser = new User(UUID.randomUUID(), u, PasswordUtil.hash(p), e);
        try {
            boolean created = userDAO.register(newUser);
            if (created) return "redirect:/auth/login";
            model.addAttribute("error", "Registration failed. Please try again.");
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
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