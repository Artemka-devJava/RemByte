package com.rembyte.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicChatWidgetController {

    @GetMapping("/widget/chat/frame")
    public String chatFrame(@RequestParam(defaultValue = "main-site") String siteKey,
                            @RequestParam(required = false) String parentOrigin,
                            Model model) {
        model.addAttribute("siteKey", siteKey);
        model.addAttribute("parentOrigin", parentOrigin == null ? "" : parentOrigin);
        return "chat-widget-frame";
    }
}
