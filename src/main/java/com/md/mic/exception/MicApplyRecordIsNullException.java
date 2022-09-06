package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;

public class MicApplyRecordIsNullException extends VoiceRoomException {

    public MicApplyRecordIsNullException() {
        super(ErrorCodeConstant.micApplyRecordIsNullError, "mic apply record is null");
    }
}
