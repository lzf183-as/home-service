package com.community.homeservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.homeservice.common.BusinessException;
import com.community.homeservice.dto.BookingForm;
import com.community.homeservice.dto.ReviewForm;
import com.community.homeservice.entity.CustomerAddress;
import com.community.homeservice.entity.OrderReview;
import com.community.homeservice.entity.OrderStatusLog;
import com.community.homeservice.entity.ServiceItem;
import com.community.homeservice.entity.ServiceOrder;
import com.community.homeservice.entity.Staff;
import com.community.homeservice.enums.OrderStatus;
import com.community.homeservice.mapper.CustomerAddressMapper;
import com.community.homeservice.mapper.OrderReviewMapper;
import com.community.homeservice.mapper.OrderStatusLogMapper;
import com.community.homeservice.mapper.ServiceItemMapper;
import com.community.homeservice.mapper.ServiceOrderMapper;
import com.community.homeservice.mapper.StaffMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 预约订单：下单、状态流转、评价。
 * 状态流转全部采用「带前置状态的条件更新」，避免并发下重复接单/重复确认。
 */
@Service
public class OrderService {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ServiceOrderMapper orderMapper;
    private final ServiceItemMapper itemMapper;
    private final StaffMapper staffMapper;
    private final OrderReviewMapper reviewMapper;
    private final OrderStatusLogMapper logMapper;
    private final CustomerAddressMapper addressMapper;
    private final ServiceCatalogService catalogService;

    public OrderService(ServiceOrderMapper orderMapper,
                        ServiceItemMapper itemMapper,
                        StaffMapper staffMapper,
                        OrderReviewMapper reviewMapper,
                        OrderStatusLogMapper logMapper,
                        CustomerAddressMapper addressMapper,
                        ServiceCatalogService catalogService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.staffMapper = staffMapper;
        this.reviewMapper = reviewMapper;
        this.logMapper = logMapper;
        this.addressMapper = addressMapper;
        this.catalogService = catalogService;
    }

    // ==================== 下单 ====================

    @Transactional
    public ServiceOrder create(Long userId, BookingForm form) {
        ServiceItem item = catalogService.getEnabledService(form.getServiceId());

        LocalDate date = form.getAppointmentDate();
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException("预约日期不能早于今天");
        }
        if (date.isAfter(LocalDate.now().plusDays(60))) {
            throw new BusinessException("最多支持提前 60 天预约");
        }

        int quantity = form.getQuantity() == null ? 1 : form.getQuantity();

        String contactName = form.getContactName();
        String contactPhone = form.getContactPhone();
        String address = form.fullAddress();

        // 选择了地址簿则以后台数据为准，避免前端篡改
        if (form.getAddressId() != null) {
            CustomerAddress addr = addressMapper.selectById(form.getAddressId());
            if (addr != null && addr.getUserId().equals(userId)) {
                contactName = addr.getContactName();
                contactPhone = addr.getContactPhone();
                address = addr.fullAddress();
            }
        }

        ServiceOrder order = new ServiceOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setServiceId(item.getId());
        order.setServiceName(item.getName());
        order.setServicePrice(item.getPrice());
        order.setQuantity(quantity);
        order.setAmount(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setAppointmentDate(date);
        order.setAppointmentTime(form.getAppointmentTime());
        order.setContactName(contactName);
        order.setContactPhone(contactPhone);
        order.setAddress(address);
        order.setRemark(form.getRemark());
        order.setStatus(OrderStatus.PENDING_ACCEPT.name());
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        writeLog(order.getId(), null, order.getStatus(), "用户", "提交预约");
        return order;
    }

    // ==================== 查询 ====================

    public Page<ServiceOrder> pageUserOrders(Long userId, String status, int page, int size) {
        return orderMapper.selectPage(new Page<>(Math.max(1, page), Math.max(1, size)),
                new LambdaQueryWrapper<ServiceOrder>()
                        .eq(ServiceOrder::getUserId, userId)
                        .eq(StringUtils.hasText(status), ServiceOrder::getStatus, status)
                        .orderByDesc(ServiceOrder::getId));
    }

    public ServiceOrder getUserOrder(Long orderId, Long userId) {
        ServiceOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw BusinessException.notFound("订单不存在");
        }
        return order;
    }

    public ServiceOrder getOrder(Long orderId) {
        ServiceOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        return order;
    }

    public Page<ServiceOrder> pageAvailableOrders(int page, int size) {
        return orderMapper.selectPage(new Page<>(Math.max(1, page), Math.max(1, size)),
                new LambdaQueryWrapper<ServiceOrder>()
                        .eq(ServiceOrder::getStatus, OrderStatus.PENDING_ACCEPT.name())
                        .orderByDesc(ServiceOrder::getId));
    }

    public Page<ServiceOrder> pageStaffOrders(Long staffId, String status, int page, int size) {
        return orderMapper.selectPage(new Page<>(Math.max(1, page), Math.max(1, size)),
                new LambdaQueryWrapper<ServiceOrder>()
                        .eq(ServiceOrder::getStaffId, staffId)
                        .eq(StringUtils.hasText(status), ServiceOrder::getStatus, status)
                        .orderByDesc(ServiceOrder::getId));
    }

    public Page<ServiceOrder> pageAllOrders(String status, String keyword, int page, int size) {
        return orderMapper.selectPage(new Page<>(Math.max(1, page), Math.max(1, size)),
                new LambdaQueryWrapper<ServiceOrder>()
                        .eq(StringUtils.hasText(status), ServiceOrder::getStatus, status)
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(ServiceOrder::getOrderNo, keyword)
                                .or().like(ServiceOrder::getServiceName, keyword)
                                .or().like(ServiceOrder::getContactName, keyword)
                                .or().like(ServiceOrder::getContactPhone, keyword))
                        .orderByDesc(ServiceOrder::getId));
    }

    public List<OrderStatusLog> listLogs(Long orderId) {
        return logMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getId));
    }

    public OrderReview getReview(Long orderId) {
        return reviewMapper.selectOne(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getOrderId, orderId));
    }

    public List<OrderReview> listReviewsByService(Long serviceId) {
        return reviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getServiceId, serviceId)
                .orderByDesc(OrderReview::getId)
                .last("limit 8"));
    }

    public long countUserOrders(Long userId) {
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getUserId, userId));
        return count == null ? 0 : count;
    }

    public long countUserCompleted(Long userId) {
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getUserId, userId)
                .eq(ServiceOrder::getStatus, OrderStatus.COMPLETED.name()));
        return count == null ? 0 : count;
    }

    public long countStaffOrders(Long staffId, String status) {
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getStaffId, staffId)
                .eq(status != null, ServiceOrder::getStatus, status));
        return count == null ? 0 : count;
    }

    // ==================== 状态流转 ====================

    @Transactional
    public void cancel(Long orderId, Long userId, String reason, String operator) {
        ServiceOrder order = getUserOrder(orderId, userId);
        if (!OrderStatus.canCancel(order.getStatus())) {
            throw new BusinessException("当前订单状态不支持取消，请联系客服");
        }
        String from = order.getStatus();
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStatus, from)
                .set(ServiceOrder::getStatus, OrderStatus.CANCELLED.name())
                .set(ServiceOrder::getCancelledAt, LocalDateTime.now())
                .set(ServiceOrder::getCancelReason, StringUtils.hasText(reason) ? reason : "用户主动取消"));
        if (rows == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        writeLog(orderId, from, OrderStatus.CANCELLED.name(), operator, "取消订单");
    }

    /** 服务人员抢单 */
    @Transactional
    public void accept(Long orderId, Long staffId, String operator) {
        ServiceOrder order = getOrder(orderId);
        if (!OrderStatus.PENDING_ACCEPT.name().equals(order.getStatus())) {
            throw new BusinessException("该订单已被接单或已取消");
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStatus, OrderStatus.PENDING_ACCEPT.name())
                .isNull(ServiceOrder::getStaffId)
                .set(ServiceOrder::getStaffId, staffId)
                .set(ServiceOrder::getStatus, OrderStatus.ACCEPTED.name())
                .set(ServiceOrder::getAcceptedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException("手慢了，订单已被其他服务人员接走");
        }
        writeLog(orderId, OrderStatus.PENDING_ACCEPT.name(), OrderStatus.ACCEPTED.name(), operator, "服务人员接单");
    }

    /** 开始服务 */
    @Transactional
    public void start(Long orderId, Long staffId, String operator) {
        ServiceOrder order = requireStaffOrder(orderId, staffId);
        if (!OrderStatus.ACCEPTED.name().equals(order.getStatus())) {
            throw new BusinessException("只有「已接单」的订单可以开始服务");
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStaffId, staffId)
                .eq(ServiceOrder::getStatus, OrderStatus.ACCEPTED.name())
                .set(ServiceOrder::getStatus, OrderStatus.SERVING.name())
                .set(ServiceOrder::getStartedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        writeLog(orderId, OrderStatus.ACCEPTED.name(), OrderStatus.SERVING.name(), operator, "开始服务");
    }

    /** 服务完成，等待用户确认 */
    @Transactional
    public void finish(Long orderId, Long staffId, String operator) {
        ServiceOrder order = requireStaffOrder(orderId, staffId);
        if (!OrderStatus.SERVING.name().equals(order.getStatus())) {
            throw new BusinessException("只有「服务中」的订单可以提交完工");
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStaffId, staffId)
                .eq(ServiceOrder::getStatus, OrderStatus.SERVING.name())
                .set(ServiceOrder::getStatus, OrderStatus.PENDING_CONFIRM.name())
                .set(ServiceOrder::getFinishedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        writeLog(orderId, OrderStatus.SERVING.name(), OrderStatus.PENDING_CONFIRM.name(), operator, "服务人员提交完工");
    }

    /** 用户确认完成，累计销量与接单数 */
    @Transactional
    public void confirm(Long orderId, Long userId, String operator) {
        ServiceOrder order = getUserOrder(orderId, userId);
        if (!OrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())) {
            throw new BusinessException("当前订单还不能确认完成");
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getUserId, userId)
                .eq(ServiceOrder::getStatus, OrderStatus.PENDING_CONFIRM.name())
                .set(ServiceOrder::getStatus, OrderStatus.COMPLETED.name())
                .set(ServiceOrder::getFinishedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }

        itemMapper.update(null, new LambdaUpdateWrapper<ServiceItem>()
                .eq(ServiceItem::getId, order.getServiceId())
                .setSql("sales = sales + 1"));
        if (order.getStaffId() != null) {
            staffMapper.update(null, new LambdaUpdateWrapper<Staff>()
                    .eq(Staff::getId, order.getStaffId())
                    .setSql("order_count = order_count + 1"));
        }
        writeLog(orderId, OrderStatus.PENDING_CONFIRM.name(), OrderStatus.COMPLETED.name(), operator, "用户确认完成");
    }

    /** 管理员派单/改派 */
    @Transactional
    public void assign(Long orderId, Long staffId, String operator) {
        ServiceOrder order = getOrder(orderId);
        if (OrderStatus.COMPLETED.name().equals(order.getStatus())
                || OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new BusinessException("已完成或已取消的订单不能派单");
        }
        Staff staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw new BusinessException("服务人员不存在");
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, orderId)
                .in(ServiceOrder::getStatus, List.of(OrderStatus.PENDING_ACCEPT.name(), OrderStatus.ACCEPTED.name()))
                .set(ServiceOrder::getStaffId, staffId)
                .set(ServiceOrder::getStatus, OrderStatus.ACCEPTED.name())
                .set(ServiceOrder::getAcceptedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException("当前状态不支持派单");
        }
        writeLog(orderId, order.getStatus(), OrderStatus.ACCEPTED.name(), operator, "管理员派单");
    }

    /** 管理员强制取消 */
    @Transactional
    public void adminCancel(Long orderId, String reason, String operator) {
        ServiceOrder order = getOrder(orderId);
        if (OrderStatus.COMPLETED.name().equals(order.getStatus())
                || OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new BusinessException("该订单已结束，无法取消");
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStatus, order.getStatus())
                .set(ServiceOrder::getStatus, OrderStatus.CANCELLED.name())
                .set(ServiceOrder::getCancelledAt, LocalDateTime.now())
                .set(ServiceOrder::getCancelReason, StringUtils.hasText(reason) ? reason : "管理员取消"));
        if (rows == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        writeLog(orderId, order.getStatus(), OrderStatus.CANCELLED.name(), operator, "管理员取消订单");
    }

    // ==================== 评价 ====================

    @Transactional
    public void review(Long userId, ReviewForm form, String operator) {
        ServiceOrder order = getUserOrder(form.getOrderId(), userId);
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BusinessException("服务完成后才能评价");
        }
        if (getReview(order.getId()) != null) {
            throw new BusinessException("该订单已评价过了");
        }

        OrderReview review = new OrderReview();
        review.setOrderId(order.getId());
        review.setUserId(userId);
        review.setStaffId(order.getStaffId());
        review.setServiceId(order.getServiceId());
        review.setRating(form.getRating());
        review.setContent(form.getContent());
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);

        refreshServiceRating(order.getServiceId());
        if (order.getStaffId() != null) {
            refreshStaffRating(order.getStaffId());
        }
        writeLog(order.getId(), order.getStatus(), order.getStatus(), operator,
                "用户评价 " + form.getRating() + " 星");
    }

    // ==================== 内部方法 ====================

    private ServiceOrder requireStaffOrder(Long orderId, Long staffId) {
        ServiceOrder order = getOrder(orderId);
        if (!staffId.equals(order.getStaffId())) {
            throw BusinessException.forbidden("这不是您的工单");
        }
        return order;
    }

    private void writeLog(Long orderId, String from, String to, String operator, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperator(operator);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }

    private void refreshServiceRating(Long serviceId) {
        List<OrderReview> list = reviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getServiceId, serviceId));
        if (list.isEmpty()) {
            return;
        }
        double avg = list.stream().mapToInt(OrderReview::getRating).average().orElse(5.0);
        itemMapper.update(null, new LambdaUpdateWrapper<ServiceItem>()
                .eq(ServiceItem::getId, serviceId)
                .set(ServiceItem::getRating, BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP)));
    }

    private void refreshStaffRating(Long staffId) {
        List<OrderReview> list = reviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getStaffId, staffId));
        if (list.isEmpty()) {
            return;
        }
        double avg = list.stream().mapToInt(OrderReview::getRating).average().orElse(5.0);
        staffMapper.update(null, new LambdaUpdateWrapper<Staff>()
                .eq(Staff::getId, staffId)
                .set(Staff::getRating, BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP)));
    }

    private String generateOrderNo() {
        return "HS" + NO_FORMAT.format(LocalDateTime.now())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
