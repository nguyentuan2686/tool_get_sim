package org.example.demo2.controller;

import org.example.demo2.service.SimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sim")
public class SimController {

    @Autowired
    SimService service;

    @PostMapping
    public HttpStatus getSim(@RequestBody List<Integer> pointElement) {
        service.getAllSimWithHighPoint(pointElement);
        return HttpStatus.OK;
    }
}
