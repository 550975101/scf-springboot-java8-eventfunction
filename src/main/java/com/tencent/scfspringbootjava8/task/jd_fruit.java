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
import java.util.HashMap;
import java.util.Map;

/**
 * @author by 封心
 * @classname jd_fruit
 * @description TODO
 * @date 2021/11/19 10:30
 * 更新时间：2021-11-7
 * 活动入口：京东APP我的-更多工具-东东农场
 * 东东农场活动链接：https://h5.m.jd.com/babelDiy/Zeus/3KSjXqQabiTuD1cJ28QskrpWoBKT/index.html
 * 一天只能帮助3个人。多出的助力码无效
 */
@Component("jd_fruit")
public class jd_fruit extends abstract_jd_task {

  static ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private CookieConfig config;


  @Scheduled(cron = "5 6-18/6 * * * ?")
  @Override
  public void start() {
    bathExec(config.getList());
  }

  @Override
  public void exec(String cookie) {

  }

  public void jdFruit(String cookie) {
    JsonNode jsonNode = initForFarm(cookie);
    if (!jsonNode.get("farmUserPro").isNull()) {
      String fruitName = jsonNode.get("farmUserPro").get("name").asText();
      System.out.println("水果名称: " + fruitName);
      String shareCode = jsonNode.get("farmUserPro").get("shareCode").asText();
      System.out.println("助力码: " + shareCode);
      long winTimes = jsonNode.get("farmUserPro").get("winTimes").asLong();
      System.out.println("已成功兑换水果: " + winTimes);
      masterHelpShare(jsonNode);
      long treeState = jsonNode.get("treeState").asLong();
      if (treeState == 2 || treeState == 3) {
        System.out.println("已成熟可以领取了");
      } else if (treeState == 1) {
        System.out.println("种植中...");
      } else if (treeState == 0) {
        System.out.println("您忘了种植新的水果");
      }
      //TODO
    }
  }

  //助力好友  暂时是从 自己配置的码助力 先不搞
  public void masterHelpShare(JsonNode jsonNode) {
    int salveHelpAddWater = 0;
    //今日剩余助力次数,默认3次（京东农场每人每天3次助力机会）。
    int remainTimes = 3;

  }

  //日常任务
  public void doDailyTask(JsonNode jsonNode, String cookie) {
    JsonNode farmTask = taskInitForFarm(cookie);

  }

  public JsonNode taskInitForFarm(String cookie) {
    Map<String, Object> taskInitForFarm = taskGetUrl("taskInitForFarm", "{\"version\": 14, \"channel\": 1, \"babelChannel\": \"120\"}", cookie);
    try {
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode jsonNode = objectMapper.readTree(res);
      return jsonNode;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 初始化农场, 可获取果树及用户信息API
   *
   * @return
   */
  public JsonNode initForFarm(String cookie) {
    String url = "https://api.m.jd.com/client.action?functionId=initForFarm";
    String body = "{\"version\": 4}";
    body = "body=" + URLEncoder.encode(body) + "&appid=wh5&clientVersion=9.1.0";
    HashMap<String, String> headers = new HashMap<>(16);
    headers.put("accept", "*/*");
    headers.put("accept-encoding", "gzip, deflate, br");
    headers.put("accept-language", "zh-CN,zh;q=0.9");
    headers.put("cache-control", "no-cache");
    headers.put("Cookie", cookie);
    headers.put("origin", "https://home.m.jd.com");
    headers.put("pragma", "no-cache");
    headers.put("referer", "https://home.m.jd.com/myJd/newhome.action");
    headers.put("sec-fetch-dest", "sec-fetch-dest");
    headers.put("sec-fetch-mode", "cors");
    headers.put("sec-fetch-site", "same-site");
    headers.put("User-Agent", UserAgentUtils.randomUserAgent());
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    try {
      String farmInfo = HttpUtils.doPost(url, headers, body);
      System.out.println(farmInfo);
      return objectMapper.readTree(farmInfo);
    } catch (Exception e) {
      System.out.println("获取信息异常: " + e.getMessage());
      e.printStackTrace();
    }
    return null;
  }


  public Map<String, Object> taskGetUrl(String functionId, String body, String cookie) {
    Map<String, Object> data = new HashMap<>(2);
    String url = "https://api.m.jd.com/client.action/functionId=" + functionId + "&body=" + body + "&appid=wh5";
    HashMap<String, String> headers = new HashMap<>(16);
    headers.put("Host", "api.m.jd.com");
    headers.put("Accept", "*/*");
    headers.put("Origin", "https://carry.m.jd.com");
    headers.put("Accept-Encoding", "gzip, deflate, br");
    headers.put("User-Agent", UserAgentUtils.randomUserAgent());
    headers.put("Accept-Language", "zh-Hans-CN;q=1,en-CN;q=0.9");
    headers.put("Referer", "https://carry.m.jd.com/");
    headers.put("Cookie", cookie);
    data.put("url", url);
    data.put("headers", headers);
    return data;
  }

}
