package com.dji.sample.control.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

/**
 * @author sean
 * @version 1.4
 * @date 2023/3/17
 */
public enum TakeoffStatusEnum {

    TASK_READY("task_ready", "The drone is preparing to take off."),

    WAYLINE_PROGRESS("wayline_progress", "The drone is taking off."),

    WAYLINE_FAILED("wayline_failed", "The drone failed to take off."),

    WAYLINE_OK("wayline_ok", "The drone took off successfully."),

    WAYLINE_CANCEL("wayline_cancel", "The drone takeoff job has been cancelled."),

    TASK_FINISH("task_finish", "The drone takeoff job is completed.");

    String status;

    String message;

    TakeoffStatusEnum(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TakeoffStatusEnum find(String status) {
        return Arrays.stream(values()).filter(statusEnum -> statusEnum.status.equals(status)).findAny().get();
    }
}
