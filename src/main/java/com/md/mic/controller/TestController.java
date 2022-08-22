package com.md.mic.controller;

import com.md.service.model.entity.Users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@RequestMapping("/voice")
public class TestController {

    @GetMapping("/test")
    public String test(@RequestAttribute(value = "user", required = false) @ApiIgnore Users user) {
        if (user == null) {
            return "no user";
        }
        return user.toString();
    }


}
