package com.community.homeservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_address")
public class CustomerAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String contactName;

    private String contactPhone;

    /** 小区 / 街道 */
    private String region;

    /** 门牌号等详细地址 */
    private String detail;

    /** 1 默认地址 */
    private Integer isDefault;

    private LocalDateTime createdAt;

    public String fullAddress() {
        String r = region == null ? "" : region;
        String d = detail == null ? "" : detail;
        return r + " " + d;
    }
}
