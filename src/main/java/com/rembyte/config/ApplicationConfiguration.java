package com.rembyte.config;

import com.rembyte.model.ServiceCategory;
import com.rembyte.repository.CategoryRepository;
import com.rembyte.service.AppUserService;
import com.rembyte.service.ChatService;
import com.rembyte.service.LegacyAttachmentMigrationService;
import com.rembyte.service.OrderLineMigrationService;
import com.rembyte.service.RepairServiceService;
import com.rembyte.service.KanbanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация и инициализация приложения
 */
@Configuration
public class ApplicationConfiguration {

    private final RepairServiceService serviceService;
    private final AppUserService userService;
    private final CategoryRepository categoryRepository;
    private final LegacyAttachmentMigrationService legacyAttachmentMigrationService;
    private final OrderLineMigrationService orderLineMigrationService;
    private final ChatService chatService;
    private final KanbanService kanbanService;

    @Value("${fixbyte.admin.username}")
    private String adminUsername;
    @Value("${fixbyte.admin.password}")
    private String adminPassword;
    @Value("${fixbyte.operator.username}")
    private String operatorUsername;
    @Value("${fixbyte.operator.password}")
    private String operatorPassword;

    public ApplicationConfiguration(RepairServiceService serviceService,
                                     AppUserService userService,
                                     CategoryRepository categoryRepository,
                                     LegacyAttachmentMigrationService legacyAttachmentMigrationService,
                                     OrderLineMigrationService orderLineMigrationService,
                                     ChatService chatService,
                                     KanbanService kanbanService) {
        this.serviceService    = serviceService;
        this.userService       = userService;
        this.categoryRepository = categoryRepository;
        this.legacyAttachmentMigrationService = legacyAttachmentMigrationService;
        this.orderLineMigrationService = orderLineMigrationService;
        this.chatService = chatService;
        this.kanbanService = kanbanService;
    }

    /**
     * Инициализировать стандартные данные при запуске
     */
    @Bean
    public ApplicationRunner initializeData() {
        return args -> {
            System.out.println("⚡ Инициализация FixByte CRM...");

            // Пользователи
            userService.initDefaultUsers(adminUsername, adminPassword, operatorUsername, operatorPassword);
            System.out.println("✅ Пользователи инициализированы");

            // Категории услуг
            initDefaultCategories();
            System.out.println("✅ Категории инициализированы");

            // Услуги
            serviceService.initializeDefaultServices();
            System.out.println("✅ Услуги инициализированы");

            // Миграция старых файлов-вложений в БД
            legacyAttachmentMigrationService.migrateOnStartup();

            // Миграция состава заказов order_services -> order_lines
            orderLineMigrationService.migrateOnStartup();

            // Конфигурация встроенного чата
            chatService.initializeDefaultWidgetSite();

            // Персональные канбан-доски для системных пользователей
            kanbanService.initializeDefaultBoardForUser(adminUsername);
            kanbanService.initializeDefaultBoardForUser(operatorUsername);
        };
    }

    private void initDefaultCategories() {
        if (categoryRepository.count() == 0) {
            Object[][] defaults = {
                {"BGA",                    "🔩"},
                {"Экран",                  "🖥️"},
                {"Батарея",                "🔋"},
                {"Материнская плата",      "🧩"},
                {"Обслуживание",           "⚙️"},
                {"Хранилище",              "💾"},
                {"Данные",                 "📂"},
                {"Программное обеспечение","💻"},
                {"Диагностика",            "🔍"},
                {"Охлаждение",             "❄️"},
                {"Периферия",              "🖱️"},
            };
            for (Object[] row : defaults) {
                categoryRepository.save(new ServiceCategory((String) row[0], (String) row[1]));
            }
        }
    }
}
