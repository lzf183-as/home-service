package com.community.homeservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceItemForm {

    private Long id;

    @NotNull(message = "请选择所属分类")
    private Long categoryId;

    @NotBlank(message = "请填写服务名称")
    @Size(max = 80, message = "服务名称过长")
    private String name;

    @Size(max = 150, message = "副标题过长")
    private String subtitle;

    @NotNull(message = "请填写价格")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    @NotBlank(message = "请填写计价单位")
    private String priceUnit = "次";

    @NotNull(message = "请填写服务时长")
    @Min(value = 30, message = "服务时长至少 30 分钟")
    private Integer duration = 120;

    @Size(max = 10, message = "封面表情过长")
    private String cover;

    @Size(max = 2000, message = "服务介绍最多 2000 字")
    private String description;

    private Integer status = 1;
}
