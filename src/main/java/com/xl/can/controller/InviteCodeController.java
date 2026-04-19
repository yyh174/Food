package com.xl.can.controller;

import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.dto.InviteCodeDetailResponse;
import com.xl.can.dto.InviteCodeResponse;
import com.xl.can.service.InviteCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invite-code")
public class InviteCodeController {

    @Autowired
    private InviteCodeService inviteCodeService;

    @PostMapping("/generate")
    public Result<InviteCodeResponse> generateInviteCode() {
        return inviteCodeService.generateInviteCode();
    }

    @GetMapping("/list")
    public Result<PageResult<InviteCodeDetailResponse>> getInviteCodeList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return inviteCodeService.getInviteCodeList(status, page, pageSize);
    }

    @PutMapping("/invalidate/{id}")
    public Result<Void> invalidateInviteCode(@PathVariable Long id) {
        return inviteCodeService.invalidateInviteCode(id);
    }

    @GetMapping("/latest")
    public Result<InviteCodeDetailResponse> getLatestUnusedInviteCode() {
        return inviteCodeService.getLatestUnusedInviteCode();
    }

    @PostMapping("/regenerate")
    public Result<InviteCodeResponse> regenerateInviteCode() {
        return inviteCodeService.regenerateLatestInviteCode();
    }
}
