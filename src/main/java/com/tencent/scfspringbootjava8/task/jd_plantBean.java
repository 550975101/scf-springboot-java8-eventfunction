package com.tencent.scfspringbootjava8.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tencent.scfspringbootjava8.config.CookieConfig;
import com.tencent.scfspringbootjava8.utils.HttpUtils;
import com.tencent.scfspringbootjava8.utils.UserAgentUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author by 封心
 * @classname jd_plantBean
 * @description TODO
 * @date 2021/11/22 15:28
 */
@Component("jd_plantBean")
public class jd_plantBean extends abstract_jd_task {

  static ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private CookieConfig config;

  long num;

  @Scheduled(cron = "1 7-21/2 * * * ?")
  @Override
  public void start() {
    bathExec(config.getList());
  }

  @Override
  public void exec(String cookie) {

  }

  public void jdPlantBean(String cookie) {
    System.out.println("获取任务及基本信息");
    JsonNode plantBeanIndexResult = plantBeanIndex(cookie);
    System.out.println(plantBeanIndexResult);
    if (plantBeanIndexResult.has("errorCode")) {
      String errorCode = plantBeanIndexResult.get("errorCode").asText();
      if ("PB101".equals(errorCode)) {
        System.out.println("活动太火爆了，还是去买买买吧！");
        return;
      }
    }
    Iterator<JsonNode> elements = plantBeanIndexResult.get("data").get("roundList").elements();
    int i = 0;
    while (elements.hasNext()) {
      JsonNode next = elements.next();
      String roundState = next.get("roundState").asText();
      if ("2".equals(roundState)) {
        num = i;
        i++;
      }
    }
    String code = plantBeanIndexResult.get("code").asText();
    boolean data = plantBeanIndexResult.has("data");
    if ("0".equals(code)&&data) {
      String shareUrl = plantBeanIndexResult.get("data").get("jwordShareInfo").get("shareUrl").asText();
      String myPlantUuid = shareUrl.substring(shareUrl.indexOf("plantUuid")+10);
      System.out.println("此号的助力码: " + cookie + " " + myPlantUuid);
    }
  }

  public JsonNode plantBeanIndex(String cookie) {
    try {
      String plantBeanIndex = request("plantBeanIndex", "{}", cookie);
      JsonNode plantBeanIndexResult = objectMapper.readTree(plantBeanIndex);
      return plantBeanIndexResult;
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  public String request(String function_id, String body, String cookie) {
    try {
      Thread.sleep(2000);
      Map<String, Object> params = taskUrl(function_id, body, cookie);
      String res = HttpUtils.doPost((String) params.get("url"), (Map<String, String>) params.get("headers"), (String) params.get("body"));
      return res;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Map<String, Object> taskUrl(String function_id, String body, String cookie) {
    try {
      Map<String, Object> data = new HashMap<>(4);
      ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(body);
      jsonNode.put("version", "9.2.4.0");
      jsonNode.put("monitor_source", "plant_app_plant_index");
      jsonNode.put("monitor_refer", "");
      String url = "https://api.m.jd.com/client.action";
      body = "functionId=" + function_id + "&body=" + URLEncoder.encode(jsonNode.toString()) + "&appid=ld&client=apple&area=19_1601_50258_51885&build=167490&clientVersion=9.3.2";
      HashMap<String, String> headers = new HashMap<>(16);
      headers.put("Cookie", cookie);
      headers.put("Host", "api.m.jd.com");
      headers.put("Accept", "*/*");
      headers.put("Connection", "keep-alive");
      headers.put("User-Agent", UserAgentUtils.randomUserAgent());
      headers.put("Accept-Language", "zh-Hans-CN;q=1,en-CN;q=0.9");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      data.put("url", url);
      data.put("headers", headers);
      data.put("body", body);
      return data;
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }
}
