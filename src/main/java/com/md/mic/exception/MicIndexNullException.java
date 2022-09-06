package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;

public class MicIndexNullException extends VoiceRoomException {

    public MicIndexNullException() {
        super(ErrorCodeConstant.micIndexNullError, "mic index is null");
    }
}
