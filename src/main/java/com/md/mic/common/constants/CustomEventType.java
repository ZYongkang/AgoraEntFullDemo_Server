package com.md.mic.common.constants;

import org.apache.commons.lang.StringUtils;

/**
 * Created by liyongxin on 2022/8/31
 */

public enum CustomEventType {

    /**
     * 申请上麦
     */
    APPLY_SITE("chatroom_applySiteNotify"),
    /**
     * 拒绝申请
     */
    APPLY_REFUSED("chatroom_applyRefusedNotify"),
    /**
     * 邀请上麦
     */
    INVITE_SITE("chatroom_inviteSiteNotify"),
    /**
     * 拒绝邀请
     */
    INVITE_REFUSED("chatroom_inviteRefusedNotify"),

    /**
     * 送礼物
     */
    SEND_GIFT("chatroom_gift"),
    ;

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
