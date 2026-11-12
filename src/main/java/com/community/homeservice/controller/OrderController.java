package com.community.homeservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.homeservice.common.BusinessException;
import com.community.homeservice.common.LoginUser;
import com.community.homeservice.dto.BookingForm;
import com.community.homeservice.dto.ReviewForm;
import com.community.homeservice.entity.CustomerAddress;
import com.community.homeservice.entity.ServiceItem;
import com.community.homeservice.entity.ServiceOrder;
import com.community.homeservice.service.OrderService;
import com.community.homeservice.service.ServiceCatalogService;
import com.community.homeservice.service.StaffService;
import com.community.homeservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class OrderController {

    private static final List<String> TIME_SLOTS = List.of(
            "08:00-10:00", "10:00-12:00", "13:00-15:00", "15:00-17:00", "18:00-20:00");

    private final OrderService orderService;
    private final ServiceCatalogService catalogService;
    private final UserService userService;
    private final StaffService staffService;

    public OrderController(OrderService orderService,
                           ServiceCatalogService catalogService,
                           UserService userService,
                           StaffService staffService) {
        this.orderService = orderService;
        this.catalogService = catalogService;
        this.userService = userService;
        this.staffService = staffService;
    }

    // ==================== 预约 ====================

    @GetMapping("/booking/{serviceId}")
    public String bookingPage(@PathVariable Long serviceId,
                              @AuthenticationPrincipal LoginUser loginUser,
                              Model model) {
        ServiceItem item = catalogService.getEnabledService(serviceId);
        List<CustomerAddress> addresses = userService.listAddresses(loginUser.getId());

        BookingForm form = new BookingForm();
        form.setServiceId(serviceId);
        form.setQuantity(1);
        form.setAppointmentDate(java.time.LocalDate.now().plusDays(1));
        form.setAppointmentTime("10:00-12:00");
        if (!addresses.isEmpty()) {
            CustomerAddress addr = addresses.get(0);
            form.setAddressId(addr.getId());
            form.setContactName(addr.getContactName());
            form.setContactPhone(addr.getContactPhone());
            form.setRegion(addr.getRegion());
            form.setDetail(addr.getDetail());
        }

        model.addAttribute("service", item);
        model.addAttribute("form", form);
        model.addAttribute("addresses", addresses);
        model.addAttribute("timeSlots", TIME_SLOTS);
        model.addAttribute("today", java.time.LocalDate.now());
        return "order/booking";
    }

    @PostMapping("/orders")
    public String create(@Valid @ModelAttribute("form") BookingForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal LoginUser loginUser,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("service", catalogService.getService(form.getServiceId()));
            model.addAttribute("addresses", userService.listAddresses(loginUser.getId()));
            model.addAttribute("timeSlots", TIME_SLOTS);
            model.addAttribute("today", java.time.LocalDate.now());
            return "order/booking";
        }
        ServiceOrder order = orderService.create(loginUser.getId(), form);
        redirectAttributes.addFlashAttribute("success", "预约提交成功，等待服务人员接单");
        return "redirect:/orders/" + order.getId();
    }

    // ==================== 我的订单 ====================

    @GetMapping("/orders")
    public String myOrders(@AuthenticationPrincipal LoginUser loginUser,
                           @RequestParam(required = false) String status,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        Page<ServiceOrder> orders = orderService.pageUserOrders(loginUser.getId(), status, page, 8);
        model.addAttribute("page", orders);
        model.addAttribute("status", status);
        model.addAttribute("staffNameMap", staffService.staffNameMap());
        return "order/list";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id,
                              @AuthenticationPrincipal LoginUser loginUser,
                              Model model) {
        ServiceOrder order = orderService.getUserOrder(id, loginUser.getId());
        model.addAttribute("order", order);
        model.addAttribute("logs", orderService.listLogs(id));
        model.addAttribute("review", orderService.getReview(id));
        model.addAttribute("staffName", order.getStaffId() == null ? null
                : staffService.staffNameMap().get(order.getStaffId()));
        model.addAttribute("canCancel",
                com.community.homeservice.enums.OrderStatus.canCancel(order.getStatus()));

        List<String> flow = List.of("PENDING_ACCEPT", "ACCEPTED", "SERVING", "PENDING_CONFIRM", "COMPLETED");
        model.addAttribute("flowLabels", flow.stream()
                .map(com.community.homeservice.enums.OrderStatus::labelOf)
                .toList());
        model.addAttribute("flowIndex", flow.indexOf(order.getStatus()));
        return "order/detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        try {
            orderService.cancel(id, loginUser.getId(), reason, loginUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "订单已取消");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/confirm")
    public String confirm(@PathVariable Long id,
                          @AuthenticationPrincipal LoginUser loginUser,
                          RedirectAttributes redirectAttributes) {
        try {
            orderService.confirm(id, loginUser.getId(), loginUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "已确认完成，感谢您的信任");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/review")
    public String review(@PathVariable Long id,
                         @Valid @ModelAttribute ReviewForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        form.setOrderId(id);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "评分需为 1-5 星，评价最多 500 字");
            return "redirect:/orders/" + id;
        }
        try {
            orderService.review(loginUser.getId(), form, loginUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "评价已提交，感谢您的反馈");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
