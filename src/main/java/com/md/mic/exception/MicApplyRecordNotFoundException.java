package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class MicApplyRecordNotFoundException extends VoiceRoomException {

    public MicApplyRecordNotFoundException() {
        super(ErrorCodeConstant.micApplyRecordNotFoundError, "mic apply record not found",
                HttpStatus.NOT_FOUND);
    }
}
