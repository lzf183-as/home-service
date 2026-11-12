package com.community.homeservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.homeservice.common.LoginUser;
import com.community.homeservice.entity.CustomerAddress;
import com.community.homeservice.entity.ServiceCategory;
import com.community.homeservice.entity.ServiceItem;
import com.community.homeservice.service.OrderService;
import com.community.homeservice.service.ServiceCatalogService;
import com.community.homeservice.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/services")
public class ServiceController {

    private final ServiceCatalogService catalogService;
    private final OrderService orderService;
    private final UserService userService;

    public ServiceController(ServiceCatalogService catalogService,
                             OrderService orderService,
                             UserService userService) {
        this.catalogService = catalogService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        Page<ServiceItem> result = catalogService.pageServices(categoryId, keyword, page, 9);
        model.addAttribute("page", result);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryNameMap", catalogService.categoryNameMap());
        model.addAttribute("currentCategory",
                categoryId == null ? null : catalogService.getCategory(categoryId));
        return "service/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser loginUser,
                         Model model) {
        ServiceItem item = catalogService.getEnabledService(id);
        ServiceCategory category = catalogService.getCategory(item.getCategoryId());

        model.addAttribute("service", item);
        model.addAttribute("category", category);
        model.addAttribute("reviews", orderService.listReviewsByService(id));
        model.addAttribute("related", catalogService.pageServices(item.getCategoryId(), null, 1, 4).getRecords()
                .stream().filter(s -> !s.getId().equals(id)).limit(3).toList());

        if (loginUser != null) {
            List<CustomerAddress> addresses = userService.listAddresses(loginUser.getId());
            model.addAttribute("addresses", addresses);
            model.addAttribute("defaultAddress", addresses.isEmpty() ? null : addresses.get(0));
        }
        return "service/detail";
    }
}
