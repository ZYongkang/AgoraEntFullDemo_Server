package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class MicIndexExceedLimitException extends VoiceRoomException {

    public MicIndexExceedLimitException(String message) {
        super(ErrorCodeConstant.micIndexExceedLimitError, message, HttpStatus.BAD_REQUEST);
    }
}
