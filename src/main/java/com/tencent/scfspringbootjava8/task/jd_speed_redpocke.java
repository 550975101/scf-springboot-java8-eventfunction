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
import java.util.Iterator;
import java.util.Map;

/**
 * @author by 封心
 * @classname jd_speed_redpocke
 * @description '京东极速版红包
 * @date 2021/11/18 10:40
 * 京东极速版红包
 * 自动提现微信现金
 * 更新时间：2021-8-2
 * 活动时间：2021-4-6 至 2021-5-30
 * 活动地址：https://prodev.m.jd.com/jdlite/active/31U4T6S4PbcK83HyLPioeCWrD63j/index.html
 * 活动入口：京东极速版-领红包
 */
@Component("jd_speed_redpocke")
public class jd_speed_redpocke extends abstract_jd_task {

  static ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private CookieConfig config;

  @Scheduled(cron = "20 0,22 * * * ?")
  @Override
  public void start() {
    bathExec(config.getList());
  }

  @Override
  public void exec(String cookie) {
    try {
      invite(cookie);
      //极速版签到提现
      sign(cookie);
      reward_query(cookie);
      for (int i = 0; i < 3; i++) {
        redPacket(cookie);
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      getPacketList(cookie);
      signPrizeDetailList(cookie);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void invite(String cookie) {
    long t = System.currentTimeMillis();
    String url = "https://api.m.jd.com";
    String inviterId = new String[]{"5V7vHE23qh2EkdBHXRFDuA=="}[(int) Math.floor(Math.random() * 1)];
    HashMap<String, String> headers = new HashMap<>(16);
    headers.put("Host", "api.m.jd.com");
    headers.put("accept", "application/json, text/plain, */*");
    headers.put("content-type", "application/x-www-form-urlencoded");
    headers.put("origin", "https://invite-reward.jd.com");
    headers.put("accept-language", "zh-cn");
    headers.put("user-Agent", UserAgentUtils.randomUserAgent());
    headers.put("Referer", "https://invite-reward.jd.com/");
    headers.put("Cookie", cookie);
    String body = "functionId=InviteFriendChangeAssertsService&body={\"method\":\"attendInviteActivity\",\"data\":{\"inviterPin\":\"" + URLEncoder.encode(inviterId) + "\",\"channel\":1,\"token\":\"\",\"frontendInitStatus\":\"\"}}&referer=-1&eid=eidI9b2981202fsec83iRW1nTsOVzCocWda3YHPN471AY78%2FQBhYbXeWtdg%2F3TCtVTMrE1JjM8Sqt8f2TqF1Z5P%2FRPGlzA1dERP0Z5bLWdq5N5B2VbBO&aid=&client=ios&clientVersion=14.4.2&networkType=wifi&fp=-1&uuid=ab048084b47df24880613326feffdf7eee471488&osVersion=14.4.2&d_brand=iPhone&d_model=iPhone10,2&agent=-1&pageClickKey=-1&platform=3&lang=zh_CN&appid=market-task-h5&_t=" + t + "";
    try {
      String res = HttpUtils.doPost(url, headers, body);
      //System.out.println(res);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void sign(String cookie) {
    String url = "https://api.m.jd.com";
    String signLinkId = "9WA12jYGulArzWS7vcrwhw";
    String body = "{\"linkId\":\"" + signLinkId + "\",\"serviceName\":\"dayDaySignGetRedEnvelopeSignService\",\"business\":1}";
    body = "functionId=apSignIn_day&body=" + body + "&_t=" + System.currentTimeMillis() + "&appid=activities_platform";
    HashMap<String, String> headers = new HashMap<>(16);
    headers.put("Cookie", cookie);
    headers.put("Host", "api.m.jd.com");
    headers.put("Origin", "https://daily-redpacket.jd.com");
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept", "*/*");
    headers.put("Connection", "keep-alive");
    headers.put("User-Agent", "jdltapp;iPhone;3.3.2;14.5.1network/wifi;hasUPPay/0;pushNoticeIsOpen/1;lang/zh_CN;model/iPhone13,2;addressid/137923973;hasOCPay/0;appBuild/1047;supportBestPay/0;pv/467.11;apprpd/MyJD_Main;");
    headers.put("Accept-Language", "zh-Hans-CN;q=1, en-CN;q=0.9, zh-Hant-CN;q=0.8");
    headers.put("Referer", "https://daily-redpacket.jd.com/?activityId=" + signLinkId);
    headers.put("Accept-Encoding", "gzip, deflate, br");
    try {
      String res = HttpUtils.doPost(url, headers, body);
      System.out.println(res);
      JsonNode jsonNode = objectMapper.readTree(res);
      long code = jsonNode.get("data").get("retCode").asLong();
      if (code == 0) {
        System.out.println("极速版签到提现：签到成功");
      } else {
        System.out.println("极速版签到提现：签到失败: " + jsonNode.get("data").get("retMessage").asText());
      }
    } catch (Exception e) {
      System.out.println("极速版签到提现：签到异常: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void reward_query(String cookie) {
    String inviter = new String[]{"HXZ60he5XxG8XNUF2LSrZg"}[(int) Math.floor(Math.random() * 1)];
    String[] linkId = {"7ya6o83WSbNhrbYJqsMfFA"};
    Map<String, Object> request = taskGetUrl("spring_reward_query", URLEncoder.encode("{\"inviter\":\"" + inviter + "\",\"linkId\":\"" + linkId[0] + "\"}"), cookie);
    String res = null;
    try {
      res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
      JsonNode jsonNode = objectMapper.readTree(res);
      long code = jsonNode.get("code").asLong();
      if (code == 0) {

      } else {
        System.out.println(jsonNode.get("code").get("errMsg").asText());
      }
    } catch (Exception e) {
      System.out.println("异常: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void redPacket(String cookie) {
    String inviter = new String[]{"HXZ60he5XxG8XNUF2LSrZg"}[(int) Math.floor(Math.random() * 1)];
    String[] linkId = {"7ya6o83WSbNhrbYJqsMfFA"};
    Map<String, Object> request = taskGetUrl("spring_reward_receive", URLEncoder.encode("{\"inviter\":\"" + inviter + "\",\"linkId\":\"" + linkId[0] + "\"}"), cookie);
    String res = null;
    try {
      res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
      System.out.println(res);
      JsonNode jsonNode = objectMapper.readTree(res);
      long code = jsonNode.get("code").asLong();
      if (code == 0) {
        long prizeType = jsonNode.get("data").get("received").get("prizeType").asLong();
        if (prizeType != 1) {
          System.out.println("获得: " + jsonNode.get("data").get("received").get("prizeDesc"));
        } else {
          System.out.println("获得优惠券");
        }
      } else {
        System.out.println(jsonNode.get("errMsg").asText());
      }
    } catch (Exception e) {
      System.out.println("异常: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void getPacketList(String cookie) {
    String[] linkId = {"7ya6o83WSbNhrbYJqsMfFA"};
    Map<String, Object> request = taskGetUrl("spring_reward_list", URLEncoder.encode("{\"pageNum\":1,\"pageSize\":100,\"linkId\":\"" + linkId[0] + "\",\"inviter\":\"\",}"), cookie);
    String res = null;
    try {
      res = HttpUtils.doGetHeaders((String) request.get("url"), (Map<String, String>) request.get("headers"));
      System.out.println(res);
      JsonNode jsonNode = objectMapper.readTree(res);
      long code = jsonNode.get("code").asLong();
      if (code == 0) {
        Iterator<JsonNode> elements = jsonNode.get("data").get("items").elements();
        while (elements.hasNext()) {
          JsonNode next = elements.next();
          long prizeType = next.get("prizeType").asLong();
          if (prizeType == 4) {
            long state = next.get("state").asLong();
            if (state == 0) {
              long id = next.get("id").asLong();
              long poolBaseId = next.get("poolBaseId").asLong();
              long prizeGroupId = next.get("prizeGroupId").asLong();
              long prizeBaseId = next.get("prizeBaseId").asLong();
              System.out.println("去提现: " + next.get("amount"));
              //体现操作
              cashOut(id, poolBaseId, prizeGroupId, prizeBaseId, cookie);
            }
          }
        }
      } else {
        System.out.println(jsonNode.get("errMsg"));
      }
    } catch (Exception e) {
      System.out.println("异常: " + e.getMessage());
      e.printStackTrace();
    }
  }

  //极速版签到提现
  public void signPrizeDetailList(String cookie) {
    String signLinkId = "9WA12jYGulArzWS7vcrwhw";
    String body = "{\"linkId\":\"" + signLinkId + "\",\"serviceName\":\"dayDaySignGetRedEnvelopeSignService\",\"business\":1,\"pageSize\":20,\"page\":1}";
    String url = "https://api.m.jd.com";
    body = "functionId=signPrizeDetailList&body=" + URLEncoder.encode(body) + "&_t=" + System.currentTimeMillis() + "&appid=activities_platform";
    HashMap<String, String> headers = new HashMap<>(16);
    headers.put("Cookie", cookie);
    headers.put("Host", "api.m.jd.com");
    headers.put("Origin", "https://daily-redpacket.jd.com");
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept", "*/*");
    headers.put("Connection", "keep-alive");
    headers.put("User-Agent", "jdltapp;iPhone;3.3.2;14.5.1network/wifi;hasUPPay/0;pushNoticeIsOpen/1;lang/zh_CN;model/iPhone13,2;addressid/137923973;hasOCPay/0;appBuild/1047;supportBestPay/0;pv/467.11;apprpd/MyJD_Main;");
    headers.put("Accept-Language", "zh-Hans-CN;q=1, en-CN;q=0.9, zh-Hant-CN;q=0.8");
    headers.put("Referer", "https://daily-redpacket.jd.com/?activityId=" + signLinkId);
    headers.put("Accept-Encoding", "gzip, deflate, br");
    String res = null;
    try {
      res = HttpUtils.doPost(url, headers, body);
      JsonNode jsonNode = objectMapper.readTree(res);
      long code = jsonNode.get("code").asLong();
      if (code == 0) {
        long dataCode = jsonNode.get("data").get("code").asLong();
        if (dataCode == 0) {
          Iterator<JsonNode> elements = jsonNode.get("data").get("prizeDrawBaseVoPageBean").get("items").elements();
          while (elements.hasNext()) {
            JsonNode next = elements.next();
            long prizeType = next.get("prizeType").asLong();
            long prizeStatus = next.get("prizeStatus").asLong();
            if (prizeType == 4 && prizeStatus == 0) {
              System.out.println("极速版签到提现，去提现: " + next.get("prizeStatus").asDouble());
              long id = next.get("id").asLong();
              long poolBaseId = next.get("poolBaseId").asLong();
              long prizeGroupId = next.get("prizeGroupId").asLong();
              long prizeBaseId = next.get("prizeBaseId").asLong();
              //提现微信操作
              apCashWithDraw(id, poolBaseId, prizeGroupId, prizeBaseId, cookie);
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println(res);
  }

  public void apCashWithDraw(long id, long poolBaseId, long prizeGroupId, long prizeBaseId, String cookie) {
    String signLinkId = "9WA12jYGulArzWS7vcrwhw";
    String body = "{\"linkId\":\"" + signLinkId + "\",\"businessSource\":\"DAY_DAY_RED_PACKET_SIGN\",\"base\":{\"prizeType\":4,\"business\":\"dayDayRedPacket\",\"id\":" + id + ",\"poolBaseId\":" + poolBaseId + ",\"prizeGroupId\":" + prizeGroupId + ",\"prizeBaseId\":" + prizeBaseId + "}}";
    String url = "https://api.m.jd.com";
    body = "functionId=apCashWithDraw&body=" + URLEncoder.encode(body) + "&_t=" + System.currentTimeMillis() + "&appid=activities_platform";
    HashMap<String, String> headers = new HashMap<>(16);
    headers.put("Cookie", cookie);
    headers.put("Host", "api.m.jd.com");
    headers.put("Origin", "https://daily-redpacket.jd.com");
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept", "*/*");
    headers.put("Connection", "keep-alive");
    headers.put("User-Agent", "jdltapp;iPhone;3.3.2;14.5.1network/wifi;hasUPPay/0;pushNoticeIsOpen/1;lang/zh_CN;model/iPhone13,2;addressid/137923973;hasOCPay/0;appBuild/1047;supportBestPay/0;pv/467.11;apprpd/MyJD_Main;");
    headers.put("Accept-Language", "zh-Hans-CN;q=1, en-CN;q=0.9, zh-Hant-CN;q=0.8");
    headers.put("Referer", "https://daily-redpacket.jd.com/?activityId=" + signLinkId);
    headers.put("Accept-Encoding", "gzip, deflate, br");
    try {
      String res = HttpUtils.doPost(url, headers, body);
      JsonNode jsonNode = objectMapper.readTree(res);
      long code = jsonNode.get("code").asLong();
      if (code == 0) {
        String status = jsonNode.get("data").get("status").asText();
        if ("310".equals(status)) {
          System.out.println("极速版签到提现现金成功");
        } else {
          System.out.println("极速版签到提现现金：失败: " + res);
        }
      } else {
        System.out.println("极速版签到提现现金：异常: " + res);
      }
    } catch (Exception e) {
      System.out.println("极速版红包提现,程序异常: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void cashOut(long id, long poolBaseId, long prizeGroupId, long prizeBaseId, String cookie) {
    String[] linkId = {"7ya6o83WSbNhrbYJqsMfFA"};
    String body = "{\"businessSource\":\"SPRING_FESTIVAL_RED_ENVELOPE\",\"base\":{\"id\":" + id + ",\"business\":null,\"poolBaseId\":" + poolBaseId + ",\"prizeGroupId\":" + prizeGroupId + ",\"prizeBaseId\":" + prizeBaseId + ",\"prizeType\":4},\"linkId\":\"" + linkId[0] + "\",\"inviter\":\"\"}";
    Map<String, Object> request = taskPostUrl("apCashWithDraw", body, cookie);
    String url = (String) request.get("url");
    Map<String, String> headers = (Map<String, String>) request.get("headers");
    body = (String) request.get("body");
    String res = null;
    try {
      res = HttpUtils.doPost(url, headers, body);
      JsonNode jsonNode = objectMapper.readTree(res);
      long code = jsonNode.get("code").asLong();
      if (code == 0) {
        String status = jsonNode.get("data").get("status").asText();
        if ("310".equals(status)) {
          System.out.println("提现成功");
        } else {
          System.out.println("提现失败: " + jsonNode.get("data").get("message").asText());
        }
      } else {
        System.out.println("提现异常: " + jsonNode.get("errMsg").asText());
      }
    } catch (Exception e) {
      System.out.println("提现异常: 程序异常" + e.getMessage());
      e.printStackTrace();
    }
  }

  public Map<String, Object> taskGetUrl(String functionId, String body, String cookie) {
    Map<String, Object> data = new HashMap<>();
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

  public Map<String, Object> taskPostUrl(String functionId, String body, String cookie) {
    Map<String, Object> data = new HashMap<>(4);
    String url = "https://api.m.jd.com/";
    body = "appid=activities_platform&functionId=" + functionId + "&body=" + body + "&t=" + System.currentTimeMillis();
    HashMap<String, String> headers = new HashMap<>(16);
    headers.put("Cookie", cookie);
    headers.put("Host", "api.m.jd.com");
    headers.put("Accept", "*/*");
    headers.put("Connection", "keep-alive");
    headers.put("User-Agent", "jdltapp;iPhone;3.3.2;14.3;b488010ad24c40885d846e66931abaf532ed26a5;network/4g;hasUPPay/0;pushNoticeIsOpen/0;lang/zh_CN;model/iPhone11,8;addressid/2005183373;hasOCPay/0;appBuild/1049;supportBestPay/0;pv/220.46;apprpd/;ref/JDLTSubMainPageViewController;psq/0;ads/;psn/b488010ad24c40885d846e66931abaf532ed26a5|520;jdv/0|iosapp|t_335139774|liteshare|CopyURL|1618673222002|1618673227;adk/;app_device/IOS;pap/JA2020_3112531|3.3.2|IOS 14.3;Mozilla/5.0 (iPhone; CPU iPhone OS 14_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148;supportJDSHWK/1 ");
    headers.put("Accept-Language", "zh-Hans-CN;q=1,en-CN;q=0.9");
    headers.put("Accept-Encoding", "gzip, deflate, br");
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Referer", "https://an.jd.com/babelDiy/Zeus/q1eB6WUB8oC4eH1BsCLWvQakVsX/index.html");
    data.put("url", url);
    data.put("headers", headers);
    data.put("body", body);
    return data;
  }
}
