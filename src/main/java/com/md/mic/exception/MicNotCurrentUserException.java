package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;

public class MicNotCurrentUserException extends VoiceRoomException {

    public MicNotCurrentUserException() {
        super(ErrorCodeConstant.micNotCurrentUser, "mic not current user");
    }
}
