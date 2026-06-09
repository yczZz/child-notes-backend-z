package com.ycz.childnotesbackend.service;

import com.ycz.childnotesbackend.model.dto.admin.AdminLotteryDto;
import com.ycz.childnotesbackend.model.dto.admin.AdminLotteryRequest;
import com.ycz.childnotesbackend.model.dto.admin.AdminPageResponse;

public interface AdminLotteryService {

    AdminPageResponse<AdminLotteryDto> listLotteries(int page, int pageSize, String status);

    AdminLotteryDto getLottery(Long id);

    AdminLotteryDto createLottery(AdminLotteryRequest request);

    AdminLotteryDto updateLottery(Long id, AdminLotteryRequest request);

    AdminLotteryDto publishLottery(Long id);

    AdminLotteryDto closeLottery(Long id);
}
