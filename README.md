# Jamie API自动化测试框架
## 项目介绍
***Jamie***是一款基于`JMeter + Spring Boot + Vue Admin Template`的API自动化测试框架, 通过`Docker`一键安装部署. 框架具备提交脚本、手动/自动化执行测试脚本、生成测试报告、导出测试结果等功能.
![000](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/aff10028-dcba-4517-882f-344bd9013623)
## 开源地址
* https://gitee.com/jixiang823/projects?tag=jamie
* https://github.com/jixiang823
## 技术栈
*Docker | CentOS 7 | Java 8 | Springboot 2.7.4 | MySQL 5.7 | Vue 2 | JMeter 5.6.2*
![001](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/e9577ce8-ac79-4da0-8155-f0a28ed30e3b)
## 安装项目
```log
# 1. 安装docker (过程略,自行百度)
https://www.docker.com/

# 2. 拉取镜像
docker pull registry.cn-hangzhou.aliyuncs.com/jix/jamie:20231001

# 3. 获取镜像ID
docker images

# 4. 启动镜像 (把$IMAGE_ID替换成实际镜像ID)
docker run --privileged=true --cap-add SYS_ADMIN -e container=docker --name="jamie" -p 30022:22 -p 30080:80 -p 33306:3306 -p 9123:9123 -d  $IMAGE_ID /usr/sbin/init

# 5. 获取容器ID
docker ps

# 6. 启动容器 (把$CONTAINER_ID替换成实际容器ID)
docker start $CONTAINER_ID

# 7. 访问项目
http://localhost:30080
```

## 本地连接容器环境
```log
# 1. ShellCraft(XShell)访问容器
Host: localhost
Port: 30022
Protocal: ssh
User Name: root
Password: 2020

# 2. Navicat访问容器内数据库
Host: localhost
Port: 33306
User Name: root
Password: 123456
```

## JMeter配置
容器环境无需配置, 这里是对本地测试环境做配置.(本文演示用脚本下载: https://gitee.com/jixiang823/jamie-report-backend-listener/raw/main/demo.jmx)
1. 安装JMeter (过程略,自行百度)
   https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.2.zip
2. 引入后端监听器
    * 下载监听器插件 https://gitee.com/jixiang823/jamie-report-backend-listener/raw/main/jamie-report-backend-listener.jar
    * 将`jamie-report-backend-listener.jar`放置在`JMeter`的`/lib/ext`目录下.
3. 配置后端监听器
    * 新建`.jmx`脚本, `添加` -> `监听器` -> `后端监听器`, 选择 `JamieReportBackendListenerClient`
      <img width="493" alt="002" src="https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/dfc3f601-b253-4754-ae9e-1d8cd1ad78ca">
    * 填写相关参数
      <img width="828" alt="003" src="https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/9ddffbab-a0b4-44b0-ae5f-e8ef081b3d13">
## 脚本规范
为提高脚本的可读性和可维护性, 请遵守:
* **独立运行**每个线程组.
* 一个**线程组**代表一个业务模块.
* 一个**事务控制器**代表一条测试用例.
* 每条测试用例可由一个或多个**HTTP请求**组成.
* 确保每条测试用例能够单独运行, 测试用例之间无依赖关系.
![004](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/961893a2-6fb2-493e-8aec-6c280a87b2c4)
## 项目主要功能介绍
### 测试报告
本地执行JMeter脚本后, 可访问 [Jamie](http://localhost:30080/#/dashboard)查看测试报告. 每执行一个`.jmx`文件就会生成一份报告. 如果存在若干属于同一业务线的脚本, 请将脚本合并为一个`.jmx`文件.
![005](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/a8da454b-8cf3-4b7d-be6c-39bd838eee40)
**Dashboard报告页**呈现的信息主要包括:
* 饼图区域
   * 项目名称、执行环境、执行批次号、执行时间 (当前批次)
   * 测试用例总数、用例执行的成功率、失败率 (当前批次)
     ![006](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/79c2cae8-fb33-465e-bc88-3566e1913b0b)
* 趋势图区域
   * 测试用例通过数的趋势 (最近的7个批次)
   * 测试用例失败数的趋势 (最近的7个批次)
   * 测试用例通过率的趋势 (最近的7个批次)
     ![007](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/75ec66d7-c8f1-44ee-a44d-170f44ad0335)
* 晴雨表区域
   * 执行通过的用例个数  (当前批次)、 执行通过的趋势 (最近的7个批次)
   * 执行失败的用例个数  (当前批次)、 执行失败的趋势(最近的7个批次)
   * 新增失败的用例个数 (上一批次某用例执行成功, 但这一批次执行失败)、 新增失败的趋势 (最近的7个批次)
   * 持续失败的用例个数 (上一批次某用例执行失败, 这一批次还是执行失败)、 持续失败的趋势 (最近的7个批次)
     ![008](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/c685cbac-732e-4c8d-a286-0e5d9fb8dc40)
* 列表区域 (用例列表)
   * 业务名称、用例名称、用例的步骤数、用例的作者、用例的执行状态、用例的执行时间
     ![009](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/363f0573-3dd4-4cc6-b5cf-b87aa308a4e8)
* 列表区域 (用例详情)
   * API的请求信息
   * API的响应信息
   * API的断言结果
     ![010](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/12a3bab7-3dcd-46bb-9eff-a0650e06ae56)
### 容器内运行脚本
#### 手动运行
在自动化运行脚本前, 建议手动运行一次, 以确保脚本能够正常执行.
1. 提交本地脚本到服务器.
   <img width="709" alt="011" src="https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/b9d74d62-df35-40ed-a95b-6151d1786b4c">
2. 进入**手动运行**页面, 输入待执行脚本在容器内的绝对路径, 如`/root/scripts/demo.jmx`, 点击“运行”.
   <img width="703" alt="012" src="https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/9cb7b829-cdfe-4130-9dc0-1dd32917916d">
3. 观察**脚本日志**, 确认脚本运行结果符合预期.
   <img width="1010" alt="013" src="https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/4f1dfae3-05f8-4c41-b4e3-a891c886605a">
4. 回到**Dashboard**页面查看测试报告.

#### 自动化运行
1. 进入**自动化配置**页面, 填写corn表达式, 如`0 0 1 * * ?`, 表示每天凌晨1点执行一次.
2. 将**启用定时任务**按钮切换为亮态, 表示开启定时任务.
3. 点击**提交**按钮, 完成自动化配置.
   <img width="477" alt="014" src="https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/b8d88fcf-3442-4922-bf08-a0b210753eae">
4. 若需要关闭自动化任务, 可将**启用定时任务**按钮切换为灰态, 再点击**提交**按钮即可.

### 测试结果
**测试结果页**记录所有历史测试数据, 主要功能有:
1. 支持关键词多条件模糊查询.
   ![015](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/bc751912-b470-40e7-b3c6-bc18addb3057)
2. 点击测试结果页每行的数据, 可查看该条用例的具体执行情况.
   ![016](https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/755c9e8f-c93c-42f0-8cbe-6c471fefb8a7)
3. 支持一键**导出**测试结果.
   <img width="455" alt="017" src="https://github.com/jixiang823/jamie-report-backend-listener/assets/41544435/1bda59eb-e93a-4b97-adf7-875d375282a2">
