package com.community.homeservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressForm {

    private Long id;

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

    private Boolean setDefault = false;
}
