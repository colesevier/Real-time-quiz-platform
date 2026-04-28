package com.quiz.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PlayerLobbyController {

    @GetMapping("/play/{code}")
    public String lobby(@PathVariable String code, HttpSession session, Model model) {
        Object nickname = session.getAttribute("nickname");
        Object joinedCode = session.getAttribute("playerCode");
        if (nickname == null || !code.equals(joinedCode)) {
            return "redirect:/join?code=" + code;
        }
        model.addAttribute("code", code);
        model.addAttribute("nickname", nickname);
        return "player_lobby";
    }
}
