package com.offerpilot.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.offerpilot.agent.entity.UserProviderConfig;
import com.offerpilot.agent.mapper.UserProviderConfigMapper;
import com.offerpilot.agent.provider.ProviderSecretCrypto;
import com.offerpilot.agent.service.impl.UserProviderConfigServiceImpl;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import com.offerpilot.security.model.LoginUser;
import com.offerpilot.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserProviderConfigServiceImplTest {

    @Mock
    private UserProviderConfigMapper userProviderConfigMapper;

    @Test
    void checkCurrentUserConfigs_recomputesReadinessAndReturnsUpdatedView() {
        ProviderSecretCrypto crypto = new ProviderSecretCrypto("test-jwt-secret");
        UserProviderConfigServiceImpl service = new UserProviderConfigServiceImpl(userProviderConfigMapper, crypto);
        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        user.setRole("USER");
        user.setStatus(1);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(user), null, List.of()));

        UserProviderConfig llmConfig = new UserProviderConfig();
        llmConfig.setId(11L);
        llmConfig.setUserId(1L);
        llmConfig.setProviderScope("llm");
        llmConfig.setEnabled(Boolean.TRUE);
        llmConfig.setBaseUrl("https://api.example.com/v1");
        llmConfig.setModel("gpt-4.1-mini");
        llmConfig.setApiKeyCiphertext(crypto.encrypt("secret-key"));

        UserProviderConfig asrConfig = new UserProviderConfig();
        asrConfig.setId(12L);
        asrConfig.setUserId(1L);
        asrConfig.setProviderScope("asr");
        asrConfig.setEnabled(Boolean.TRUE);
        asrConfig.setProviderName("DashScope");

        when(userProviderConfigMapper.selectList(any())).thenReturn(List.of(llmConfig, asrConfig));

        try {
            List<UserProviderConfigItemVO> result = service.checkCurrentUserConfigs();

            verify(userProviderConfigMapper).updateById(llmConfig);
            verify(userProviderConfigMapper).updateById(asrConfig);
            assertEquals(6, result.size());

            UserProviderConfigItemVO llm = result.stream()
                    .filter(item -> "llm".equals(item.getScope()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("ready", llm.getStatus());
            assertTrue(llm.getStatusMessage().contains("可供对应能力使用"));

            UserProviderConfigItemVO asr = result.stream()
                    .filter(item -> "asr".equals(item.getScope()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("incomplete", asr.getStatus());
            assertTrue(asr.getStatusMessage().contains("API Key"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
