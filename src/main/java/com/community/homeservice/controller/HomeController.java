package com.community.homeservice.controller;

import com.community.homeservice.common.BusinessException;
import com.community.homeservice.common.LoginUser;
import com.community.homeservice.dto.RegisterForm;
import com.community.homeservice.service.ServiceCatalogService;
import com.community.homeservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final ServiceCatalogService catalogService;
    private final UserService userService;

    public HomeController(ServiceCatalogService catalogService, UserService userService) {
        this.catalogService = catalogService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("categories", catalogService.listEnabledCategories());
        model.addAttribute("hotServices", catalogService.listHotServices(6));
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser != null) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        if (loginUser != null) {
            return "redirect:/";
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(form);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("success", "注册成功，请使用新账号登录");
        return "redirect:/login";
    }

    /** 兼容 GET 直接访问与安全框架对 POST 的转发，避免非法请求落到 405 */
    @RequestMapping(value = "/403", method = {RequestMethod.GET, RequestMethod.POST})
    public String forbidden() {
        return "error/403";
    }
}
