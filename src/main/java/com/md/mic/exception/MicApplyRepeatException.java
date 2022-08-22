package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class MicApplyRepeatException extends VoiceRoomException {

    public MicApplyRepeatException() {
        super(ErrorCodeConstant.micRepeatApplyError, "addMicApply repeat error", HttpStatus.BAD_REQUEST);
    }
}
