package com.community.homeservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class BookingForm {

    @NotNull(message = "请选择服务")
    private Long serviceId;

    @NotNull(message = "请选择预约日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate appointmentDate;

    @NotBlank(message = "请选择预约时段")
    private String appointmentTime;

    /** 选择已有地址簿时传 id，否则使用下方手填信息 */
    private Long addressId;

    @NotBlank(message = "请填写联系人")
    @Size(max = 20, message = "联系人姓名过长")
    private String contactName;

    @NotBlank(message = "请填写联系电话")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    @NotBlank(message = "请填写所在小区/街道")
    @Size(max = 100, message = "地址过长")
    private String region;

    @NotBlank(message = "请填写门牌号等详细地址")
    @Size(max = 200, message = "地址过长")
    private String detail;

    @Size(max = 500, message = "备注最多 500 字")
    private String remark;

    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 20, message = "单次最多预约 20 份")
    private Integer quantity = 1;

    public String fullAddress() {
        return (region == null ? "" : region) + " " + (detail == null ? "" : detail);
    }
}
