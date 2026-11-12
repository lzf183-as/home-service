package com.community.homeservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.homeservice.common.BusinessException;
import com.community.homeservice.dto.CategoryForm;
import com.community.homeservice.dto.ServiceItemForm;
import com.community.homeservice.entity.ServiceCategory;
import com.community.homeservice.entity.ServiceItem;
import com.community.homeservice.mapper.ServiceCategoryMapper;
import com.community.homeservice.mapper.ServiceItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务目录：分类与项目的读取。
 */
@Service
public class ServiceCatalogService {

    private final ServiceCategoryMapper categoryMapper;
    private final ServiceItemMapper itemMapper;

    public ServiceCatalogService(ServiceCategoryMapper categoryMapper, ServiceItemMapper itemMapper) {
        this.categoryMapper = categoryMapper;
        this.itemMapper = itemMapper;
    }

    public List<ServiceCategory> listEnabledCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ServiceCategory>()
                .eq(ServiceCategory::getStatus, 1)
                .orderByAsc(ServiceCategory::getSortNo)
                .orderByAsc(ServiceCategory::getId));
    }

    public List<ServiceCategory> listAllCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ServiceCategory>()
                .orderByAsc(ServiceCategory::getSortNo)
                .orderByAsc(ServiceCategory::getId));
    }

    public Map<Long, String> categoryNameMap() {
        Map<Long, String> map = new LinkedHashMap<>();
        for (ServiceCategory c : listAllCategories()) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    public Map<Long, Long> categoryServiceCount() {
        List<ServiceItem> items = itemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                .select(ServiceItem::getCategoryId));
        Map<Long, Long> map = new LinkedHashMap<>();
        for (ServiceItem item : items) {
            map.merge(item.getCategoryId(), 1L, Long::sum);
        }
        return map;
    }

    public List<ServiceItem> listHotServices(int limit) {
        return itemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getStatus, 1)
                .orderByDesc(ServiceItem::getSales)
                .last("limit " + Math.max(1, Math.min(limit, 20))));
    }

    public Page<ServiceItem> pageServices(Long categoryId, String keyword, int page, int size) {
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getStatus, 1)
                .eq(categoryId != null, ServiceItem::getCategoryId, categoryId)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(ServiceItem::getName, keyword)
                        .or()
                        .like(ServiceItem::getSubtitle, keyword))
                .orderByDesc(ServiceItem::getSales)
                .orderByAsc(ServiceItem::getId);
        return itemMapper.selectPage(new Page<>(Math.max(1, page), Math.max(1, size)), wrapper);
    }

    public ServiceItem getEnabledService(Long id) {
        ServiceItem item = itemMapper.selectById(id);
        if (item == null || item.getStatus() == null || item.getStatus() != 1) {
            throw BusinessException.notFound("该服务不存在或已下架");
        }
        return item;
    }

    public ServiceItem getService(Long id) {
        return itemMapper.selectById(id);
    }

    public ServiceCategory getCategory(Long id) {
        return categoryMapper.selectById(id);
    }

    // ==================== 管理端写操作 ====================

    public Page<ServiceItem> pageAllServices(Long categoryId, String keyword, Integer status, int page, int size) {
        return itemMapper.selectPage(new Page<>(Math.max(1, page), Math.max(1, size)),
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(categoryId != null, ServiceItem::getCategoryId, categoryId)
                        .eq(status != null, ServiceItem::getStatus, status)
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(ServiceItem::getName, keyword)
                                .or().like(ServiceItem::getSubtitle, keyword))
                        .orderByDesc(ServiceItem::getId));
    }

    @Transactional
    public void saveCategory(CategoryForm form) {
        ServiceCategory category = new ServiceCategory();
        category.setName(form.getName());
        category.setIcon(StringUtils.hasText(form.getIcon()) ? form.getIcon() : "🧹");
        category.setDescription(form.getDescription());
        category.setSortNo(form.getSortNo() == null ? 0 : form.getSortNo());
        category.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        if (form.getId() == null) {
            category.setCreatedAt(LocalDateTime.now());
            categoryMapper.insert(category);
        } else {
            category.setId(form.getId());
            categoryMapper.updateById(category);
        }
    }

    public void toggleCategory(Long id) {
        ServiceCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        int next = (category.getStatus() != null && category.getStatus() == 1) ? 0 : 1;
        categoryMapper.update(null, new LambdaUpdateWrapper<ServiceCategory>()
                .eq(ServiceCategory::getId, id)
                .set(ServiceCategory::getStatus, next));
    }

    public void deleteCategory(Long id) {
        Long count = itemMapper.selectCount(new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getCategoryId, id));
        if (count != null && count > 0) {
            throw new BusinessException("该分类下还有服务项目，请先移动或下架");
        }
        categoryMapper.deleteById(id);
    }

    @Transactional
    public void saveService(ServiceItemForm form) {
        ServiceCategory category = categoryMapper.selectById(form.getCategoryId());
        if (category == null) {
            throw BusinessException.notFound("所属分类不存在");
        }
        ServiceItem item = new ServiceItem();
        item.setCategoryId(form.getCategoryId());
        item.setName(form.getName());
        item.setSubtitle(form.getSubtitle());
        item.setPrice(form.getPrice());
        item.setPriceUnit(StringUtils.hasText(form.getPriceUnit()) ? form.getPriceUnit() : "次");
        item.setDuration(form.getDuration() == null ? 120 : form.getDuration());
        item.setCover(StringUtils.hasText(form.getCover()) ? form.getCover() : "🧽");
        item.setDescription(form.getDescription());
        item.setStatus(form.getStatus() == null ? 1 : form.getStatus());

        if (form.getId() == null) {
            item.setSales(0);
            item.setRating(BigDecimal.valueOf(5.0));
            item.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(item);
        } else {
            item.setId(form.getId());
            itemMapper.updateById(item);
        }
    }

    public void toggleService(Long id) {
        ServiceItem item = itemMapper.selectById(id);
        if (item == null) {
            throw BusinessException.notFound("服务不存在");
        }
        int next = (item.getStatus() != null && item.getStatus() == 1) ? 0 : 1;
        itemMapper.update(null, new LambdaUpdateWrapper<ServiceItem>()
                .eq(ServiceItem::getId, id)
                .set(ServiceItem::getStatus, next));
    }
}
