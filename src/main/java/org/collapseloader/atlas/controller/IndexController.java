package org.collapseloader.atlas.controller;

import org.collapseloader.atlas.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class IndexController {
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Integer>>> index() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", 200)));
    }
}
