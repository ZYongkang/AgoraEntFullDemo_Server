package com.md.service.task;

import com.md.service.service.RoomInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时关闭房间
 */
@Component
@Slf4j
public class ScheduledTaskCloseRoom {

    @Resource
    private RoomInfoService roomInfoService;

    @Scheduled(fixedRate = 1000 * 60)
    public void scheduledTaskCloseRoom() {
        log.info("开始执行定时关闭房间任务");
        roomInfoService.searchRoomAndClose();
        log.info("结束定时关闭房间任务");
    }
}
