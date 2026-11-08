package com.gregomebije.wallet.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.gregomebije.wallet.exception.Exceptions.*;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(DuplicateRequestException.class)
    ResponseEntity<?> duplicate(DuplicateRequestException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "error", "message", e.getMessage()));
    }
    @ExceptionHandler(InvalidWebhookSignatureException.class)
    ResponseEntity<?> signature(InvalidWebhookSignatureException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error"));
    }
    @ExceptionHandler({IllegalArgumentException.class})
    ResponseEntity<?> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
    }
}