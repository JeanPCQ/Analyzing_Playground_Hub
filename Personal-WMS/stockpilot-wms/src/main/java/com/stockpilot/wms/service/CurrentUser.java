package com.stockpilot.wms.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Who is doing this? Used to stamp the inventory audit trail. */
public final class CurrentUser {

    private CurrentUser() { }

    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return "system";
        }
        return auth.getName();
    }
}
