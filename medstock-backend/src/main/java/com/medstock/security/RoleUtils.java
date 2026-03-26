package com.medstock.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RoleUtils {

    private static final List<String> PRIORITY_ORDER = List.of("ADMIN", "OWNER", "EMPLOYEE");

    private RoleUtils() {
    }

    public static List<String> parseRoles(String rawRoles) {
        if (rawRoles == null || rawRoles.isBlank()) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String candidate : rawRoles.split(",")) {
            String role = candidate == null ? "" : candidate.trim().toUpperCase(Locale.ROOT);
            if (!role.isBlank()) {
                normalized.add(role);
            }
        }
        return List.copyOf(normalized);
    }

    public static String serializeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return String.join(",", new LinkedHashSet<>(roles));
    }

    public static String primaryRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        for (String prioritized : PRIORITY_ORDER) {
            if (roles.contains(prioritized)) {
                return prioritized;
            }
        }

        return roles.getFirst();
    }

    public static List<String> addRole(String existingRoles, String roleToAdd) {
        List<String> roles = new ArrayList<>(parseRoles(existingRoles));
        String normalized = roleToAdd == null ? "" : roleToAdd.trim().toUpperCase(Locale.ROOT);
        if (!normalized.isBlank() && !roles.contains(normalized)) {
            roles.add(normalized);
        }
        return List.copyOf(roles);
    }

    public static boolean hasRole(String rawRoles, String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return parseRoles(rawRoles).contains(normalized);
    }
}
