package com.jamie.jmeter.dto;

import com.jamie.jmeter.pojo.TestSummary;
import lombok.Data;

import java.util.List;


@Data
public class ReportDTO {

    private TestSummary testSummary; // 数据总览
    private List<TestCaseDTO> testCaseDTOS; // 测试用例信息

}
