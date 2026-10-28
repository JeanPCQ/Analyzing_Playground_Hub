package com.stockpilot.wms.web;

import com.stockpilot.wms.service.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * When a screen action breaks a rule, send the user back to the page they came from
 * with the message shown at the top, instead of an error page.
 */
@ControllerAdvice(basePackages = "com.stockpilot.wms.web")
public class WebExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + backTo(request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public String handleBadInput(Exception ex, HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", "Please fill in every field with a valid value.");
        return "redirect:" + backTo(request);
    }

    private static String backTo(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return (referer == null || referer.isBlank()) ? "/" : referer;
    }
}
