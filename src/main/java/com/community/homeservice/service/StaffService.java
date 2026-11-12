package com.community.homeservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.homeservice.common.BusinessException;
import com.community.homeservice.dto.StaffForm;
import com.community.homeservice.entity.Staff;
import com.community.homeservice.entity.SysUser;
import com.community.homeservice.mapper.StaffMapper;
import com.community.homeservice.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StaffService {

    private final StaffMapper staffMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public StaffService(StaffMapper staffMapper, SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.staffMapper = staffMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 服务人员视图：档案 + 账号信息 */
    public record StaffView(Staff staff, String realName, String phone, String username) {
    }

    public Staff getByUserId(Long userId) {
        Staff staff = staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getUserId, userId));
        if (staff == null) {
            throw new BusinessException("当前账号未关联服务人员档案，请联系管理员");
        }
        return staff;
    }

    public List<Staff> listAll() {
        return staffMapper.selectList(new LambdaQueryWrapper<Staff>()
                .orderByDesc(Staff::getAvailable)
                .orderByDesc(Staff::getRating));
    }

    public Page<Staff> pageStaff(int page, int size) {
        return staffMapper.selectPage(new Page<>(Math.max(1, page), Math.max(1, size)),
                new LambdaQueryWrapper<Staff>().orderByDesc(Staff::getId));
    }

    public List<StaffView> listViews() {
        List<Staff> staffs = listAll();
        Map<Long, SysUser> users = new LinkedHashMap<>();
        if (!staffs.isEmpty()) {
            List<Long> ids = staffs.stream().map(Staff::getUserId).toList();
            for (SysUser u : userMapper.selectBatchIds(ids)) {
                users.put(u.getId(), u);
            }
        }
        List<StaffView> views = new ArrayList<>();
        for (Staff s : staffs) {
            SysUser u = users.get(s.getUserId());
            views.add(new StaffView(s,
                    u == null ? "-" : u.getRealName(),
                    u == null ? "-" : u.getPhone(),
                    u == null ? "-" : u.getUsername()));
        }
        return views;
    }

    public Map<Long, String> staffNameMap() {
        Map<Long, String> map = new LinkedHashMap<>();
        for (StaffView v : listViews()) {
            map.put(v.staff().getId(), v.realName());
        }
        return map;
    }

    @Transactional
    public void createStaff(StaffForm form) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, form.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("登录账号已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(form.getUsername());
        user.setPassword(passwordEncoder.encode(
                StringUtils.hasText(form.getPassword()) ? form.getPassword() : "123456789"));
        user.setRealName(form.getRealName());
        user.setPhone(form.getPhone());
        user.setRole("STAFF");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);

        Staff staff = new Staff();
        staff.setUserId(user.getId());
        staff.setJobType(form.getJobType());
        staff.setStaffLevel(StringUtils.hasText(form.getStaffLevel()) ? form.getStaffLevel() : "初级");
        staff.setRating(BigDecimal.valueOf(5.0));
        staff.setOrderCount(0);
        staff.setAvailable(1);
        staff.setIntro(form.getIntro());
        staff.setCreatedAt(LocalDateTime.now());
        staffMapper.insert(staff);
    }

    public void updateStaff(Long staffId, String jobType, String staffLevel, String intro) {
        staffMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Staff>()
                .eq(Staff::getId, staffId)
                .set(Staff::getJobType, jobType)
                .set(Staff::getStaffLevel, staffLevel)
                .set(Staff::getIntro, intro));
    }

    public void toggleAvailable(Long staffId, int available) {
        staffMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Staff>()
                .eq(Staff::getId, staffId)
                .set(Staff::getAvailable, available));
    }

    public String jobTypeOf(Long staffId) {
        Staff staff = staffMapper.selectById(staffId);
        return staff == null ? "-" : staff.getJobType();
    }
}
