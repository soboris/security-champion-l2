package com.cybersecurity.sechamp2025.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class VoucherController {

    @GetMapping("/voucher")
    public String voucherPage() {
        return "voucher"; // This will resolve to templates/voucher.html
    }
}