package com.xl.can.service;

import com.xl.can.common.Result;
import com.xl.can.dto.TicketTypeCreateDTO;
import com.xl.can.vo.TicketTypeVO;

import java.util.List;

public interface TicketTypeService {
    Result<List<TicketTypeVO>> list(String type);
    Result<TicketTypeVO> create(TicketTypeCreateDTO dto);
    Result<Void> delete(Long id);
}
