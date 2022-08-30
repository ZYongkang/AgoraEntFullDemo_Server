package com.md.mic.exception;

public class VoiceRoomException extends RuntimeException {

    private String code;

    private String message;

    public VoiceRoomException(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public VoiceRoomException(String message, String code, String message1) {
        super(message);
        this.code = code;
        this.message = message1;
    }

    public VoiceRoomException(String message, Throwable cause, String code, String message1) {
        super(message, cause);
        this.code = code;
        this.message = message1;
    }

    public VoiceRoomException(Throwable cause, String code, String message) {
        super(cause);
        this.code = code;
        this.message = message;
    }

    public VoiceRoomException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace, String code, String message1) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
        this.message = message1;
    }
}
