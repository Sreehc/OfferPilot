package com.offerpilot.ai.service;

import com.offerpilot.ai.entity.SystemConfig;
import java.util.List;

public interface SystemConfigService {
    String getString(String key, String defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    Integer getInteger(String key, Integer defaultValue);

    List<SystemConfig> listAll();

    SystemConfig save(String group, String key, String value, String valueType, String description, boolean enabled);
}
