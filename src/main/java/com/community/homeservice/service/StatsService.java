package com.community.homeservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.homeservice.entity.ServiceOrder;
import com.community.homeservice.entity.SysUser;
import com.community.homeservice.entity.Staff;
import com.community.homeservice.enums.OrderStatus;
import com.community.homeservice.mapper.ServiceOrderMapper;
import com.community.homeservice.mapper.StaffMapper;
import com.community.homeservice.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理后台数据看板统计。数据量较小，直接在内存聚合，避免 SQL 方言差异。
 */
@Service
public class StatsService {

    private final ServiceOrderMapper orderMapper;
    private final SysUserMapper userMapper;
    private final StaffMapper staffMapper;

    public StatsService(ServiceOrderMapper orderMapper, SysUserMapper userMapper, StaffMapper staffMapper) {
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.staffMapper = staffMapper;
    }

    public record Overview(long totalOrders,
                           long completedOrders,
                           long pendingOrders,
                           long todayOrders,
                           BigDecimal revenue,
                           long userCount,
                           long staffCount) {
    }

    public record DayPoint(String label, long count) {
    }

    public record ServiceRank(String name, long count) {
    }

    public Overview overview() {
        List<ServiceOrder> all = orderMapper.selectList(null);
        long total = all.size();
        long completed = all.stream().filter(o -> OrderStatus.COMPLETED.name().equals(o.getStatus())).count();
        long pending = all.stream().filter(o -> OrderStatus.PENDING_ACCEPT.name().equals(o.getStatus())).count();
        long today = all.stream().filter(o -> o.getCreatedAt() != null
                && o.getCreatedAt().toLocalDate().equals(LocalDate.now())).count();
        BigDecimal revenue = all.stream()
                .filter(o -> OrderStatus.COMPLETED.name().equals(o.getStatus()))
                .map(ServiceOrder::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long userCount = nz(userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "USER")));
        long staffCount = nz(staffMapper.selectCount(new LambdaQueryWrapper<Staff>()));

        return new Overview(total, completed, pending, today, revenue, userCount, staffCount);
    }

    public List<DayPoint> trendLast7Days() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        List<ServiceOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .ge(ServiceOrder::getCreatedAt, start.atStartOfDay()));

        Map<LocalDate, Long> counts = orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<DayPoint> points = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            points.add(new DayPoint((d.getMonthValue()) + "/" + d.getDayOfMonth(), counts.getOrDefault(d, 0L)));
        }
        return points;
    }

    public List<ServiceRank> topServices(int limit) {
        List<ServiceOrder> completed = orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getStatus, OrderStatus.COMPLETED.name()));
        Map<String, Long> counts = completed.stream()
                .collect(Collectors.groupingBy(ServiceOrder::getServiceName, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new ServiceRank(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public Map<String, Long> statusDistribution() {
        List<ServiceOrder> all = orderMapper.selectList(null);
        Map<String, Long> dist = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            dist.put(s.name(), 0L);
        }
        for (ServiceOrder o : all) {
            dist.merge(o.getStatus(), 1L, Long::sum);
        }
        return dist;
    }

    private long nz(Long v) {
        return v == null ? 0L : v;
    }
}
