package com.community.homeservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("service_item")
public class ServiceItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String name;

    private String subtitle;

    private BigDecimal price;

    /** 计价单位：次 / 小时 / 月 */
    private String priceUnit;

    /** 服务时长（分钟） */
    private Integer duration;

    /** 封面表情，前端用渐变色块渲染 */
    private String cover;

    private String description;

    private Integer sales;

    private BigDecimal rating;

    /** 1 上架 0 下架 */
    private Integer status;

    private LocalDateTime createdAt;

    /** 去掉多余小数零，用于页面展示价格 */
    public String getPriceText() {
        if (price == null) {
            return "0";
        }
        return price.stripTrailingZeros().toPlainString();
    }
}
