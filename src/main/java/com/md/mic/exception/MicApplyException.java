package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;

public class MicApplyException extends VoiceRoomException {

    public MicApplyException() {
        super(ErrorCodeConstant.micApplyError, "addMicApply error");
    }
}
