package com.jamie.jmeter.listener;

import com.google.gson.Gson;
import com.jamie.jmeter.model.JMeterReportModel;
import com.jamie.jmeter.model.TestCaseModel;
import com.jamie.jmeter.pojo.ApiObject;
import com.jamie.jmeter.pojo.Dashboard;
import com.jamie.jmeter.pojo.TestCase;
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

        jMeterReportModel = new JMeterReportModel();
        testCaseModels = new ArrayList<>();
        dashboard = new Dashboard();

        count = 0; // 计数(执行通过的用例数)
        owner = context.getParameter("owner"); // 用例作者
        hostName = context.getParameter("host"); // 数据收集服务的域名
        if (hostName.endsWith("/")) {
            hostName = hostName.substring(0, hostName.length() - 1);
        }
        dashboard.setProjectName(context.getParameter("name")); // 项目名称
        dashboard.setEnv(context.getParameter("env")); // 执行环境
        dashboard.setProjectStartTime(System.currentTimeMillis()); // 执行开始时间
        String os = System.getProperty("os.name").toLowerCase();
        dashboard.setBuildType(os.contains("win") || os.contains("mac") ? 1 : 0); // 构建方式 手动/自动

    }

    @Override
    public void teardownTest(BackendListenerContext context) {

        dashboard.setProjectEndTime(System.currentTimeMillis()); // 项目结束执行时间
        dashboard.setProjectDuration(dashboard.getProjectEndTime() - dashboard.getProjectStartTime()); // 项目执行持续时间(毫秒)
        jMeterReportModel.setDashboard(dashboard); // 设置看板数据
        jMeterReportModel.setTestCaseModels(testCaseModels); // 设置用例相关数据

        // 完整数据提交给数据服务器
        HttpResponse<JsonNode> response;
        try {
            response = Unirest.post(hostName.concat("/jmeter/report/save"))
                    .header("Content-Type", "application/json")
                    .body(new Gson().toJson(jMeterReportModel))
                    .asJson();
            log.info("数据发送成功：{}", response.getBody().toString());
        } catch (UnirestException e) {
            log.error("数据发送异常：{}", e.getMessage());
        }
        log.info("测试数据: {}", jMeterReportModel);

    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext backendListenerContext) {

        // 记录每一条用例的执行结果
        for (SampleResult sampleResult : sampleResults) {
            setTestCaseModels(sampleResult);
        }
        // 记录Dashboard数据
        dashboard.setCaseNum(testCaseModels.size()); // 执行的用例总数
        dashboard.setCasePassNum(count); // 执行成功的用例总数
        dashboard.setCaseFailNum(testCaseModels.size() - count); // 执行失败的用例总数
        dashboard.setNewlyFailNum(testCaseModels.size() - count); // 新增失败的用例总数
        dashboard.setKeepFailingNum(0); // 设置持续失败默认值(在jmeter-report-backend服务里做计算)
        dashboard.setCasePassRate(BigDecimal
                .valueOf((double) dashboard.getCasePassNum() / dashboard.getCaseNum())
                .setScale(2, RoundingMode.HALF_UP)); // 用例执行成功率
    }

    // 得到testCaseModel的数据
    private void setTestCaseModels(SampleResult sampleResult) {

        TestCaseModel testCaseModel = new TestCaseModel();
        TestCase testCase = new TestCase();

        // 用例相关数据
        testCase.setModuleName(sampleResult.getThreadName().split(" ")[0]); // 模块名称
        testCase.setCaseOwner(owner); // 用例作者
        testCase.setCaseName(sampleResult.getSampleLabel()); // 用例名称
        testCase.setCaseStepNum(sampleResult.getSubResults().length); // 每条用例的步骤数
        testCase.setCaseResult(sampleResult.isSuccessful()); // 用例是否执行通过 1:成功 0:失败
        testCase.setCaseStartTime(sampleResult.getStartTime()); // 用例开始执行时间
        testCase.setCaseEndTime(sampleResult.getEndTime()); // 用例结束执行时间
        testCase.setCaseDuration(testCase.getCaseEndTime() - testCase.getCaseStartTime()); // 用例执行持续时间
        if (sampleResult.isSuccessful()) {
            count += 1;
        } // 用例执行成功数累加

        testCaseModel.setCaseInfo(testCase);

        // API相关数据(用例的步骤)
        List<ApiObject> apiObjects = new ArrayList<>();
        for (SampleResult subResult : sampleResult.getSubResults()) {

            HTTPSampleResult httpSampleResult = (HTTPSampleResult) subResult;
            ApiObject apiObject = new ApiObject();

            apiObject.setApiName(httpSampleResult.getSampleLabel()); // API名称
            apiObject.setRequestUrl(httpSampleResult.getUrlAsString()); // 请求URL
            apiObject.setRequestMethod(httpSampleResult.getHTTPMethod()); // 请求方法
            apiObject.setRequestHeader(httpSampleResult.getRequestHeaders()); // 请求头
            apiObject.setRequestBody(httpSampleResult.getSamplerData()); // 请求体
            apiObject.setResponseHeader(httpSampleResult.getResponseHeaders()); // 响应头
            apiObject.setResponseBody(httpSampleResult.getResponseDataAsString()); // 响应体
            apiObject.setResponseCode(httpSampleResult.getResponseCode()); // 响应码
            // 接口是否执行通过 1:成功 0:失败
            apiObject.setApiResult(true);
            if (!(apiObject.getResponseCode().startsWith("2") || apiObject.getResponseCode().startsWith("3"))) {
                apiObject.setApiResult(false);
            }
            // 断言信息
            AssertionResult[] assertionResults = httpSampleResult.getAssertionResults();
            StringBuilder stringBuilder = new StringBuilder();
            for (AssertionResult assertionResult : assertionResults) {
                if (assertionResult.isFailure()) {
                    apiObject.setApiResult(false);
                    stringBuilder
                            .append(assertionResult.getName())
                            .append(": ")
                            .append(assertionResult.getFailureMessage())
                            .append("\n");
                }
            }
            apiObject.setAssertMessage(stringBuilder.toString());
            apiObject.setApiStartTime(httpSampleResult.getStartTime()); // API执行开始时间
            apiObject.setApiEndTime(httpSampleResult.getEndTime()); // API执行结束时间
            apiObject.setApiDuration(httpSampleResult.getTime()); // API执行持续时间

            apiObjects.add(apiObject);

        }

        testCaseModel.setCaseSteps(apiObjects);

        testCaseModels.add(testCaseModel);

    }

}
