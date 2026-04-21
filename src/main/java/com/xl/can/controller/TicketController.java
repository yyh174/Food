package com.xl.can.controller;

import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.dto.*;
import com.xl.can.service.TicketService;
import com.xl.can.service.TicketTypeService;
import com.xl.can.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketTypeService ticketTypeService;

    @GetMapping("/types")
    public Result<List<TicketTypeVO>> listTypes(@RequestParam(required = false) String type) {
        return ticketTypeService.list(type);
    }

    @PostMapping("/types")
    public Result<TicketTypeVO> createType(@RequestBody TicketTypeCreateDTO dto) {
        return ticketTypeService.create(dto);
    }

    @DeleteMapping("/types/{id}")
    public Result<Void> deleteType(@PathVariable Long id) {
        return ticketTypeService.delete(id);
    }

    @GetMapping
    public Result<PageResult<TicketListVO>> list(TicketListDTO dto) {
        return ticketService.list(dto);
    }

    @GetMapping("/{id}")
    public Result<TicketDetailVO> getDetail(@PathVariable Long id) {
        return ticketService.getDetail(id);
    }

    @PostMapping
    public Result<TicketListVO> create(@RequestBody TicketCreateDTO dto) {
        return ticketService.create(dto);
    }

    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @RequestBody TicketAssignDTO dto) {
        return ticketService.assign(id, dto);
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id, @RequestBody TicketActionDTO dto) {
        return ticketService.submit(id, dto);
    }

    @PostMapping("/{id}/verify")
    public Result<Void> verify(@PathVariable Long id, @RequestBody TicketActionDTO dto) {
        return ticketService.verify(id, dto);
    }

    @PostMapping("/{id}/archive")
    public Result<Void> archive(@PathVariable Long id, @RequestBody TicketActionDTO dto) {
        return ticketService.archive(id, dto);
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody TicketRejectDTO dto) {
        return ticketService.reject(id, dto);
    }

    @GetMapping("/statistics")
    public Result<TicketStatisticsVO> statistics(TicketStatisticsDTO dto) {
        return ticketService.statistics(dto);
    }
}
