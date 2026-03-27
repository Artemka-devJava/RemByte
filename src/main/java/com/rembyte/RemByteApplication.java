package com.rembyte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FixByte CRM - Приложение для управления сервисом по ремонту ПК и ноутбуков
 */
@SpringBootApplication
public class RemByteApplication {
    public static void main(String[] args) {
        SpringApplication.run(RemByteApplication.class, args);
        System.out.println("⚡ FixByte CRM запущен!");
        System.out.println("📱 Веб-интерфейс доступен по адресу: http://localhost:9087");
    }
}

