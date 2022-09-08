package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class MicInitException extends VoiceRoomException {

    public MicInitException() {
        super(ErrorCodeConstant.micInitError, "mic init error", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
