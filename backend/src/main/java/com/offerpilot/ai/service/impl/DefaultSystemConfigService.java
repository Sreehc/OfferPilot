package com.offerpilot.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.ai.entity.SystemConfig;
import com.offerpilot.ai.mapper.SystemConfigMapper;
import com.offerpilot.ai.service.SystemConfigService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSystemConfigService implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    @Override
    public String getString(String key, String defaultValue) {
        try {
            SystemConfig config = getByKey(key);
            if (config == null || config.getEnabled() == null || config.getEnabled() == 0) {
                return defaultValue;
            }
            return StringUtils.hasText(config.getConfigValue()) ? config.getConfigValue() : defaultValue;
        } catch (Exception exception) {
            log.debug("Read system config {} failed, fallback to default: {}", key, exception.getMessage());
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        String value = getString(key, defaultValue == null ? null : String.valueOf(defaultValue));
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    @Override
    public List<SystemConfig> listAll() {
        try {
            return systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                    .orderByAsc(SystemConfig::getConfigGroup)
                    .orderByAsc(SystemConfig::getConfigKey));
        } catch (Exception exception) {
            log.debug("List system configs failed: {}", exception.getMessage());
            return List.of();
        }
    }

    @Override
    public SystemConfig save(String group, String key, String value, String valueType, String description, boolean enabled) {
        SystemConfig config = getByKey(key);
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(key);
        }
        config.setConfigGroup(group);
        config.setConfigValue(value);
        config.setValueType(valueType);
        config.setDescription(description);
        config.setEnabled(enabled ? 1 : 0);
        if (config.getId() == null) {
            systemConfigMapper.insert(config);
        } else {
            systemConfigMapper.updateById(config);
        }
        return config;
    }

    private SystemConfig getByKey(String key) {
        return systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
                .last("LIMIT 1"));
    }
}
