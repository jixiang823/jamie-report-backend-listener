package com.jamie.jmeter.model;

import com.jamie.jmeter.pojo.TestSummary;
import lombok.Data;

import java.util.List;


@Data
public class ReportModel {

    private TestSummary testSummary; // 看板信息
    private List<TestCaseModel> testCaseModels; // 测试用例信息

}
