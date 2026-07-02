package com.example.dto;

// 通用返回DTO（前端Vue所有请求都解析这个结构）
public class ResultDTO<T> {
    private int code; // 200成功/500失败/400参数错误
    private String msg;
    private T data;

    // 静态工厂方法
    public static <T> ResultDTO<T> success(T data) {
        return new ResultDTO<>(200, "操作成功", data);
    }

    public static <T> ResultDTO<T> fail(String msg) {
        return new ResultDTO<>(500, msg, null);
    }

    public static <T> ResultDTO<T> paramError(String msg) {
        return new ResultDTO<>(400, msg, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ResultDTO() {
    }

    public ResultDTO(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public boolean isSuccess() {
        return code == 200;
    }
}
