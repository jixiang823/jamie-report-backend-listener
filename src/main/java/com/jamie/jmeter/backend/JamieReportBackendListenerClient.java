package com.jamie.jmeter.backend;

import com.google.gson.Gson;
import com.jamie.jmeter.dto.ReportDTO;
import com.jamie.jmeter.dto.TestCaseDTO;
import com.jamie.jmeter.pojo.ApiInfo;
import com.jamie.jmeter.pojo.TestSummary;
import com.jamie.jmeter.pojo.TestCaseInfo;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JamieReportBackendListenerClient extends AbstractBackendListenerClient {

    private static String owner; // 用例作者
    private ReportDTO reportDto; // 入库的测试数据
    private List<TestCaseDTO> testCaseDTOS; // 用例相关数据
    private TestSummary testSummary; // 概要
    private Integer count; // 计数 执行成功的用例数


    // 后端监听器相关参数
    public Arguments getDefaultParameters() {

        Arguments arguments = new Arguments();
        arguments.addArgument("owner","填写脚本作者");
        arguments.addArgument("feature", "填写业务线名称");
        arguments.addArgument("env", "填写环境名称");
        return arguments;

    }

    @Override
    public void setupTest(BackendListenerContext context) {

        reportDto = new ReportDTO();
        testCaseDTOS = new ArrayList<>();
        testSummary = new TestSummary();
        count = 0; // 计数(执行通过的用例数)
        owner = context.getParameter("owner"); // 用例作者
        testSummary.setFeatureName(context.getParameter("feature")); // 项目名称
        testSummary.setBuildEnv(context.getParameter("env")); // 执行环境
        testSummary.setStartTime(System.currentTimeMillis()); // 执行开始时间

    }

    @Override
    public void teardownTest(BackendListenerContext context) {

        testSummary.setEndTime(System.currentTimeMillis()); // 项目结束执行时间
        testSummary.setDuration(testSummary.getEndTime() - testSummary.getStartTime()); // 项目执行持续时间(毫秒)
        reportDto.setTestSummary(testSummary); // 设置概述相关数据
        reportDto.setTestCaseDTOS(testCaseDTOS); // 设置用例相关数据

        // 完整数据提交给数据服务器
        try {
            Unirest.post("http://localhost:9123/report/save")
                    .header("Content-Type", "application/json")
                    .body(new Gson().toJson(reportDto))
                    .asJson();
            log.info("数据发送成功");
        } catch (UnirestException e) {
            log.error("数据发送异常：{}", e.getMessage());
        }
        log.info("测试数据: {}", reportDto);

    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext backendListenerContext) {

        // 记录每一条用例的执行结果
        for (SampleResult sampleResult : sampleResults) {
            setTestCaseModels(sampleResult);
        }
        // 记录testSummary数据
        testSummary.setCaseNum(testCaseDTOS.size()); // 执行的用例总数
        testSummary.setCasePassNum(count); // 执行成功的用例总数
        testSummary.setCaseFailNum(testCaseDTOS.size() - count); // 执行失败的用例总数
        testSummary.setNewlyFailNum(0); // 新增失败的用例总数(在jmeter-report-backend服务里做计算)
        testSummary.setKeepFailingNum(0); // 设置持续失败默认值(在jmeter-report-backend服务里做计算)
        testSummary.setCasePassRate(BigDecimal
                .valueOf((double) testSummary.getCasePassNum() / testSummary.getCaseNum())
                .setScale(2, RoundingMode.HALF_UP)); // 用例执行成功率
    }

    // 得到testCaseModel的数据
    private void setTestCaseModels(SampleResult sampleResult) {

        TestCaseDTO testCaseDto = new TestCaseDTO();
        TestCaseInfo testCaseInfo = new TestCaseInfo();
        // 用例相关数据
        testCaseInfo.setStoryName(sampleResult.getThreadName().split(" ")[0].trim()); // 模块名称
        testCaseInfo.setCaseOwner(owner); // 用例作者
        testCaseInfo.setCaseName(sampleResult.getSampleLabel()); // 用例名称
        testCaseInfo.setCaseStepNum(sampleResult.getSubResults().length); // 每条用例的步骤数
        testCaseInfo.setCaseResult(sampleResult.isSuccessful()); // 用例是否执行通过 1:成功 0:失败
        testCaseInfo.setNewlyFail(false); // 设置默认值
        testCaseInfo.setKeepFailing(false); // 设置默认值
        testCaseInfo.setStartTime(sampleResult.getStartTime()); // 用例开始执行时间
        testCaseInfo.setEndTime(sampleResult.getEndTime()); // 用例结束执行时间
        testCaseInfo.setDuration(testCaseInfo.getEndTime() - testCaseInfo.getStartTime()); // 用例执行持续时间
        if (sampleResult.isSuccessful()) {
            count += 1;
        } // 用例执行成功数累加

        testCaseDto.setCaseInfo(testCaseInfo);

        // API相关数据(用例的步骤)
        List<ApiInfo> apiInfos = new ArrayList<>();
        for (SampleResult subResult : sampleResult.getSubResults()) {

            HTTPSampleResult httpSampleResult = (HTTPSampleResult) subResult;
            ApiInfo apiInfo = new ApiInfo();

            apiInfo.setApiName(httpSampleResult.getSampleLabel()); // API名称
            apiInfo.setRequestHost(httpSampleResult.getURL().getHost()); // 请求域名
            apiInfo.setRequestPath(httpSampleResult.getURL().getPath()); // 请求路径
            apiInfo.setRequestMethod(httpSampleResult.getHTTPMethod()); // 请求方法
            apiInfo.setRequestHeader(httpSampleResult.getRequestHeaders()); // 请求头
            apiInfo.setRequestBody(httpSampleResult.getSamplerData()); // 请求体
            apiInfo.setResponseHeader(httpSampleResult.getResponseHeaders()); // 响应头
            apiInfo.setResponseBody(httpSampleResult.getResponseDataAsString()); // 响应体
            apiInfo.setResponseCode(httpSampleResult.getResponseCode()); // 响应码
            // 接口是否执行通过 1:成功 0:失败
            apiInfo.setApiResult(true);
            if (!(apiInfo.getResponseCode().startsWith("2") || apiInfo.getResponseCode().startsWith("3"))) {
                apiInfo.setApiResult(false);
            }
            // 断言信息
            AssertionResult[] assertionResults = httpSampleResult.getAssertionResults();
            StringBuilder stringBuilder = new StringBuilder();
            for (AssertionResult assertionResult : assertionResults) {
                if (assertionResult.isFailure()) {
                    apiInfo.setApiResult(false);
                    stringBuilder
                            .append(assertionResult.getName())
                            .append(": ")
                            .append(assertionResult.getFailureMessage())
                            .append("\n");
                }
            }
            apiInfo.setAssertMessage(stringBuilder.toString());
            apiInfo.setStartTime(httpSampleResult.getStartTime()); // API执行开始时间
            apiInfo.setEndTime(httpSampleResult.getEndTime()); // API执行结束时间
            apiInfo.setDuration(httpSampleResult.getTime()); // API执行持续时间

            apiInfos.add(apiInfo);

        }

        testCaseDto.setCaseSteps(apiInfos);

        testCaseDTOS.add(testCaseDto);

    }

}
