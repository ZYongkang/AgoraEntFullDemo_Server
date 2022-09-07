package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class MicApplyException extends VoiceRoomException {

    public MicApplyException() {
        super(ErrorCodeConstant.micApplyError, "addMicApply error", HttpStatus.BAD_REQUEST);
    }
}
