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

    /**
     * Метка для cache-busting статики (?v=... у /js/*.js и /css/*.css).
     * На проде/докере статика отдаётся с Cache-Control: max-age=1h
     * (application-prod/docker.properties) — без этого браузер после деплоя
     * ещё час крутит старый JS/CSS против уже обновившегося HTML, и новые
     * куски интерфейса (которых не было в старом JS) просто не оживают.
     * Время сборки меняется при каждой mvn package — в отличие от версии в
     * pom.xml, её не нужно не забывать бампать вручную.
     */
    @ModelAttribute("assetVersion")
    public String assetVersion() {
        return buildProperties != null ? String.valueOf(buildProperties.getTime().toEpochMilli()) : "dev";
    }
}
