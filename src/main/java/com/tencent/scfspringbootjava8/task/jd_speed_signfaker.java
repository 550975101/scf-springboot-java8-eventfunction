package com.tencent.scfspringbootjava8.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.scfspringbootjava8.config.CookieConfig;
import com.tencent.scfspringbootjava8.utils.HMACSHA256Util;
import com.tencent.scfspringbootjava8.utils.HttpUtils;
import com.tencent.scfspringbootjava8.utils.TaskFlag;
import com.tencent.scfspringbootjava8.utils.UserAgentUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author by 封心
 * @classname jd_speed_signfaker
 * @description TODO
 * @date 2021/11/18 16:37
 * <p>
 * 京东极速版签到+赚现金任务
 * 每日9毛左右，满3，10，50可兑换无门槛红包
 * ⚠️⚠️⚠️一个号需要运行40分钟左右
 * 活动时间：长期
 * 活动入口：京东极速版app-现金签到
 */
@Component("jd_speed_signfaker")
public class jd_speed_signfaker extends abstract_jd_task {

    static ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CookieConfig config;

    @Scheduled(cron = "21 3,8 * * * ?")
    @Override
    public void start() {
        bathExec(config.getList());
    }

    @Override
    public void exec(String cookie) {
        try {
            //TODO
            richManIndex(cookie);
            wheelsHome(cookie);
            apTaskList(cookie);
            wheelsHome(cookie);

            invite(cookie);
            invite2(cookie);

            taskList(cookie);
            queryJoy(cookie);
            //cash()
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void cash(String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("MyAssetsService.execute", "{\"method\": \"userCashRecord\", \"data\": {\"channel\": 1, \"pageNum\": 1, \"pageSize\": 20}}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            long data = jsonNode.get("data").get("goldBalance").asLong();
            System.out.println("data.goldBalance" + data);
        } catch (Exception e) {
            System.out.println("API请求失败");
            e.printStackTrace();
        }
    }

    public void queryJoy(String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("ClientHandleService.execute", "{\"method\": \"queryJoyPage\", \"data\": {\"channel\": 1}}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            if (!jsonNode.get("data").get("taskBubbles").isNull()) {
                Iterator<JsonNode> elements = jsonNode.get("data").get("taskBubbles").elements();
                while (elements.hasNext()) {
                    JsonNode next = elements.next();
                    long id = next.get("id").asLong();
                    long activeType = next.get("activeType").asLong();
                    rewardTask(id, activeType, cookie);
                    Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            System.out.println("API请求失败");
            e.printStackTrace();
        }
    }

    private void rewardTask(long id, long activeType, String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("ClientHandleService.execute", "{\"method\":\"joyTaskReward\",\"data\":{\"id\":" + id + ",\"channel\":1,\"clientTime\":" + System.currentTimeMillis() + ",\"activeType\":" + activeType + "}}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            long code = jsonNode.get("code").asLong();
            if (code == 0) {
                System.out.println("气泡收取成功，获得" + jsonNode.get("data").get("reward").asLong() + "金币");
            } else {
                System.out.println("气泡收取失败，" + jsonNode.get("message").asText());
            }
        } catch (Exception e) {
            System.out.println("API请求失败");
            e.printStackTrace();
        }
    }

    public void taskList(String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("ClientHandleService.execute", "{\"version\":\"3.1.0\",\"method\":\"newTaskCenterPage\",\"data\":{\"channel\":1}}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            Iterator<JsonNode> data = jsonNode.get("data").elements();
            while (data.hasNext()) {
                JsonNode next = data.next();
                String taskName = next.get("taskInfo").get("mainTitle").asText();
                long status = next.get("taskInfo").get("status").asLong();
                if (status == 0) {
                    long taskType = next.get("taskInfo").get("taskType").asLong();
                    if (taskType >= 1000) {
                        doTask(taskType, cookie);
                        Thread.sleep(1000);
                    } else {
                        TaskFlag taskFlag = new TaskFlag();
                        taskFlag.setFlag(true);
                        while (taskFlag.getFlag()) {
                            if (taskType != 3) {
                                queryItem(taskType, taskFlag, cookie);
                            } else {
                                startItem("", taskType, taskFlag, cookie);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("API请求失败，请检查网路重试");
            e.printStackTrace();
        }
    }

    public void doTask(long taskType, String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("ClientHandleService.execute", "{\"method\":\"marketTaskRewardPayment\",\"data\":{\"channel\":1,\"clientTime\":" + System.currentTimeMillis() + ",\"activeType\":\"" + taskType + "\"}}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            long code = jsonNode.get("code").asLong();
            if (code == 0) {
                String mainTitle = jsonNode.get("data").get("taskInfo").get("mainTitle").asText();
                long reward = jsonNode.get("data").get("reward").asLong();
                System.out.println("任务" + mainTitle + " 完成，预计获得" + reward + "金币");
            } else {
                System.out.println("任务完成失败: " + jsonNode.get("message").asText());
            }
        } catch (Exception e) {
            System.out.println("请求api失败");
            e.printStackTrace();
        }
    }

    public void queryItem(long taskType, TaskFlag taskFlag, String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("ClientHandleService.execute", "{\"method\":\"queryNextTask\",\"data\":{\"channel\":1,\"activeType\":\"" + taskType + "\"}}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            long code = jsonNode.get("code").asLong();
            if (code == 0 && !jsonNode.get("data").isNull()) {
                String nextResource = jsonNode.get("data").get("nextResource").asText();
                startItem(nextResource, taskType, taskFlag, cookie);
            } else {
                System.out.println("商品任务开启失败: " + jsonNode.get("message").asText());
                taskFlag.setFlag(false);
            }
        } catch (Exception e) {
            System.out.println("请求api失败");
            e.printStackTrace();
        }
    }

    public void startItem(String nextResource, long taskType, TaskFlag taskFlag, String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("ClientHandleService.execute", "{\"method\":\"enterAndLeave\",\"data\":{\"activeId\":\"" + nextResource + "\",\"clientTime\":" + System.currentTimeMillis() + ",\"channel\":\"1\",\"messageType\":\"1\",\"activeType\":" + taskType + "}}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            long code = jsonNode.get("code").asLong();
            if (code == 0 && !jsonNode.get("data").isNull()) {
                long isTaskLimit = jsonNode.get("data").get("taskInfo").get("isTaskLimit").asLong();
                if (isTaskLimit == 0) {
                    long videoBrowsing = jsonNode.get("data").get("taskInfo").get("videoBrowsing").asLong();
                    long taskCompletionProgress = jsonNode.get("data").get("taskInfo").get("taskCompletionProgress").asLong();
                    long taskCompletionLimit = jsonNode.get("data").get("taskInfo").get("taskCompletionLimit").asLong();
                    if (taskType != 3) {
                        videoBrowsing = taskType == 1 ? 5 : 10;
                        long l = (taskCompletionProgress + 1) / taskCompletionLimit;
                        System.out.println(l + "浏览商品任务记录成功,等待 " + videoBrowsing + "秒");
                        Thread.sleep(videoBrowsing * 1000);
                        String uuid = jsonNode.get("data").get("uuid").asText();
                        endItem(uuid, taskType, nextResource, videoBrowsing, taskFlag, cookie);
                    }
                } else {
                    taskFlag.setFlag(false);
                    System.out.println("任务已经达上限");
                }
            } else {
                taskFlag.setFlag(false);
                System.out.println("任务开启失败: " + jsonNode.get("message").asText());
            }
        } catch (Exception e) {
            System.out.println("请求api失败");
            e.printStackTrace();
        }
    }

    public void endItem(String uuid, long taskType, String nextResource, long videoBrowsing, TaskFlag taskFlag, String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("ClientHandleService.execute", "{\"method\":\"enterAndLeave\",\"data\":{\"channel\":\"1\",\"clientTime\":" + System.currentTimeMillis() + ",\"uuid\":\"" + uuid + "\",\"videoTimeLength\":" + videoBrowsing + ",\"messageType\":\"2\",\"activeType\":\"" + taskType + "\",\"activeId\":\"" + nextResource + "\"}}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            long code = jsonNode.get("code").asLong();
            boolean isSuccess = jsonNode.get("isSuccess").asBoolean();
            if (code == 0 && isSuccess) {
                rewardItem(uuid, taskType, nextResource, videoBrowsing, taskFlag, cookie);
            } else {
                System.out.println("任务结束失败" + jsonNode.get("message").asText());
            }
        } catch (Exception e) {
            System.out.println("请求api失败");
            e.printStackTrace();
        }
    }

    private void rewardItem(String uuid, long taskType, String nextResource, long videoBrowsing, TaskFlag taskFlag, String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("ClientHandleService.execute", "{\"method\":\"rewardPayment\",\"data\":{\"channel\":\"1\",\"clientTime\":" + System.currentTimeMillis() + ",\"uuid\":\"" + uuid + "\",\"videoTimeLength\":" + videoBrowsing + ",\"messageType\":\"2\",\"activeType\":" + taskType + ",\"activeId\":\"" + nextResource + "\"}}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            long code = jsonNode.get("code").asLong();
            boolean isSuccess = jsonNode.get("isSuccess").asBoolean();
            if (code == 0 && isSuccess) {
                System.out.println("任务完成，获得: " + jsonNode.get("data").get("reward").asLong());
            } else {
                System.out.println("任务结束失败" + jsonNode.get("message").asText());
            }
        } catch (Exception e) {
            System.out.println("请求api失败");
            e.printStackTrace();
        }
    }


    public void richManIndex(String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("richManIndex", "{\"actId\": \"hbdfw\", \"needGoldToast\": \"true\"}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            JsonNode jsonNode = objectMapper.readTree(res);
            long code = jsonNode.get("code").asLong();
            if (code == 0 && !jsonNode.get("data").isNull() && !jsonNode.get("data").get("userInfo").isNull()) {
                System.out.println("用户当前位置" + jsonNode.get("data").get("userInfo").get("position").asText() + "，剩余机会: " + jsonNode.get("data").get("userInfo").get("randomTimes").asLong());
                long randomTimes = jsonNode.get("data").get("userInfo").get("randomTimes").asLong();
                for (long i = 0; i < randomTimes; i++) {
                    shootRichManDice(cookie);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shootRichManDice(String cookie) {
        String res = null;
        try {
            Map<String, Object> request = taskUrl("shootRichManDice", "{\"actId\": \"hbdfw\"}", cookie);
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            JsonNode jsonNode = objectMapper.readTree(res);
            boolean data = jsonNode.get("data").isNull();
            boolean rewardType = jsonNode.get("data").get("rewardType").isNull();
            boolean couponDesc = jsonNode.get("data").get("couponDesc").isNull();
            long code = jsonNode.get("code").asLong();
            if (code == 0 && !data && !rewardType && !couponDesc) {
                long couponUsedValue = jsonNode.get("data").get("couponUsedValue").asLong();
                long rewardValue = jsonNode.get("data").get("rewardValue").asLong();
                String poolName = jsonNode.get("data").get("poolName").asText();
                System.out.println("红包大富翁抽奖获得：" + (couponUsedValue - rewardValue) + " " + poolName);
            } else {
                System.out.println("红包大富翁抽奖获: 空气");
            }
        } catch (Exception e) {
            System.out.println("红包大富翁抽奖程序异常");
            e.printStackTrace();
        }
    }

    // 大转盘
    public void wheelsHome(String cookie) {
        String body = "{\"linkId\":\"toxw9c5sy9xllGBr3QFdYg\"}";
        Map<String, Object> request = taskGetUrl("wheelsHome", URLEncoder.encode(body), cookie);
        String res = null;
        try {
            res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            long code = jsonNode.get("code").asLong();
            if (code == 0) {
                System.out.println("幸运大转盘】剩余抽奖机会：" + jsonNode.get("data").get("lotteryChances").asLong());
                long lotteryChances = jsonNode.get("data").get("lotteryChances").asLong();
                if (lotteryChances > 0) {
                    for (long i = 0; i < lotteryChances; i++) {
                        wheelsLottery(cookie);
                        Thread.sleep(500);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //// 大转盘
    public void wheelsLottery(String cookie) {
        String body = "{\"linkId\":\"toxw9c5sy9xllGBr3QFdYg\"}";
        Map<String, Object> request = taskGetUrl("wheelsLottery", URLEncoder.encode(body), cookie);
        try {
            String res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            JsonNode jsonNode = objectMapper.readTree(res);
            if (!jsonNode.get("data").isNull()) {
                if (!jsonNode.get("data").get("rewardType").isNull()) {
                    long couponUsedValue = jsonNode.get("data").get("couponUsedValue").asLong();
                    long rewardValue = jsonNode.get("data").get("rewardValue").asLong();
                    System.out.println("幸运大转盘抽奖获得：" + (couponUsedValue - rewardValue) + " " + jsonNode.get("data").get("couponDesc").asText());
                } else {
                    System.out.println("幸运大转盘抽奖获得：空气");
                }
            }
        } catch (Exception e) {
            System.out.println("幸运大转盘抽奖获得：程序异常");
            e.printStackTrace();
        }
    }

    //大转盘任务
    public void apTaskList(String cookie) {
        String body = "{\"linkId\":\"toxw9c5sy9xllGBr3QFdYg\"}";
        Map<String, Object> request = taskGetUrl("apTaskList", URLEncoder.encode(body), cookie);
        try {
            String res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            System.out.println(res);
            JsonNode jsonNode = objectMapper.readTree(res);
            long code = jsonNode.get("code").asLong();
            if (code == 0) {
                Iterator<JsonNode> data = jsonNode.get("data").elements();
                while (data.hasNext()) {
                    JsonNode next = data.next();
                    boolean taskFinished = next.get("taskFinished").asBoolean();
                    List<String> collect = Stream.of("SIGN", "BROWSE_CHANNEL").collect(Collectors.toList());
                    String taskType = next.get("taskType").asText();
                    if (!taskFinished && collect.contains(taskType)) {
                        System.out.println("去做任务 " + next.get("taskTitle").asText());
                        long id = next.get("id").asLong();
                        String taskSourceUrl = next.get("taskSourceUrl").asText();
                        //做任务
                        apDoTask(taskType, id, 4, taskSourceUrl, cookie);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void invite(String cookie) {
        long t = System.currentTimeMillis();
        String[] inviterIdArr = {"5V7vHE23qh2EkdBHXRFDuA==",
                "4AVQao+eH8Q8kvmXnWmkG8ef/fNr5fdejnD9+9Ugbec=",
                "jbGBRBPo5DmwB9ntTCSVOGXuh1YQyccCuZpWwb3PlIc=",
                "wXX9SjXOdYMWe5Ru/1+x9A==",
                "mCvmrmFghpDCLcL3VZs53BkAhucziHAYn3HhPmURJJE=",
                "E9EvSFNuA1pahSQT0uSsXkW1v0j+QOHQbk8+peJYc0I=",
                "zPiP6uq7hi9AS7VecMnRvA==",
                "YQ5wwbSWDzNIudDC2OWvSw==",
                "+vbK7QKOtpHM4dsSRqUPPX/11g/P71iBYh46dyiMuKk=",
                "w22w0sZEccp/OWxg1d20RtsryQGfghc94PsLIBqeX0E=",
                "VdDrieI4oR6XwchWlxwfCQqEf6/k8cYvTG52R1ToSoQ=",
                "D7QE/1ouU1wA14mAV0zGMg=="};
        String inviterId = inviterIdArr[(int) Math.floor(Math.random() * inviterIdArr.length)];
        String url = "https://api.m.jd.com/?t=" + t;
        String body = "{\"method\":\"attendInviteActivity\",\"data\":{\"inviterPin\":\"" + inviterId + "\",\"channel\":1,\"token\":\"\",\"frontendInitStatus\":\"\"}}";
        body = "functionId=InviteFriendChangeAssertsService&body=" + URLEncoder.encode(body) + "&referer=-1&eid=eidI9b2981202fsec83iRW1nTsOVzCocWda3YHPN471AY78%2FQBhYbXeWtdg%2F3TCtVTMrE1JjM8Sqt8f2TqF1Z5P%2FRPGlzA1dERP0Z5bLWdq5N5B2VbBO&aid=&client=ios&clientVersion=14.4.2&networkType=wifi&fp=-1&uuid=ab048084b47df24880613326feffdf7eee471488&osVersion=14.4.2&d_brand=iPhone&d_model=iPhone10,2&agent=-1&pageClickKey=-1&platform=3&lang=zh_CN&appid=market-task-h5&_t=" + t;
        HashMap<String, String> headers = new HashMap<>(16);
        headers.put("Host", "api.m.jd.com");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Origin", "https://invite-reward.jd.com");
        headers.put("Accept-Language", "zh-Hans-CN;q=1,en-CN;q=0.9");
        headers.put("User-Agent", UserAgentUtils.randomUserAgent());
        headers.put("Referer", "https://invite-reward.jd.com/");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Cookie", cookie);
        String res = HttpUtils.doPost(url, headers, body);
        System.out.println(res);
    }

    public void invite2(String cookie) {
        long t = System.currentTimeMillis();
        String[] inviterIdArr = {"5V7vHE23qh2EkdBHXRFDuA==",
                "wXX9SjXOdYMWe5Ru/1+x9A==",
                "mCvmrmFghpDCLcL3VZs53BkAhucziHAYn3HhPmURJJE=",
                "4AVQao+eH8Q8kvmXnWmkG8ef/fNr5fdejnD9+9Ugbec=",
                "jbGBRBPo5DmwB9ntTCSVOGXuh1YQyccCuZpWwb3PlIc="};
        String inviterId = inviterIdArr[(int) Math.floor(Math.random() * inviterIdArr.length)];
        String url = "https://api.m.jd.com/";
        String body = "{\"method\":\"participateInviteTask\",\"data\":{\"channel\":\"1\",\"encryptionInviterPin\":\"" + inviterId + "\",\"type\":1}}";
        body = "functionId=TaskInviteService&body=" + URLEncoder.encode(body) + "&appid=market-task-h5&uuid=&_t=" + t;
        HashMap<String, String> headers = new HashMap<>(16);
        headers.put("Host", "api.m.jd.com");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Origin", "https://gray.jd.com");
        headers.put("Accept-Language", "zh-CN,zh-Hans;q=0.9");
        headers.put("User-Agent", UserAgentUtils.randomUserAgent());
        headers.put("Referer", "https://gray.jd.com/");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Cookie", cookie);
        String res = HttpUtils.doPost(url, headers, body);
        System.out.println(res);
    }

    private void apDoTask(String taskType, long id, int i, String taskSourceUrl, String cookie) {
        String body = "{\"linkId\":\"toxw9c5sy9xllGBr3QFdYg\",\"taskType\":\"" + taskType + "\",\"taskId\":" + id + ",\"channel\":" + i + ",\"itemId\":\"" + taskSourceUrl + "\"}";
        Map<String, Object> request = taskGetUrl("apDoTask", URLEncoder.encode(body), cookie);
        try {
            String res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
            JsonNode jsonNode = objectMapper.readTree(res);
            //data.code ===0 && data.data && data.data.finished
            long code = jsonNode.get("code").asLong();
            if (code == 0 && !jsonNode.get("data").isNull() && jsonNode.get("data").get("finished").asBoolean()) {
                System.out.println("任务完成");
            } else {
                System.out.println("执行任务结果: " + res);
            }
        } catch (Exception e) {
            System.out.println("执行任务异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Object> taskUrl(String functionId, String body, String cookie) throws Exception {
        Map<String, Object> data = new HashMap<>(2);
        long time = System.currentTimeMillis();
        //lite-android&{"actId":"hbdfw","needGoldToast":"true"}&android&3.1.0&richManIndex&1637401085515&846c4c32dae910ef
        String str = "lite-android&" + body + "&android&3.1.0&" + functionId + "&" + time + "&846c4c32dae910ef";
        String salt = "12aea658f76e453faf803d15c40a72e0";
        String sign = HMACSHA256Util.HMACSHA256(str, salt);
        String url = "https://api.m.jd.com/api?functionId=" + functionId + "&body=" + URLEncoder.encode(body) + "&appid=lite-android&client=android&uuid=846c4c32dae910ef&clientVersion=3.1.0&t=" + time + "&sign=" + sign;
        HashMap<String, String> headers = new HashMap<>(16);
        headers.put("Cookie", cookie);
        headers.put("Host", "api.m.jd.com");
        headers.put("Accept", "*/*");
        headers.put("User-Agent", "JDMobileLite/3.1.0 (iPad; iOS 14.4; Scale/2.00)");
        headers.put("Accept-Language", "zh-Hans-CN;q=1,en-CN;q=0.9");
        headers.put("kernelplatform", "RN");
        data.put("url", url);
        data.put("headers", headers);
        return data;
    }


    public Map<String, Object> taskGetUrl(String functionId, String body, String cookie) {
        Map<String, Object> data = new HashMap<>(2);
        String url = "https://api.m.jd.com/?appid=activities_platform&functionId=" + functionId + "&body=" + body + "&t=" + System.currentTimeMillis();
        HashMap<String, String> headers = new HashMap<>(16);
        headers.put("Cookie", cookie);
        headers.put("Host", "api.m.jd.com");
        headers.put("Accept", "*/*");
        headers.put("Connection", "keep-alive");
        headers.put("User-Agent", UserAgentUtils.randomUserAgent());
        headers.put("Accept-Language", "zh-Hans-CN;q=1,en-CN;q=0.9");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Referer", "https://an.jd.com/babelDiy/Zeus/q1eB6WUB8oC4eH1BsCLWvQakVsX/index.html");
        data.put("url", url);
        data.put("headers", headers);
        return data;
    }
}
