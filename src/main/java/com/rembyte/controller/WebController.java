package com.rembyte.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для веб-интерфейса FixByte CRM
 */
@Controller
public class WebController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }

    @GetMapping("/clients")
    public String clients() {
        return "clients";
    }

    @GetMapping("/services")
    public String services() {
        return "services";
    }

    @GetMapping("/calculator")
    public String calculator() {
        return "calculator";
    }

    @GetMapping("/users")
    public String users() {
        return "users";
    }
}

