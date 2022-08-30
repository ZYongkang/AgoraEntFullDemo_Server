package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;

public class RoomNotFoundException extends VoiceRoomException {

    public RoomNotFoundException(String message) {
        super(ErrorCodeConstant.roomNotFound, message);
    }
}
