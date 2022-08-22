package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class UserNotInRoomException extends VoiceRoomException {

    public UserNotInRoomException() {
        super(ErrorCodeConstant.userNotInRoomError, "user not in voice room", HttpStatus.FORBIDDEN);
    }
}
