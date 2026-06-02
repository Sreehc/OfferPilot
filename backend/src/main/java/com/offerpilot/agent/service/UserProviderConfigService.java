package com.offerpilot.agent.service;

import com.offerpilot.agent.dto.ProviderConfigUpdateItemRequest;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import java.util.List;

public interface UserProviderConfigService {
    List<UserProviderConfigItemVO> listCurrentUserConfigs();

    List<UserProviderConfigItemVO> updateCurrentUserConfigs(List<ProviderConfigUpdateItemRequest> requests);
}
