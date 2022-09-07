package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class GiftNotFoundException extends VoiceRoomException {

    public GiftNotFoundException(String message) {
        super(ErrorCodeConstant.giftNotFound, message, HttpStatus.NOT_FOUND);
    }
}
