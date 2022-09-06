package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;

public class MicStatusCannotBeModifiedException extends VoiceRoomException {

    public MicStatusCannotBeModifiedException() {
        super(ErrorCodeConstant.micStatusCannotBeModified, "mic current status cannot be modified");
    }
}
