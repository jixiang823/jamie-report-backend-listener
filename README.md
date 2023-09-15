# jamie-report-backend-listener使用说明

## 前置条件
* 已启动Docker镜像 (详见 **Jamie接口自动化测试框架的使用说明**)
* 本地已安装JMeter

## 脚本规范
### 为确保测试脚本的可读性和可维护性, 请严格遵守以下规范:
* **独立运行**每个线程组.
* 一个**线程组**代表一个业务模块.
* 一个**事务控制器**代表一条测试用例.
* 一条测试用例由一个或多个**HTTP请求**组成.
* 每条测试用例能够单独运行, 测试用例之间不要形成依赖关系.
![image 4.png](src%2Fmain%2Fresources%2Fimage%204.png)

## 监听器配置
### JMeter引入jar包
将`jamie-report-backend-listener`工程打成的`.jar`包放置在JMeter的`/lib/ext`目录下.

### 配置后端监听器
新建`.jmx`脚本, `添加` -> `监听器` -> `后端监听器`, 选择 `JamieReportBackendListener`
![image.png](src%2Fmain%2Fresources%2Fimage.png)
填写相关参数. (`host`无需修改)
![image 2.png](src%2Fmain%2Fresources%2Fimage%202.png)
`.jmx`脚本执行完毕后, 会调用`9123`端口的服务, 解析测试结果并入库.

###  查看测试报告
浏览器打开 `http://localhost:30080` 可查看测试报告.
![image 3.png](src%2Fmain%2Fresources%2Fimage%203.png)