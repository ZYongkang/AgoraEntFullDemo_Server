package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class MicIndexNullException extends VoiceRoomException {

    public MicIndexNullException() {
        super(ErrorCodeConstant.micIndexNullError, "mic index is null", HttpStatus.BAD_REQUEST);
    }
}
