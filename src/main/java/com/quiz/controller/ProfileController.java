package com.quiz.controller;

import com.quiz.dao.ProfileDAO;
import com.quiz.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileDAO profileDAO;

    public ProfileController(ProfileDAO profileDAO) {
        this.profileDAO = profileDAO;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Pull userID from session, redirect to login if missing. Returns null on redirect. */
    private UUID resolveUser(HttpSession session) {
        Object id = session.getAttribute("userID");
        return (id instanceof UUID uid) ? uid : null;
    }

    // ── Profile page ─────────────────────────────────────────────────────────

    /**
     * GET /profile
     * Shows the user's current profile info and edit form.
     */
    @GetMapping
    public String showProfile(HttpSession session, Model model) {
        UUID userID = resolveUser(session);
        if (userID == null) return "redirect:/auth/login";

        Optional<User> userOpt = profileDAO.findById(userID);
        if (userOpt.isEmpty()) return "redirect:/auth/login";

        model.addAttribute("user", userOpt.get());
        return "profile";
    }

    /**
     * POST /profile
     * Updates displayName and bio.
     */
    @PostMapping
    public String updateProfile(@RequestParam(required = false) String displayName,
                                @RequestParam(required = false) String bio,
                                HttpSession session,
                                RedirectAttributes flash) {
        UUID userID = resolveUser(session);
        if (userID == null) return "redirect:/auth/login";

        // Validate bio length
        if (bio != null && bio.length() > 500) {
            flash.addFlashAttribute("profileError", "Bio must be 500 characters or fewer.");
            return "redirect:/profile";
        }

        boolean ok = profileDAO.updateProfile(userID, displayName, bio);
        if (ok) {
            flash.addFlashAttribute("profileSuccess", "Profile updated successfully.");
        } else {
            flash.addFlashAttribute("profileError", "Update failed. Please try again.");
        }
        return "redirect:/profile";
    }

    // ── Password change ───────────────────────────────────────────────────────

    /**
     * GET /profile/password
     * Shows the change-password form.
     */
    @GetMapping("/password")
    public String showPasswordForm(HttpSession session) {
        if (resolveUser(session) == null) return "redirect:/auth/login";
        return "profile_password";
    }

    /**
     * POST /profile/password
     * Verifies old password, then sets new one.
     */
    @PostMapping("/password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes flash) {
        UUID userID = resolveUser(session);
        if (userID == null) return "redirect:/auth/login";

        // Basic validation
        if (newPassword == null || newPassword.length() < 8) {
            flash.addFlashAttribute("pwError", "New password must be at least 8 characters.");
            return "redirect:/profile/password";
        }
        if (!newPassword.equals(confirmPassword)) {
            flash.addFlashAttribute("pwError", "Passwords do not match.");
            return "redirect:/profile/password";
        }

        boolean ok = profileDAO.changePassword(userID, oldPassword, newPassword);
        if (ok) {
            flash.addFlashAttribute("pwSuccess", "Password changed successfully.");
            return "redirect:/profile";
        } else {
            flash.addFlashAttribute("pwError", "Current password is incorrect.");
            return "redirect:/profile/password";
        }
    }
}