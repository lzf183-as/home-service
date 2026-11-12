package com.community.homeservice.controller;

import com.community.homeservice.common.BusinessException;
import com.community.homeservice.common.LoginUser;
import com.community.homeservice.dto.AddressForm;
import com.community.homeservice.dto.ProfileForm;
import com.community.homeservice.entity.CustomerAddress;
import com.community.homeservice.entity.SysUser;
import com.community.homeservice.service.OrderService;
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
public class ProfileController {

    private final UserService userService;
    private final OrderService orderService;

    public ProfileController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal LoginUser loginUser,
                          @RequestParam(required = false) Long editAddressId,
                          Model model) {
        fillModel(loginUser.getId(), editAddressId, model);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal LoginUser loginUser,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillModel(loginUser.getId(), null, model);
            return "profile";
        }
        userService.updateProfile(loginUser.getId(), form);
        redirectAttributes.addFlashAttribute("success", "个人资料已更新");
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @AuthenticationPrincipal LoginUser loginUser,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.changePassword(loginUser.getId(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "密码修改成功，下次请使用新密码登录");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/addresses")
    public String saveAddress(@Valid @ModelAttribute("addressForm") AddressForm form,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal LoginUser loginUser,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillModel(loginUser.getId(), form.getId(), model);
            return "profile";
        }
        userService.saveAddress(loginUser.getId(), form);
        redirectAttributes.addFlashAttribute("success", "地址已保存");
        return "redirect:/profile";
    }

    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id,
                                @AuthenticationPrincipal LoginUser loginUser,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.deleteAddress(loginUser.getId(), id);
            redirectAttributes.addFlashAttribute("success", "地址已删除");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/addresses/{id}/default")
    public String setDefault(@PathVariable Long id,
                             @AuthenticationPrincipal LoginUser loginUser,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.setDefaultAddress(loginUser.getId(), id);
            redirectAttributes.addFlashAttribute("success", "已设为默认地址");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/profile";
    }

    private void fillModel(Long userId, Long editAddressId, Model model) {
        SysUser user = userService.getById(userId);
        List<CustomerAddress> addresses = userService.listAddresses(userId);
        model.addAttribute("user", user);
        model.addAttribute("addresses", addresses);
        model.addAttribute("orderCount", orderService.countUserOrders(userId));
        model.addAttribute("completedCount", orderService.countUserCompleted(userId));

        if (!model.containsAttribute("profileForm")) {
            ProfileForm pf = new ProfileForm();
            pf.setRealName(user.getRealName());
            pf.setPhone(user.getPhone());
            pf.setEmail(user.getEmail());
            model.addAttribute("profileForm", pf);
        }

        if (!model.containsAttribute("addressForm")) {
            AddressForm af = new AddressForm();
            if (editAddressId != null) {
                addresses.stream()
                        .filter(a -> a.getId().equals(editAddressId))
                        .findFirst()
                        .ifPresent(a -> {
                            af.setId(a.getId());
                            af.setContactName(a.getContactName());
                            af.setContactPhone(a.getContactPhone());
                            af.setRegion(a.getRegion());
                            af.setDetail(a.getDetail());
                            af.setSetDefault(a.getIsDefault() != null && a.getIsDefault() == 1);
                        });
            }
            model.addAttribute("addressForm", af);
        }
        model.addAttribute("editAddressId", editAddressId);
    }
}
