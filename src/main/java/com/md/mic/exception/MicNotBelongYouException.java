package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class MicNotBelongYouException extends VoiceRoomException {

    public MicNotBelongYouException() {
        super(ErrorCodeConstant.micNotBelongYouError, "mic index not belong you",
                HttpStatus.BAD_REQUEST);
    }
}
