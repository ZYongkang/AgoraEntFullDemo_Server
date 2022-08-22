package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class RoomNotFoundException extends VoiceRoomException {

    public RoomNotFoundException(String message) {
        super(ErrorCodeConstant.roomNotFound, message, HttpStatus.NOT_FOUND);
    }
}
