package com.community.homeservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewForm {

    /** 由 URL 路径决定，不参与表单校验 */
    private Long orderId;

    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分为 1-5 星")
    @Max(value = 5, message = "评分为 1-5 星")
    private Integer rating;

    @Size(max = 500, message = "评价最多 500 字")
    private String content;
}
