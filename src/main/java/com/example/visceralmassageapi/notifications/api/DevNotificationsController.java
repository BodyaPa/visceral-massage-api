package com.example.visceralmassageapi.notifications.api;

import com.example.visceralmassageapi.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dev/notifications")
@RequiredArgsConstructor
@Profile("dev")
public class DevNotificationsController {

    private final NotificationService notificationService;

    @PostMapping("/email-test")
    public String emailTest(@RequestParam String to) {
        notificationService.sendEmail(
                to,
                "Test notification",
                "notifications life!!!"
        );
        return "ok";
    }
}