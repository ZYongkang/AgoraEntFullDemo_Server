package com.md.mic.common.constants;

import org.apache.commons.lang.StringUtils;

/**
 * Created by liyongxin on 2022/8/31
 */

public enum CustomEventType {

    APPLY_SITE("chatroom_applySiteNotify"),
    INVITE_SITE("chatroom_inviteSiteNotify");

    private String eventType;

    CustomEventType(String type) {
        this.eventType = type;
    }

    @Override
    public String toString() {
        return this.eventType;
    }

    public String getValue() {
        return this.eventType;
    }

    public static CustomEventType parse(String eventType) {
        if (StringUtils.isEmpty(eventType)) {
            return null;
        }
        for (CustomEventType type : values()) {
            if (type.toString().equalsIgnoreCase(eventType)) {
                return type;
            }
        }
        return null;
    }

}
