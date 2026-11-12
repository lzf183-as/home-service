package com.community.homeservice.controller;

import com.community.homeservice.common.BusinessException;
import com.community.homeservice.dto.CategoryForm;
import com.community.homeservice.dto.ServiceItemForm;
import com.community.homeservice.entity.ServiceCategory;
import com.community.homeservice.entity.ServiceItem;
import com.community.homeservice.service.ServiceCatalogService;
import jakarta.validation.Valid;
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

@Controller
@RequestMapping("/admin")
public class AdminCatalogController {

    private final ServiceCatalogService catalogService;

    public AdminCatalogController(ServiceCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // ==================== 分类 ====================

    @GetMapping("/categories")
    public String categories(@RequestParam(required = false) Long editId, Model model) {
        if (!model.containsAttribute("form")) {
            CategoryForm form = new CategoryForm();
            if (editId != null) {
                ServiceCategory category = catalogService.getCategory(editId);
                if (category != null) {
                    form.setId(category.getId());
                    form.setName(category.getName());
                    form.setIcon(category.getIcon());
                    form.setDescription(category.getDescription());
                    form.setSortNo(category.getSortNo());
                    form.setStatus(category.getStatus());
                }
            }
            model.addAttribute("form", form);
        }
        model.addAttribute("categories", catalogService.listAllCategories());
        model.addAttribute("editId", editId);
        model.addAttribute("countMap", catalogService.categoryServiceCount());
        return "admin/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@Valid @ModelAttribute("form") CategoryForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", catalogService.listAllCategories());
            model.addAttribute("countMap", catalogService.categoryServiceCount());
            return "admin/categories";
        }
        catalogService.saveCategory(form);
        redirectAttributes.addFlashAttribute("success", form.getId() == null ? "分类已新增" : "分类已更新");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/toggle")
    public String toggleCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            catalogService.toggleCategory(id);
            redirectAttributes.addFlashAttribute("success", "分类状态已更新");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            catalogService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "分类已删除");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ==================== 服务项目 ====================

    @GetMapping("/services")
    public String services(@RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Integer status,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        model.addAttribute("page", catalogService.pageAllServices(categoryId, keyword, status, page, 10));
        model.addAttribute("categories", catalogService.listAllCategories());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("categoryNameMap", catalogService.categoryNameMap());
        return "admin/services";
    }

    @GetMapping("/services/new")
    public String newService(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ServiceItemForm());
        }
        model.addAttribute("categories", catalogService.listEnabledCategories());
        model.addAttribute("editing", false);
        return "admin/service-form";
    }

    @GetMapping("/services/{id}/edit")
    public String editService(@PathVariable Long id, Model model) {
        ServiceItem item = catalogService.getService(id);
        if (item == null) {
            throw new BusinessException("服务不存在");
        }
        if (!model.containsAttribute("form")) {
            ServiceItemForm form = new ServiceItemForm();
            form.setId(item.getId());
            form.setCategoryId(item.getCategoryId());
            form.setName(item.getName());
            form.setSubtitle(item.getSubtitle());
            form.setPrice(item.getPrice());
            form.setPriceUnit(item.getPriceUnit());
            form.setDuration(item.getDuration());
            form.setCover(item.getCover());
            form.setDescription(item.getDescription());
            form.setStatus(item.getStatus());
            model.addAttribute("form", form);
        }
        model.addAttribute("categories", catalogService.listAllCategories());
        model.addAttribute("editing", true);
        return "admin/service-form";
    }

    @PostMapping("/services/save")
    public String saveService(@Valid @ModelAttribute("form") ServiceItemForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", catalogService.listAllCategories());
            model.addAttribute("editing", form.getId() != null);
            return "admin/service-form";
        }
        catalogService.saveService(form);
        redirectAttributes.addFlashAttribute("success", form.getId() == null ? "服务已上架" : "服务已更新");
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/toggle")
    public String toggleService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            catalogService.toggleService(id);
            redirectAttributes.addFlashAttribute("success", "上下架状态已更新");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/services";
    }
}
