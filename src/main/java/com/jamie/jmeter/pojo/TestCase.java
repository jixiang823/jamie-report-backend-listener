package com.jamie.jmeter.pojo;

import lombok.Data;

// 列表页
@Data
public class TestCase {

    private String moduleName; // 模块名称
    private String caseOwner; // 用例作者
    private String caseName; // 用例名称
    private int caseStepNum; // 每条用例的步骤数
    private Boolean isCasePass; // 用例是否执行成功 0:成功 1:失败
    private Long caseStartTime; // 用例执行开始时间
    private Long caseEndTime; // 用例执行结束时间
    private Long caseDuration; // 用例执行持续时间

}


