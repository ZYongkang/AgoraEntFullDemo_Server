package com.md.mic.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@Configuration
@RestControllerAdvice
public class VoiceRoomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({VoiceRoomException.class})
    public ResponseEntity<Object> giftNotFoundExceptionHandler(VoiceRoomException exception,
            WebRequest request) {
        if (log.isInfoEnabled()) {
            log.info("[BizException]业务异常信息 ex={}", exception.getMessage(), exception);
        }
        HttpHeaders headers = new HttpHeaders();
        ExceptionResult response =
                new ExceptionResult(exception.getCode(), exception.getMessage());
        return handleExceptionInternal(exception, response, headers, exception.getHttpStatus(),
                request);
    }


}
