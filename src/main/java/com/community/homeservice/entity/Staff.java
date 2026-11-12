package com.community.homeservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("staff")
public class Staff {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 保洁 / 家电清洗 / 母婴护理 / 维修 / 养老陪护 */
    private String jobType;

    private String staffLevel;

    private BigDecimal rating;

    private Integer orderCount;

    /** 1 空闲可接单 0 忙碌 */
    private Integer available;

    private String intro;

    private LocalDateTime createdAt;
}
