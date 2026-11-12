package com.community.homeservice.controller;

import com.community.homeservice.common.BusinessException;
import com.community.homeservice.common.LoginUser;
import com.community.homeservice.dto.StaffForm;
import com.community.homeservice.entity.Staff;
import com.community.homeservice.entity.SysUser;
import com.community.homeservice.enums.OrderStatus;
import com.community.homeservice.service.OrderService;
import com.community.homeservice.service.StaffService;
import com.community.homeservice.service.StatsService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final List<String> JOB_TYPES = List.of(
            "保洁", "家电清洗", "母婴护理", "维修", "养老陪护", "收纳整理");
    private static final List<String> LEVELS = List.of("初级", "中级", "金牌");

    private final StatsService statsService;
    private final OrderService orderService;
    private final StaffService staffService;
    private final UserService userService;

    public AdminController(StatsService statsService,
                           OrderService orderService,
                           StaffService staffService,
                           UserService userService) {
        this.statsService = statsService;
        this.orderService = orderService;
        this.staffService = staffService;
        this.userService = userService;
    }

    // ==================== 看板 ====================

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<StatsService.DayPoint> trend = statsService.trendLast7Days();
        model.addAttribute("overview", statsService.overview());
        model.addAttribute("trend", trend);
        model.addAttribute("maxTrend", Math.max(1, trend.stream().mapToLong(StatsService.DayPoint::count).max().orElse(0)));
        model.addAttribute("topServices", statsService.topServices(5));
        model.addAttribute("statusDist", statsService.statusDistribution());
        model.addAttribute("recentOrders", orderService.pageAllOrders(null, null, 1, 6).getRecords());
        model.addAttribute("staffNameMap", staffService.staffNameMap());
        return "admin/dashboard";
    }

    // ==================== 订单管理 ====================

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String status,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(defaultValue = "1") int page,
                         Model model) {
        model.addAttribute("orders", orderService.pageAllOrders(status, keyword, page, 10));
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("staffList", staffService.listViews());
        model.addAttribute("staffNameMap", staffService.staffNameMap());
        model.addAttribute("statusOptions", OrderStatus.values());
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/assign")
    public String assign(@PathVariable Long id,
                         @RequestParam Long staffId,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        try {
            orderService.assign(id, staffId, loginUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "派单成功");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(required = false) String reason,
                              @AuthenticationPrincipal LoginUser loginUser,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.adminCancel(id, reason, loginUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "订单已取消");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/orders";
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "1") int page,
                        Model model) {
        model.addAttribute("users", userService.pageUsers(keyword, page, 10));
        model.addAttribute("keyword", keyword);
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id,
                             @RequestParam int status,
                             RedirectAttributes redirectAttributes) {
        try {
            SysUser user = userService.getById(id);
            if ("ADMIN".equals(user.getRole())) {
                throw new BusinessException("不能禁用管理员账号");
            }
            userService.toggleStatus(id, status);
            redirectAttributes.addFlashAttribute("success", status == 1 ? "账号已启用" : "账号已禁用");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ==================== 服务人员管理 ====================

    @GetMapping("/staffs")
    public String staffs(Model model) {
        model.addAttribute("staffViews", staffService.listViews());
        model.addAttribute("jobTypes", JOB_TYPES);
        model.addAttribute("levels", LEVELS);
        return "admin/staffs";
    }

    @GetMapping("/staffs/new")
    public String newStaff(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new StaffForm());
        }
        model.addAttribute("jobTypes", JOB_TYPES);
        model.addAttribute("levels", LEVELS);
        return "admin/staff-form";
    }

    @PostMapping("/staffs")
    public String createStaff(@Valid @ModelAttribute("form") StaffForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("jobTypes", JOB_TYPES);
            model.addAttribute("levels", LEVELS);
            return "admin/staff-form";
        }
        try {
            staffService.createStaff(form);
            redirectAttributes.addFlashAttribute("success", "服务人员账号已创建");
            return "redirect:/admin/staffs";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/staffs/new";
        }
    }

    @PostMapping("/staffs/{id}/available")
    public String toggleAvailable(@PathVariable Long id,
                                  @RequestParam int available,
                                  RedirectAttributes redirectAttributes) {
        staffService.toggleAvailable(id, available);
        redirectAttributes.addFlashAttribute("success", available == 1 ? "已设为接单中" : "已设为休息中");
        return "redirect:/admin/staffs";
    }

    @PostMapping("/staffs/{id}/update")
    public String updateStaff(@PathVariable Long id,
                              @RequestParam String jobType,
                              @RequestParam String staffLevel,
                              @RequestParam(required = false) String intro,
                              RedirectAttributes redirectAttributes) {
        Staff staff = staffService.listAll().stream()
                .filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        if (staff == null) {
            redirectAttributes.addFlashAttribute("error", "服务人员不存在");
        } else {
            staffService.updateStaff(id, jobType, staffLevel, intro);
            redirectAttributes.addFlashAttribute("success", "服务人员信息已更新");
        }
        return "redirect:/admin/staffs";
    }
}
