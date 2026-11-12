package com.community.homeservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.homeservice.common.BusinessException;
import com.community.homeservice.dto.AddressForm;
import com.community.homeservice.dto.ProfileForm;
import com.community.homeservice.dto.RegisterForm;
import com.community.homeservice.entity.CustomerAddress;
import com.community.homeservice.entity.SysUser;
import com.community.homeservice.mapper.CustomerAddressMapper;
import com.community.homeservice.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final SysUserMapper userMapper;
    private final CustomerAddressMapper addressMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserMapper userMapper,
                       CustomerAddressMapper addressMapper,
                       PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== 账号 ====================

    @Transactional
    public SysUser register(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, form.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已被注册，请换一个");
        }

        SysUser user = new SysUser();
        user.setUsername(form.getUsername());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRealName(form.getRealName());
        user.setPhone(form.getPhone());
        user.setRole("USER");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    public SysUser getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public void updateProfile(Long userId, ProfileForm form) {
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getRealName, form.getRealName())
                .set(SysUser::getPhone, form.getPhone())
                .set(SysUser::getEmail, form.getEmail()));
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码至少 6 位");
        }
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getPassword, passwordEncoder.encode(newPassword)));
    }

    public Page<SysUser> pageUsers(String keyword, int page, int size) {
        return userMapper.selectPage(new Page<>(Math.max(1, page), Math.max(1, size)),
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "USER")
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getRealName, keyword)
                                .or().like(SysUser::getPhone, keyword))
                        .orderByDesc(SysUser::getId));
    }

    public void toggleStatus(Long userId, int status) {
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getStatus, status));
    }

    // ==================== 地址簿 ====================

    public List<CustomerAddress> listAddresses(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<CustomerAddress>()
                .eq(CustomerAddress::getUserId, userId)
                .orderByDesc(CustomerAddress::getIsDefault)
                .orderByDesc(CustomerAddress::getId));
    }

    public CustomerAddress getDefaultAddress(Long userId) {
        List<CustomerAddress> list = listAddresses(userId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Transactional
    public void saveAddress(Long userId, AddressForm form) {
        boolean setDefault = Boolean.TRUE.equals(form.getSetDefault());
        boolean firstAddress = listAddresses(userId).isEmpty();

        if (setDefault || firstAddress) {
            addressMapper.update(null, new LambdaUpdateWrapper<CustomerAddress>()
                    .eq(CustomerAddress::getUserId, userId)
                    .set(CustomerAddress::getIsDefault, 0));
        }

        if (form.getId() != null) {
            CustomerAddress exist = addressMapper.selectById(form.getId());
            if (exist == null || !exist.getUserId().equals(userId)) {
                throw new BusinessException("地址不存在");
            }
            exist.setContactName(form.getContactName());
            exist.setContactPhone(form.getContactPhone());
            exist.setRegion(form.getRegion());
            exist.setDetail(form.getDetail());
            if (setDefault || firstAddress) {
                exist.setIsDefault(1);
            }
            addressMapper.updateById(exist);
        } else {
            CustomerAddress addr = new CustomerAddress();
            addr.setUserId(userId);
            addr.setContactName(form.getContactName());
            addr.setContactPhone(form.getContactPhone());
            addr.setRegion(form.getRegion());
            addr.setDetail(form.getDetail());
            addr.setIsDefault((setDefault || firstAddress) ? 1 : 0);
            addr.setCreatedAt(LocalDateTime.now());
            addressMapper.insert(addr);
        }
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        CustomerAddress addr = addressMapper.selectById(addressId);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        addressMapper.deleteById(addressId);
        // 若删除的是默认地址，把剩下的第一条设为默认
        List<CustomerAddress> rest = listAddresses(userId);
        if (!rest.isEmpty() && rest.stream().noneMatch(a -> a.getIsDefault() != null && a.getIsDefault() == 1)) {
            addressMapper.update(null, new LambdaUpdateWrapper<CustomerAddress>()
                    .eq(CustomerAddress::getId, rest.get(0).getId())
                    .set(CustomerAddress::getIsDefault, 1));
        }
    }

    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        CustomerAddress addr = addressMapper.selectById(addressId);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        addressMapper.update(null, new LambdaUpdateWrapper<CustomerAddress>()
                .eq(CustomerAddress::getUserId, userId)
                .set(CustomerAddress::getIsDefault, 0));
        addressMapper.update(null, new LambdaUpdateWrapper<CustomerAddress>()
                .eq(CustomerAddress::getId, addressId)
                .set(CustomerAddress::getIsDefault, 1));
    }
}
