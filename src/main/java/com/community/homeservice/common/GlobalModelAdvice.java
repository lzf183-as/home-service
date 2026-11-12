package com.community.homeservice.common;

import com.community.homeservice.enums.OrderStatus;
import com.community.homeservice.service.ServiceCatalogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 向所有页面注入公共模型：登录用户、订单状态文案、导航分类。
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final ServiceCatalogService catalogService;

    public GlobalModelAdvice(ServiceCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model, @AuthenticationPrincipal LoginUser loginUser) {
        model.addAttribute("currentUser", loginUser);

        Map<String, String> labels = new LinkedHashMap<>();
        Map<String, String> tones = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            labels.put(s.name(), s.getLabel());
            tones.put(s.name(), s.getTone());
        }
        model.addAttribute("statusLabels", labels);
        model.addAttribute("statusTones", tones);
        model.addAttribute("navCategories", catalogService.listEnabledCategories());
    }
}
