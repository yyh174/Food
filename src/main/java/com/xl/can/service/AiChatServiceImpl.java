package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xl.can.ai.tool.OrderQueryTool;
import com.xl.can.ai.tool.ReviewAnalysisTool;
import com.xl.can.ai.tool.SalesQueryTool;
import com.xl.can.context.UserContext;
import com.xl.can.dto.AiChatRequest;
import com.xl.can.entity.AiChatMessage;
import com.xl.can.entity.AiChatSession;
import com.xl.can.mapper.AiChatMessageMapper;
import com.xl.can.mapper.AiChatSessionMapper;
import com.xl.can.vo.AiChatMessageVO;
import com.xl.can.vo.AiChatSessionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    @Autowired
    private AiChatSessionMapper sessionMapper;

    @Autowired
    private AiChatMessageMapper messageMapper;

    @Autowired
    private SalesQueryTool salesQueryTool;

    @Autowired
    private ReviewAnalysisTool reviewAnalysisTool;

    @Autowired
    private OrderQueryTool orderQueryTool;

    @Autowired
    private ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
        你是一个友好的餐饮数据分析助手。
        
        【必须严格遵守的规则】
        1. 只输出纯文本，禁止JSON、禁止代码块、禁止任何格式标记
        2. 用户打招呼说"你好"、"hi"、"嗨"时，直接回复"您好！有什么可以帮您的？"即可，不要调用任何工具
        3. 只有用户明确询问销量、订单、客流、评价等数据时才调用工具
        4. 回复简短，20字以内
        """;

    @Override
    public AiChatSessionVO chat(AiChatRequest request, Long userId) {
        Long tenantId = UserContext.getTenantId();
        Long shopId = UserContext.getShopId();

        AiChatSession session;
        if (request.getSessionId() != null) {
            session = sessionMapper.selectById(request.getSessionId());
            if (session == null || !session.getUserId().equals(userId)) {
                throw new RuntimeException("会话不存在或无权限访问");
            }
        } else {
            session = createNewSession(userId, tenantId, shopId);
        }

        // Save user message
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setRole("user");
        userMessage.setContent(request.getContent());
        messageMapper.insert(userMessage);

        // Generate title for new sessions
        if (session.getTitle() == null || session.getTitle().isEmpty()) {
            String title = request.getContent();
            if (title.length() > 30) {
                title = title.substring(0, 30) + "...";
            }
            session.setTitle(title);
            sessionMapper.updateById(session);
        }

        Long sessionId = session.getId();

        try {
            // 构建消息历史
            List<Message> messages = buildMessages(sessionId, request.getContent());

            log.info("开始调用 AI 接口，消息数: {}", messages.size());

            // 调用 AI（自动处理 Function Calling）
            String response = chatClient.prompt()
                    .messages(messages)
                    .tools(salesQueryTool, reviewAnalysisTool, orderQueryTool)
                    .call()
                    .content();

            log.info("AI 响应: {}", response);

            // 保存响应
            if (response != null && !response.isEmpty()) {
                AiChatMessage assistantMessage = new AiChatMessage();
                assistantMessage.setSessionId(sessionId);
                assistantMessage.setRole("assistant");
                assistantMessage.setContent(response);
                messageMapper.insert(assistantMessage);
            }

            return buildSessionVO(session);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI chat error: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
        }
    }

    private List<Message> buildMessages(Long sessionId, String currentContent) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        List<AiChatMessage> history = getChatHistory(sessionId);
        for (AiChatMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        messages.add(new UserMessage(currentContent));
        return messages;
    }

    private List<AiChatMessage> getChatHistory(Long sessionId) {
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessage::getSessionId, sessionId)
               .orderByAsc(AiChatMessage::getCreatedAt);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public AiChatSessionVO createSession(Long userId) {
        Long tenantId = UserContext.getTenantId();
        Long shopId = UserContext.getShopId();
        AiChatSession session = createNewSession(userId, tenantId, shopId);

        return buildSessionVO(session);
    }

    @Override
    public List<AiChatSessionVO> getSessionList(Long userId) {
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatSession::getUserId, userId)
               .orderByDesc(AiChatSession::getCreatedAt);
        List<AiChatSession> sessions = sessionMapper.selectList(wrapper);
        return sessions.stream().map(this::buildSessionVO).collect(Collectors.toList());
    }

    @Override
    public List<AiChatMessageVO> getHistory(Long sessionId) {
        List<AiChatMessage> messages = getChatHistory(sessionId);
        // 过滤掉占位符消息
        return messages.stream()
                .filter(m -> !"正在分析中...".equals(m.getContent()))
                .map(this::buildMessageVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此会话");
        }
        messageMapper.delete(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    private AiChatSession createNewSession(Long userId, Long tenantId, Long shopId) {
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTenantId(tenantId);
        session.setShopId(shopId);
        sessionMapper.insert(session);
        return session;
    }

    private AiChatSessionVO buildSessionVO(AiChatSession session) {
        AiChatSessionVO vo = new AiChatSessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }

    private AiChatMessageVO buildMessageVO(AiChatMessage message) {
        AiChatMessageVO vo = new AiChatMessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}
