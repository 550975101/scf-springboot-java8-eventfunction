package com.tencent.scfspringbootjava8.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.scfspringbootjava8.config.CookieConfig;
import com.tencent.scfspringbootjava8.utils.HttpUtils;
import com.tencent.scfspringbootjava8.utils.UserAgentUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.util.*;
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
            apDoTask(taskType, id, 4, taskSourceUrl,cookie);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
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
        System.out.println("执行任务结果: "+res);
      }
    } catch (Exception e) {
      System.out.println("执行任务异常: "+e.getMessage());
      e.printStackTrace();
    }
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
