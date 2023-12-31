package com.jamie.jmeter.pojo;


import lombok.Data;

// 详情页
@Data
public class ApiInfo {

    private String apiName; // API名称
    private String requestHost; // 请求域名
    private String requestPath; // 请求path
    private String requestMethod; // 请求方法
    private String requestHeader; // 请求头
    private String requestBody; // 请求体
    private String responseHeader; // 响应头
    private String responseBody; // 响应
    private String responseCode; // 响应码
    private String assertMessage; // 接口断言信息
    private Boolean apiResult; // 接口是否执行成功 1:成功 0:失败
    private Long startTime; // 接口执行开始时间
    private Long endTime; // 接口执行结束时间
    private Long duration; // 接口执行持续时间

}
