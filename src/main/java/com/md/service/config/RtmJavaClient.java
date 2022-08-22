//package com.md.service.config;
//
//import com.md.service.common.CommonKey;
//import com.md.service.service.RoomInfoService;
//import io.agora.rtm.*;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.Resource;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//@Component
//@Slf4j
//public class RtmJavaClient {
//
//    @Value("${rtm.java.appId}")
//    private String appId;
//
//    private RtmClient mRtmClient;
//
//    @Resource
//    private RedisTemplate redisTemplate;
//
//    @Resource
//    private RoomInfoService roomInfoService;
//
//    @PostConstruct
//    public RtmClient init(){
//        try {
//            mRtmClient = RtmClient.createInstance(appId,
//                    new RtmClientListener() {
//                        @Override
//                        public void onConnectionStateChanged(int state, int reason) {
//                            System.out.println("on connection state changed to "
//                                    + state + " reason: " + reason);
//                        }
//                        @Override
//                        public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
//                            String msg = rtmMessage.getText();
//                            System.out.println("Receive message: " + msg
//                                    + " from " + peerId);
//                        }
//                        @Override
//                        public void onTokenExpired() {
//                        }
//                        @Override
//                        public void onPeersOnlineStatusChanged(Map<String, Integer> peersStatus) {
//                        }
//                        @Override
//                        public void onImageMessageReceivedFromPeer(RtmImageMessage message, String peerId) {
//
//                        }
//                        @Override
//                        public void onFileMessageReceivedFromPeer(RtmFileMessage message, String peerId) {
//
//                        }
//                        @Override
//                        public void onMediaUploadingProgress(RtmMediaOperationProgress progress, long requestId) {
//
//                        }
//                        @Override
//                        public void onMediaDownloadingProgress(RtmMediaOperationProgress progress, long requestId) {
//
//                        }
//                    });
//            login("system_admin");
//        } catch (Exception e) {
//            System.out.println("Rtm sdk init fatal error!");
//            throw new RuntimeException("Need to check rtm sdk init process");
//        }
//        return mRtmClient;
//    }
//
//    public void login(String userNo,String channel){
//        mRtmClient.login(null,userNo, new ResultCallback<Void>() {
//            @Override
//            public void onSuccess(Void responseInfo) {
//                redisTemplate.opsForValue().set(CommonKey.login_status + userNo,1);
//                System.out.println("login success!");
//                RtmChannel mRtmChannel = mRtmClient.createChannel(channel,
//                        new ChannelListener(channel));
//                if (mRtmChannel == null) {
//                    System.out.println("channel created failed!");
//                    return;
//                }
//                mRtmChannel.join(new ResultCallback<Void>() {
//                    @Override
//                    public void onSuccess(Void responseInfo) {
//                        System.out.println("join channel success!");
//                    }
//                    @Override
//                    public void onFailure(ErrorInfo errorInfo) {
//                        System.out.println("join channel failure! errorCode = "
//                                + errorInfo.getErrorCode());
//                    }
//                });
//            }
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                redisTemplate.opsForValue().set(CommonKey.login_status + userNo,0);
//                System.out.println("login failure!");
//            }
//        });
//    }
//
//    public void login(String userNo){
//        mRtmClient.login(null,userNo, new ResultCallback<Void>() {
//            @Override
//            public void onSuccess(Void responseInfo) {
//                System.out.println("login success!");
//            }
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                System.out.println("login failure!");
//            }
//        });
//    }
//
//
//    public void queryPeersOnlineStatus(Set<String> peerIds,Map<String,String> map){
//        mRtmClient.queryPeersOnlineStatus(peerIds, new 	ResultCallback< Map< String, Boolean>>() {
//            @Override
//            public void onSuccess(Map<String, Boolean> responseInfo) {
//                log.info("responseInfo:{}",responseInfo);
//                responseInfo.forEach((e,k) -> {
//                    if(!k){// 用户不在线直接关闭房间
//                        roomInfoService.closeRoom(map.get(e));
//                    }
//                });
//            }
//
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                log.info("errorInfo:{}",errorInfo);
//            }
//        });
//    }
//
//    public void joinRoom(String channel){
//        RtmChannel mRtmChannel = mRtmClient.createChannel(channel,
//                new ChannelListener(channel));
//        if (mRtmChannel == null) {
//            System.out.println("channel created failed!");
//            return;
//        }
//        mRtmChannel.join(new ResultCallback<Void>() {
//            @Override
//            public void onSuccess(Void responseInfo) {
//                System.out.println("join channel success!");
//            }
//
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                System.out.println("join channel failure! errorCode = "
//                        + errorInfo.getErrorCode());
//            }
//        });
//    }
//
//    public void closeRoom(String channel){
//        RtmChannel mRtmChannel = mRtmClient.createChannel(channel,
//                new ChannelListener(channel));
//        if (mRtmChannel == null) {
//            System.out.println("channel created failed!");
//            return;
//        }
//        mRtmChannel.leave(new ResultCallback<Void>(){
//            @Override
//            public void onSuccess(Void responseInfo) {
//                System.out.println("leave channel success!");
//            }
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                System.out.println("leave channel error!");
//            }
//        });
//    }
//
//    public void sendMessage(String channel,String msg){
//        return;
////        RtmChannel mRtmChannel = mRtmClient.createChannel(channel,
////                new ChannelListener(channel));
////        if (mRtmChannel == null) {
////           log.info("channel created failed!");
////            return;
////        }
////        RtmMessage message = mRtmClient.createMessage();
////        message.setText(msg);
////        mRtmChannel.sendMessage(message, new ResultCallback<Void>() {
////            @Override
////            public void onSuccess(Void aVoid) {
////                log.info("sendMessage channel:{},msg:{}",channel,msg);
////            }
////            @Override
////            public void onFailure(ErrorInfo errorInfo) {
////                final int errorCode = errorInfo.getErrorCode();
////                log.info("Send Message to channel failed, erroCode = "
////                        + errorCode);
////            }
////        });
//    }
//
//    public void sendMessagePeer(String msg,String userNo){
//        RtmMessage message = mRtmClient.createMessage();
//        message.setText(msg);
//        SendMessageOptions option = new SendMessageOptions();
//        mRtmClient.sendMessageToPeer(userNo,message,option,new ResultCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//            }
//            @Override
//            public void onFailure(ErrorInfo errorInfo) {
//                final int errorCode = errorInfo.getErrorCode();
//                System.out.println("Send Message to channel failed, erroCode = "
//                        + errorCode);
//            }
//        });
//    }
//}
//class ChannelListener implements RtmChannelListener {
//    private String channel_;
//
//    public ChannelListener(String channel) {
//        channel_ = channel;
//    }
//
//    @Override
//    public void onMemberCountUpdated(int memberCount) {
//    }
//
//    @Override
//    public void onAttributesUpdated(List<RtmChannelAttribute> attribute) {
//    }
//
//    @Override
//    public void onMessageReceived(
//            final RtmMessage message, final RtmChannelMember fromMember) {
//        String account = fromMember.getUserId();
//        String msg = message.getText();
//        System.out.println("Receive message from channel: " + channel_ +
//                " member: " + account + " message: " + msg);
//    }
//
//    @Override
//    public void onMemberJoined(RtmChannelMember member) {
//        String account = member.getUserId();
//        System.out.println("member " + account + " joined the channel "
//                + channel_);
//    }
//
//    @Override
//    public void onMemberLeft(RtmChannelMember member) {
//        String account = member.getUserId();
//        System.out.println("member " + account + " lefted the channel "
//                + channel_);
//    }
//
//    @Override
//    public void onImageMessageReceived(RtmImageMessage message, RtmChannelMember fromMember) {
//
//    }
//
//    @Override
//    public void onFileMessageReceived(RtmFileMessage message, RtmChannelMember fromMember) {
//
//    }
//}
