package com.medstock.service.ai;

import com.medstock.entity.AiProviderConfig;

public final class AiProviderRuntimeContext {

    private static final ThreadLocal<AiProviderConfig> ACTIVE_CONFIG = new ThreadLocal<>();

    private AiProviderRuntimeContext() {
    }

    public static void set(AiProviderConfig config) {
        ACTIVE_CONFIG.set(config);
    }

    public static AiProviderConfig get() {
        return ACTIVE_CONFIG.get();
    }

    public static void clear() {
        ACTIVE_CONFIG.remove();
    }
}
