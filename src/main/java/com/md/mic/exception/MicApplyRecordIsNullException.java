package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class MicApplyRecordIsNullException extends VoiceRoomException {

    public MicApplyRecordIsNullException() {
        super(ErrorCodeConstant.micApplyRecordIsNullError, "mic apply record is null",
                HttpStatus.BAD_REQUEST);
    }
}
