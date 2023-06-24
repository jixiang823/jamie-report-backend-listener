package com.jamie.jmeter.pojo;

import lombok.Data;

import java.math.BigDecimal;

// 看板
@Data
public class Dashboard {

    private String projectName; // 项目名称
    private String buildEnv; // 用例执行环境
    private Integer buildType; // 用例构建方式 0:自动 1:手动
    private Integer caseNum; // 执行的用例总数
    private Integer casePassNum; // 执行成功的用例总数
    private Integer caseFailNum; // 执行失败的用例总数
    private BigDecimal casePassRate; // 用例执行成功率
    private Integer newlyFailNum; // 新增失败的用例总数
    private Integer keepFailingNum; // 持续失败的用例总数
    private Long projectStartTime; // 项目执行开始时间
    private Long projectEndTime; // 项目执行结束时间
    private Long projectDuration; // 项目执行持续时间

}
