package com.community.homeservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryForm {

    private Long id;

    @NotBlank(message = "请填写分类名称")
    @Size(max = 50, message = "分类名称过长")
    private String name;

    @Size(max = 10, message = "图标过长")
    private String icon;

    @Size(max = 200, message = "描述过长")
    private String description;

    @NotNull(message = "请填写排序号")
    @Min(value = 0, message = "排序号不能为负")
    private Integer sortNo = 0;

    private Integer status = 1;
}
