package com.md.mic.exception;

import com.md.mic.common.constants.ErrorCodeConstant;

public class GiftNotFoundException extends VoiceRoomException {

    public GiftNotFoundException(String message) {
        super(ErrorCodeConstant.giftNotFound, message);
    }
}
