package com.ycz.childnotesbackend.controller.admin;

import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.dto.admin.AdminLotteryDto;
import com.ycz.childnotesbackend.model.dto.admin.AdminLotteryRequest;
import com.ycz.childnotesbackend.model.dto.admin.AdminPageResponse;
import com.ycz.childnotesbackend.service.AdminLotteryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@RestController
@RequestMapping("/admin/api/lotteries")
public class AdminLotteryController {

    private final AdminLotteryService adminLotteryService;

    public AdminLotteryController(AdminLotteryService adminLotteryService) {
        this.adminLotteryService = adminLotteryService;
    }

    @GetMapping
    public Response<AdminPageResponse<AdminLotteryDto>> list(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int pageSize,
                                                            @RequestParam(defaultValue = "all") String status) {
        return handle(() -> adminLotteryService.listLotteries(page, pageSize, status));
    }

    @GetMapping("/{id}")
    public Response<AdminLotteryDto> get(@PathVariable Long id) {
        return handle(() -> adminLotteryService.getLottery(id));
    }

    @PostMapping
    public Response<AdminLotteryDto> create(@RequestBody AdminLotteryRequest request) {
        return handle(() -> adminLotteryService.createLottery(request));
    }

    @PutMapping("/{id}")
    public Response<AdminLotteryDto> update(@PathVariable Long id, @RequestBody AdminLotteryRequest request) {
        return handle(() -> adminLotteryService.updateLottery(id, request));
    }

    @PostMapping("/{id}/publish")
    public Response<AdminLotteryDto> publish(@PathVariable Long id) {
        return handle(() -> adminLotteryService.publishLottery(id));
    }

    @PostMapping("/{id}/close")
    public Response<AdminLotteryDto> close(@PathVariable Long id) {
        return handle(() -> adminLotteryService.closeLottery(id));
    }

    private <T> Response<T> handle(Supplier<T> supplier) {
        try {
            return new Response<>(supplier.get());
        } catch (RuntimeException e) {
            return new Response<>("000520", e.getMessage());
        }
    }
}
