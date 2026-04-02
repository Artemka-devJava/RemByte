package com.rembyte.controller;

import com.rembyte.service.PluginSettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Контроллер для веб-интерфейса FixByte CRM
 */
@Controller
public class WebController {

    private final PluginSettingsService pluginSettingsService;

    public WebController(PluginSettingsService pluginSettingsService) {
        this.pluginSettingsService = pluginSettingsService;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        // Предсоздаем сессию до рендера шаблона, чтобы избежать
        // "Cannot create a session after the response has been committed"
        request.getSession(true);
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

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }


    @GetMapping("/plugins")
    public String plugins() {
        return "plugins";
    }

    @GetMapping("/kanban")
    public String kanban() {
        return "kanban";
    }

    @GetMapping("/notes")
    public String notes() {
        if (!pluginSettingsService.isNotesPluginEnabled()) {
            return "redirect:/dashboard?notesDisabled=true";
        }
        return "notes";
    }
}
