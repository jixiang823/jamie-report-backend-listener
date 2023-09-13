package com.jamie.jmeter.model;

import com.jamie.jmeter.pojo.ApiInfo;
import com.jamie.jmeter.pojo.TestCaseInfo;
import lombok.Data;

import java.util.List;

@Data
public class TestCaseModel {

    private TestCaseInfo caseInfo; // 测试用例信息
    private List<ApiInfo> caseSteps; // 测试用例内的接口信息

}
