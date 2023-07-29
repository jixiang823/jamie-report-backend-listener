package com.jamie.jmeter.pojo;

import lombok.Data;

// 列表页
@Data
public class TestCase {

    private String storyName; // 业务名称
    private String caseOwner; // 用例作者
    private String caseName; // 用例名称
    private Integer caseStepNum; // 每条用例的步骤数
    private Boolean caseResult; // 用例是否执行成功 1:成功 0:失败
    private Boolean newlyFail; // 用例是否新增失败
    private Boolean keepFailing; // 用例是否持续失败
    private Long startTime; // 用例执行开始时间
    private Long endTime; // 用例执行结束时间
    private Long duration; // 用例执行持续时间

}



