package com.community.homeservice.controller;

import com.community.homeservice.common.BusinessException;
import com.community.homeservice.common.LoginUser;
import com.community.homeservice.entity.Staff;
import com.community.homeservice.enums.OrderStatus;
import com.community.homeservice.service.OrderService;
import com.community.homeservice.service.StaffService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;
    private final OrderService orderService;

    public StaffController(StaffService staffService, OrderService orderService) {
        this.staffService = staffService;
        this.orderService = orderService;
    }

    @GetMapping("/workbench")
    public String workbench(@AuthenticationPrincipal LoginUser loginUser,
                            @RequestParam(defaultValue = "pool") String tab,
                            @RequestParam(required = false) String status,
                            @RequestParam(defaultValue = "1") int page,
                            Model model) {
        Staff staff = staffService.getByUserId(loginUser.getId());
        model.addAttribute("staff", staff);
        model.addAttribute("tab", tab);

        if ("mine".equals(tab)) {
            model.addAttribute("orders", orderService.pageStaffOrders(staff.getId(), status, page, 8));
            model.addAttribute("status", status);
        } else {
            model.addAttribute("orders", orderService.pageAvailableOrders(page, 8));
        }

        model.addAttribute("acceptedCount", orderService.countStaffOrders(staff.getId(), OrderStatus.ACCEPTED.name()));
        model.addAttribute("servingCount", orderService.countStaffOrders(staff.getId(), OrderStatus.SERVING.name()));
        model.addAttribute("completedCount", orderService.countStaffOrders(staff.getId(), OrderStatus.COMPLETED.name()));
        return "staff/workbench";
    }

    @PostMapping("/orders/{id}/accept")
    public String accept(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        try {
            Staff staff = staffService.getByUserId(loginUser.getId());
            orderService.accept(id, staff.getId(), loginUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "接单成功，请按预约时间上门服务");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/staff/workbench?tab=mine";
    }

    @PostMapping("/orders/{id}/start")
    public String start(@PathVariable Long id,
                        @AuthenticationPrincipal LoginUser loginUser,
                        RedirectAttributes redirectAttributes) {
        try {
            Staff staff = staffService.getByUserId(loginUser.getId());
            orderService.start(id, staff.getId(), loginUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "已开始服务");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/staff/workbench?tab=mine";
    }

    @PostMapping("/orders/{id}/finish")
    public String finish(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        try {
            Staff staff = staffService.getByUserId(loginUser.getId());
            orderService.finish(id, staff.getId(), loginUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "已提交完工，等待用户确认");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/staff/workbench?tab=mine";
    }
}
