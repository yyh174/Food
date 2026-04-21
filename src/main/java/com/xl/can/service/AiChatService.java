package com.xl.can.service;

import com.xl.can.dto.AiChatRequest;
import com.xl.can.vo.AiChatMessageVO;
import com.xl.can.vo.AiChatSessionVO;

import java.util.List;

public interface AiChatService {
    
    AiChatSessionVO chat(AiChatRequest request, Long userId);
    
    AiChatSessionVO createSession(Long userId);
    
    List<AiChatSessionVO> getSessionList(Long userId);
    
    List<AiChatMessageVO> getHistory(Long sessionId);
    
    void deleteSession(Long sessionId, Long userId);
}
