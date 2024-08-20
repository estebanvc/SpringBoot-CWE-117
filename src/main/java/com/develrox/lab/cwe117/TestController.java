package com.develrox.lab.cwe117;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api")
@RestController
public class TestController {

    private final Logger log = LoggerFactory.getLogger(TestController.class);

    @PostMapping("/test")
    public ResponseEntity<?> persistData(@RequestBody UserInputDTO userInputDto) {
        log.info("REST request to save UserInputDTO {}", userInputDto);
        // log.info("REST request to save UserInputDTO {}", LogUtils.escapeLogInput(userInputDto.toString()));
        // log.info("REST request to save UserInputDTO {}", StringEscapeUtils.escapeJava(userInputDto.toString()));
        return ResponseEntity.ok(userInputDto);
    }
}
