package com.rembyte.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Версия сервиса для шаблонов (sidebar-shell.html, login.html), чтобы в UI
 * всегда было видно, какая сборка запущена. Берётся из
 * META-INF/build-info.properties (генерируется spring-boot:build-info из
 * версии в pom.xml при `mvn package`); при `spring-boot:run` без пакетной
 * сборки файла нет — тогда показываем "dev".
 */
@ControllerAdvice(annotations = Controller.class)
public class AppVersionModelAdvice {

    private final BuildProperties buildProperties;

    public AppVersionModelAdvice(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties.getIfAvailable();
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        return buildProperties != null ? buildProperties.getVersion() : "dev";
    }
}
