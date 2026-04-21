package com.xl.can.controller;

import com.xl.can.common.Result;
import com.xl.can.context.UserContext;
import com.xl.can.dto.AiChatRequest;
import com.xl.can.service.AiChatService;
import com.xl.can.vo.AiChatMessageVO;
import com.xl.can.vo.AiChatSessionVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/chat")
    public Result<AiChatSessionVO> chat(@Valid @RequestBody AiChatRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }
        AiChatSessionVO session = aiChatService.chat(request, userId);
        return Result.success(session);
    }

    @GetMapping("/history/{sessionId}")
    public Result<List<AiChatMessageVO>> getHistory(@PathVariable Long sessionId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }
        List<AiChatMessageVO> messages = aiChatService.getHistory(sessionId);
        return Result.success(messages);
    }

    @PostMapping("/new-session")
    public Result<AiChatSessionVO> newSession() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }
        AiChatSessionVO session = aiChatService.createSession(userId);
        return Result.success(session);
    }

    @GetMapping("/sessions")
    public Result<List<AiChatSessionVO>> getSessions() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }
        List<AiChatSessionVO> sessions = aiChatService.getSessionList(userId);
        return Result.success(sessions);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }
        aiChatService.deleteSession(sessionId, userId);
        return Result.success();
    }
}
