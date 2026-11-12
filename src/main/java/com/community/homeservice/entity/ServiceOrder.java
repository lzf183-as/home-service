package com.community.homeservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("service_order")
public class ServiceOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long serviceId;

    private Long staffId;

    private String serviceName;

    private BigDecimal servicePrice;

    private Integer quantity;

    private BigDecimal amount;

    private LocalDate appointmentDate;

    private String appointmentTime;

    private String contactName;

    private String contactPhone;

    private String address;

    private String remark;

    /** 见 OrderStatus */
    private String status;

    private String cancelReason;

    private LocalDateTime createdAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime cancelledAt;
}
