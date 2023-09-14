package com.jamie.jmeter.listener;

import com.google.gson.Gson;
import com.jamie.jmeter.model.ReportModel;
import com.jamie.jmeter.model.TestCaseModel;
import com.jamie.jmeter.pojo.ApiInfo;
import com.jamie.jmeter.pojo.TestSummary;
import com.jamie.jmeter.pojo.TestCaseInfo;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
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
public class JamieReportBackendListener extends AbstractBackendListenerClient {

    private static String owner; // 用例作者
    private static String hostName; // 数据存储服务的域名
    private ReportModel reportModel; // 入库的测试数据
    private List<TestCaseModel> testCaseModels; // 用例相关数据
    private TestSummary testSummary; // 概要
    private Integer count; // 计数 执行成功的用例数


    // 后端监听器相关参数
    public Arguments getDefaultParameters() {

        Arguments arguments = new Arguments();
        arguments.addArgument("owner","脚本作者");
        arguments.addArgument("host", "数据收集服务域名");
        arguments.addArgument("feature", "业务线");
        arguments.addArgument("env", "脚本执行环境");
        return arguments;

    }

    @Override
    public void setupTest(BackendListenerContext context) {

        reportModel = new ReportModel();
        testCaseModels = new ArrayList<>();
        testSummary = new TestSummary();
        count = 0; // 计数(执行通过的用例数)
        owner = context.getParameter("owner"); // 用例作者
        hostName = context.getParameter("host"); // 数据收集服务的域名
        if (hostName.endsWith("/")) {
            hostName = hostName.substring(0, hostName.length() - 1);
        }
        testSummary.setFeatureName(context.getParameter("feature")); // 项目名称
        testSummary.setBuildEnv(context.getParameter("env")); // 执行环境
        testSummary.setStartTime(System.currentTimeMillis()); // 执行开始时间

    }

    @Override
    public void teardownTest(BackendListenerContext context) {
        testSummary.setEndTime(System.currentTimeMillis()); // 项目结束执行时间
        testSummary.setDuration(testSummary.getEndTime() - testSummary.getStartTime()); // 项目执行持续时间(毫秒)
        reportModel.setTestSummary(testSummary); // 设置看板数据
        reportModel.setTestCaseModels(testCaseModels); // 设置用例相关数据

        // 完整数据提交给数据服务器
        HttpResponse<JsonNode> response;
        try {
            response = Unirest.post(hostName.concat("/report/save"))
                    .header("Content-Type", "application/json")
                    .body(new Gson().toJson(reportModel))
                    .asJson();
            log.info("数据发送成功");
        } catch (UnirestException e) {
            log.error("数据发送异常：{}", e.getMessage());
        }
        log.info("测试数据: {}", reportModel);

    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext backendListenerContext) {

        // 记录每一条用例的执行结果
        for (SampleResult sampleResult : sampleResults) {
            setTestCaseModels(sampleResult);
        }
        // 记录testSummary数据
        testSummary.setCaseNum(testCaseModels.size()); // 执行的用例总数
        testSummary.setCasePassNum(count); // 执行成功的用例总数
        testSummary.setCaseFailNum(testCaseModels.size() - count); // 执行失败的用例总数
        testSummary.setNewlyFailNum(0); // 新增失败的用例总数(在jmeter-report-backend服务里做计算)
        testSummary.setKeepFailingNum(0); // 设置持续失败默认值(在jmeter-report-backend服务里做计算)
        testSummary.setCasePassRate(BigDecimal
                .valueOf((double) testSummary.getCasePassNum() / testSummary.getCaseNum())
                .setScale(2, RoundingMode.HALF_UP)); // 用例执行成功率
    }

    // 得到testCaseModel的数据
    private void setTestCaseModels(SampleResult sampleResult) {

        TestCaseModel testCaseModel = new TestCaseModel();
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

        testCaseModel.setCaseInfo(testCaseInfo);

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

        testCaseModel.setCaseSteps(apiInfos);

        testCaseModels.add(testCaseModel);

    }

}
