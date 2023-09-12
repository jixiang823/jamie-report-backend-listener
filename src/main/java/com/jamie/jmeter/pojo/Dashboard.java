package com.jamie.jmeter.pojo;

import lombok.Data;

import java.math.BigDecimal;

// 看板
@Data
public class Dashboard {

    private String featureName; // 业务线名称
    private String buildEnv; // 用例执行环境
    private Integer caseNum; // 执行的用例总数
    private Integer casePassNum; // 执行成功的用例总数
    private Integer caseFailNum; // 执行失败的用例总数
    private BigDecimal casePassRate; // 用例执行成功率
    private Integer newlyFailNum; // 新增失败的用例总数
    private Integer keepFailingNum; // 持续失败的用例总数
    private Long startTime; // 项目执行开始时间
    private Long endTime; // 项目执行结束时间
    private Long duration; // 项目执行持续时间

}
