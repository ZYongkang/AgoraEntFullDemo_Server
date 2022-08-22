package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class VoiceRoomTypeMismatchException extends VoiceRoomException {

    public VoiceRoomTypeMismatchException(String message) {
        super(ErrorCodeConstant.voiceRoomTypeMismatch, message, HttpStatus.BAD_REQUEST);
    }
}
