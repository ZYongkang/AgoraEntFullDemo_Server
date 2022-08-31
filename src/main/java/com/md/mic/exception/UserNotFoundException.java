package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;

public class UserNotFoundException extends VoiceRoomException {

    public UserNotFoundException(String message) {
        super(ErrorCodeConstant.userNotFound, message);
    }
}
