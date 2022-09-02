package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;

public class VoiceRoomSecurityException extends VoiceRoomException {

    public VoiceRoomSecurityException(String message) {
        super(ErrorCodeConstant.roomUnSupportedOperation, message);
    }
}
