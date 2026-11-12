package com.community.homeservice.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, Model model,
                                 HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(ex.getStatus());
        model.addAttribute("status", ex.getStatus());
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("backUrl", request.getHeader("Referer"));
        return "error/business";
    }
}
