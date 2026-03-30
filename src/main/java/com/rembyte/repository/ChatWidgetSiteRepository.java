package com.rembyte.repository;

import com.rembyte.model.ChatWidgetSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatWidgetSiteRepository extends JpaRepository<ChatWidgetSite, Long> {
    Optional<ChatWidgetSite> findBySiteKey(String siteKey);
}

