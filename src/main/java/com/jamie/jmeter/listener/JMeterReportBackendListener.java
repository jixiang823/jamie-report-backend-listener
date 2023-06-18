package com.jamie.jmeter.listener;

import com.jamie.jmeter.model.JMeterReportModel;
import com.jamie.jmeter.model.TestCaseModel;
import com.jamie.jmeter.pojo.ApiObject;
import com.jamie.jmeter.pojo.Dashboard;
import com.jamie.jmeter.pojo.TestCase;
import com.jamie.jmeter.utils.GsonUtil;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JMeterReportBackendListener extends AbstractBackendListenerClient {

    private static String owner; // 用例作者
    private static String hostName; // 数据存储服务的域名
    private JMeterReportModel jMeterReportModel; // 最终提交数据(看板+测试用例)
    private List<TestCaseModel> testCaseModels; // 测试用例数据
    private Dashboard dashboard; // 看板数据
    private Integer count; // 执行成功的用例计数


    // 后端监听器相关参数
    public Arguments getDefaultParameters() {

        Arguments arguments = new Arguments();
        arguments.addArgument("owner","用例作者");
        arguments.addArgument("host", "数据收集服务域名");
        arguments.addArgument("name", "被测项目");
        arguments.addArgument("env", "被测环境");
        return arguments;

    }

    @Override
    public void setupTest(BackendListenerContext context) {

        log.info(" ***** 测试开始 ***** ");
        // 最终提交的数据(Dashboard+TestCaseModels)
        jMeterReportModel = new JMeterReportModel();
        // 用例相关数据
        testCaseModels = new ArrayList<>();
        // 看板相关数据
        dashboard = new Dashboard();
        // 执行成功的用例技术
        count = 0;
        // 用例作者
        owner = context.getParameter("owner");
        // 数据收集服务的域名
        hostName = context.getParameter("host");
        if (hostName.endsWith("/")) {
            hostName = hostName.substring(0, hostName.length() - 1);
        }
        // 项目名称
        dashboard.setProjectName(context.getParameter("name"));
        // 执行环境
        dashboard.setEnv(context.getParameter("env"));
        // 执行开始时间
        dashboard.setProjectStartTime(System.currentTimeMillis());
        // 构建方式 手动/自动
        String os = System.getProperty("os.name").toLowerCase();
        dashboard.setType(os.contains("win") || os.contains("mac") ? 1 : 0);

    }

    @Override
    public void teardownTest(BackendListenerContext context) {

        // 结束执行时间
        dashboard.setProjectEndTime(System.currentTimeMillis());
        // 执行持续时间
        dashboard.setProjectDuration((dashboard.getProjectEndTime() - dashboard.getProjectStartTime()) / 1000);
        // 测试看板数据
        jMeterReportModel.setDashboard(dashboard);
        // 测试用例信息
        jMeterReportModel.setTestCaseModels(testCaseModels);
        // TODO 发送数据
        //send(jMeterReportModel);
        log.info("测试数据: {}", jMeterReportModel);
        log.info(" ***** 测试结束 ***** ");

    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext backendListenerContext) {

        // 记录每一条用例的执行结果
        for (SampleResult sampleResult : sampleResults) {
            handlerResult(sampleResult);
        }
        // 执行的用例总数
        dashboard.setCaseNum(testCaseModels.size());
        // 执行成功的用例总数
        dashboard.setCasePassNum(count);
        // 执行失败的用例总数
        dashboard.setCaseFailNum(testCaseModels.size() - count);
        // 新增失败的用例总数 (首次执行的值就是失败用例数)
        dashboard.setNewlyFailNum(testCaseModels.size() - count);
        // 用例执行成功率
        dashboard.setCassPassRate((double) dashboard.getCasePassNum() / dashboard.getCaseNum());

    }

    // 处理testCaseModel的数据
    private void handlerResult (SampleResult sampleResult) {

        // TestCase
        TestCase testCase = new TestCase();
        // TestCaseModel
        TestCaseModel testCaseModel = new TestCaseModel();
        // 模块名称
        testCase.setModuleName(sampleResult.getThreadName().split(" ")[0]);
        // 用例作者
        testCase.setCaseOwner(owner);
        // 用例名称
        testCase.setCaseName(sampleResult.getSampleLabel());
        // 每条用例的步骤数
        testCase.setCaseStepNum(sampleResult.getSubResults().length);
        // 用例是否执行成功 0:成功 1:失败
        testCase.setIsCasePass(sampleResult.isSuccessful());
        // 用例执行成功数累加
        if (sampleResult.isSuccessful()) {
            count += 1;
        }
        // 用例执行开始时间
        testCase.setCaseStartTime(sampleResult.getStartTime());
        // 用例执行结束时间
        testCase.setCaseEndTime(sampleResult.getEndTime());
        // 用例执行持续时间
        testCase.setCaseDuration(testCase.getCaseEndTime() - testCase.getCaseStartTime());
        testCaseModel.setTestCase(testCase);

        // ApiObjects
        List<ApiObject> apiObjects = new ArrayList<>();
        for (SampleResult subResult : sampleResult.getSubResults()) {
            HTTPSampleResult httpSampleResult = (HTTPSampleResult) subResult;
            // ApiObject
            ApiObject apiObject = new ApiObject();
            // API名称
            apiObject.setApiName(httpSampleResult.getSampleLabel());
            // 请求url
            apiObject.setRequestUrl(httpSampleResult.getUrlAsString());
            // 请求方法
            apiObject.setRequestMethod(httpSampleResult.getHTTPMethod());
            // 请求头
            apiObject.setRequestHeader(httpSampleResult.getRequestHeaders());
            // 请求体
            apiObject.setRequestBody(httpSampleResult.getSamplerData());
            // 响应头
            apiObject.setResponseHeader(httpSampleResult.getResponseHeaders());
            // 响应体
            apiObject.setResponseBody(httpSampleResult.getResponseDataAsString());
            // 响应码
            apiObject.setResponseCode(httpSampleResult.getResponseCode());
            // 接口是否执行成功 0:成功 1:失败
            apiObject.setIsApiPass(true);
            if (!(apiObject.getResponseCode().startsWith("2") || apiObject.getResponseCode().startsWith("3"))) {
                apiObject.setIsApiPass(false);
            }
            // 接口断言信息
            AssertionResult[] assertionResults = httpSampleResult.getAssertionResults();
            StringBuilder stringBuilder = new StringBuilder();
            for (AssertionResult assertionResult : assertionResults) {
                if (assertionResult.isFailure()) {
                    apiObject.setIsApiPass(false);
                    stringBuilder
                            .append(assertionResult.getName())
                            .append(": ")
                            .append(assertionResult.getFailureMessage())
                            .append("\n");
                }
            }
            apiObject.setAssertMessage(stringBuilder.toString());
            // 接口执行开始时间
            apiObject.setApiStartTime(httpSampleResult.getStartTime());
            // 接口执行结束时间
            apiObject.setApiEndTime(httpSampleResult.getEndTime());
            // 接口执行持续时间
            apiObject.setApiDuration(httpSampleResult.getTime());
            apiObjects.add(apiObject);
        }
        testCaseModel.setApiObjects(apiObjects);

        testCaseModels.add(testCaseModel);

    }

    // 调用数据收集服务,储存数据到数据库
    private void send(JMeterReportModel jMeterReportModel) {
        // TODO 可考虑改用其他http框架,以及发送数据
        HttpResponse<JsonNode> response;
        try {
            response = Unirest.post(hostName.concat("/jmeter-results/save"))
                    .header("Content-Type", "application/json")
                    .body(GsonUtil.objToJson(jMeterReportModel))
                    .asJson();
            log.info("数据发送成功：{}", response.getBody().toString());
        } catch (UnirestException e) {
            log.error("数据发送异常：{}", e.getMessage());
        }

    }

}
