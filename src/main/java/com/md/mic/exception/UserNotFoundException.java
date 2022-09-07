package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends VoiceRoomException {

    public UserNotFoundException(String message) {
        super(ErrorCodeConstant.userNotFound, message, HttpStatus.NOT_FOUND);
    }
}
