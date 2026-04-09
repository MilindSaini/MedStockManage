package com.medstock.controller;

import com.medstock.dto.ai.AiAutofillResponse;
import com.medstock.service.AiAutofillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAutofillService aiAutofillService;

    @PostMapping(value = "/autofill", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiAutofillResponse> autofill(
        @RequestParam("image") MultipartFile image,
        Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is required");
        }

        try {
            AiAutofillResponse response = aiAutofillService.autofill(image.getBytes(), image.getContentType());
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not process image");
        }
    }
}
