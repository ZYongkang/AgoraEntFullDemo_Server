package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;

public class MicInitException extends VoiceRoomException {

    public MicInitException() {
        super(ErrorCodeConstant.micInitError, "mic init error");
    }
}
