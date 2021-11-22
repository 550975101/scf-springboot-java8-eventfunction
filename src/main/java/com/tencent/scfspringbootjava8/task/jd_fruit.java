package com.tencent.scfspringbootjava8.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.scfspringbootjava8.config.CookieConfig;
import com.tencent.scfspringbootjava8.utils.HttpUtils;
import com.tencent.scfspringbootjava8.utils.TaskFlag;
import com.tencent.scfspringbootjava8.utils.UserAgentUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

  Map<String, String> shareCodeMap = new ConcurrentHashMap<>();

  @Autowired
  public CookieConfig config;

  ////农场使用水滴换豆卡(如果出现限时活动时100g水换20豆,此时比浇水划算,推荐换豆),true表示换豆(不浇水),false表示不换豆(继续浇水),脚本默认是浇水
  Boolean jdFruitBeanCard = false;


  @Scheduled(cron = "5 6-18/6 * * * ?")
  @Override
  public void start() {
    bathExec(config.getList());
  }

  @Override
  public void exec(String cookie) {
    jdFruit(cookie);
  }

  public void jdFruit(String cookie) {
    JsonNode farmInfo = initForFarm(cookie);
    if (!farmInfo.get("farmUserPro").isNull()) {
      String fruitName = farmInfo.get("farmUserPro").get("name").asText();
      System.out.println("水果名称: " + fruitName);
      String shareCode = farmInfo.get("farmUserPro").get("shareCode").asText();
      System.out.println("助力码: " + shareCode);
      System.out.println("将助力码添加至助力池: ");
      shareCodeMap.put(cookie, shareCode);
      long winTimes = farmInfo.get("farmUserPro").get("winTimes").asLong();
      System.out.println("已成功兑换水果: " + winTimes);
      masterHelpShare(farmInfo);
      long treeState = farmInfo.get("treeState").asLong();
      if (treeState == 2 || treeState == 3) {
        System.out.println("已成熟可以领取了");
      } else if (treeState == 1) {
        System.out.println("种植中...");
      } else if (treeState == 0) {
        System.out.println("您忘了种植新的水果");
      }
      //TODO
      doDailyTask(farmInfo, cookie);
      //浇水10次
      doTenWater(cookie);
      //领取首次浇水奖励
      getFirstWaterAward(cookie);
      ////领取10浇水奖励
      getTenWaterAward(cookie);
      //领取为2好友浇水奖励
      getWaterFriendGotAward(cookie);
      //鸭子任务
      duck(cookie);
      //预测水果成熟时间
      predictionFruit(cookie);
    }
  }

  public void predictionFruit(String cookie) {
    System.out.println("开始预测水果成熟时间");
    JsonNode farmInfo = initForFarm(cookie);
    JsonNode farmTask = taskInitForFarm(cookie);
    long waterEveryDayT = farmTask.get("totalWaterTaskInit").get("totalWaterTaskTimes").asLong();
    System.out.println("今日共浇水" + waterEveryDayT + "次");
    long totalEnergy = farmInfo.get("farmUserPro").get("totalEnergy").asLong();
    System.out.println("剩余 水滴" + totalEnergy + "g");
    long toFlowTimes = farmInfo.get("toFlowTimes").asLong();
    long toFruitTimes = farmInfo.get("toFruitTimes").asLong();
    long treeEnergy = farmInfo.get("farmUserPro").get("treeEnergy").asLong();
    if (toFlowTimes > (treeEnergy / 10)) {
      System.out.println("开花进度,再浇水" + (toFlowTimes - (treeEnergy / 10)) + "次开花");
    } else if (toFruitTimes > (treeEnergy / 10)) {
      System.out.println("结果进度,再浇水" + (toFruitTimes - (treeEnergy / 10)) + "次结果");
    }
    //预测n天后水果课可兑换功能
    long treeTotalEnergy = farmInfo.get("farmUserPro").get("treeTotalEnergy").asLong();
    long treeEnergyNew = farmInfo.get("farmUserPro").get("treeEnergy").asLong();
    long totalEnergyNew = farmInfo.get("farmUserPro").get("totalEnergy").asLong();
    long waterTotalT = (treeTotalEnergy - treeEnergyNew - totalEnergyNew) / 10;
    long waterD = (long) Math.ceil(waterTotalT / waterEveryDayT);
    System.out.println(waterD == 1 ? "明天" : waterD == 2 ? "后天" : waterD + "天之后" + LocalDate.now().plusDays(waterD));
  }

  public void duck(String cookie) {
    for (int i = 0; i < 10; i++) {
      //这里循环十次
      JsonNode duckRes = getFullCollectionReward(cookie);
      String code = duckRes.get("code").asText();
      if ("0".equals(code)) {
        boolean hasLimit = duckRes.get("hasLimit").asBoolean();
        if (!hasLimit) {
          System.out.println("小鸭子游戏:" + duckRes.get("title").asText());
        } else {
          System.out.println(duckRes.get("title").asText());
          break;
        }
      } else if ("10".equals(code)) {
        System.out.println("小鸭子游戏达到上限");
        break;
      }
    }
  }

  public JsonNode getFullCollectionReward(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("getFullCollectionReward", URLEncoder.encode("{\"type\": 2, \"version\": 6, \"channel\": 2}"), cookie);
      String res = HttpUtils.doPost((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"), "{}");
      System.out.println(res);
      JsonNode duckRes = objectMapper.readTree(res);
      return duckRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void getWaterFriendGotAward(String cookie) {
    JsonNode farmTask = taskInitForFarm(cookie);
    long waterFriendCountKey = farmTask.get("waterFriendTaskInit").get("waterFriendCountKey").asLong();
    long waterFriendMax = farmTask.get("waterFriendTaskInit").get("waterFriendMax").asLong();
    long waterFriendSendWater = farmTask.get("waterFriendTaskInit").get("waterFriendSendWater").asLong();
    boolean waterFriendGotAward = farmTask.get("waterFriendTaskInit").get("waterFriendGotAward").asBoolean();
    if (waterFriendCountKey >= waterFriendMax) {
      if (!waterFriendGotAward) {
        JsonNode waterFriendGotAwardRes = waterFriendGotAwardForFarm(cookie);
        System.out.println("给" + waterFriendMax + "好友浇水,奖励" + waterFriendGotAwardRes.get("addWater").asLong() + "g");
      } else {
        System.out.println("给好友浇水的" + waterFriendSendWater + "g水滴奖励已领取");
      }
    } else {
      System.out.println("暂未给" + waterFriendMax + "个好友浇水");
    }
  }

  //领取给3个好友浇水后的奖励水滴API
  private JsonNode waterFriendGotAwardForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("waterFriendGotAwardForFarm", URLEncoder.encode("{\"version\": 4, \"channel\": 1}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode waterFriendGotAwardRes = objectMapper.readTree(res);
      return waterFriendGotAwardRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void getTenWaterAward(String cookie) {
    JsonNode farmTask = taskInitForFarm(cookie);
    boolean b = farmTask.get("totalWaterTaskInit").get("f").asBoolean();
    long totalWaterTaskTimes = farmTask.get("totalWaterTaskInit").get("totalWaterTaskTimes").asLong();
    long totalWaterTaskLimit = farmTask.get("totalWaterTaskInit").get("totalWaterTaskLimit").asLong();
    if (!b && totalWaterTaskTimes >= totalWaterTaskLimit) {
      JsonNode totalWaterReward = totalWaterTaskForFarm(cookie);
      String code = totalWaterReward.get("code").asText();
      if ("0".equals(code)) {
        System.out.println("十次浇水奖励,获得" + totalWaterReward.get("totalWaterTaskEnergy").asLong() + "g💧");
      } else {
        System.out.println("领取10次浇水奖励结果: " + totalWaterReward);
      }
    } else if (totalWaterTaskTimes < totalWaterTaskLimit) {
      long l = farmTask.get("totalWaterTaskInit").get("totalWaterTaskTimes").asLong();
      System.out.println("十次浇水奖励，任务未完成，今日浇水" + l + "次");
    }
    System.out.println("inished 水果任务完成!");
  }

  //领取10次浇水奖励API
  public JsonNode totalWaterTaskForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("totalWaterTaskForFarm", URLEncoder.encode("{}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode totalWaterReward = objectMapper.readTree(res);
      return totalWaterReward;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void getFirstWaterAward(String cookie) {
    JsonNode farmTask = taskInitForFarm(cookie);
    boolean b = farmTask.get("firstWaterInit").get("f").asBoolean();
    long totalWaterTimes = farmTask.get("firstWaterInit").get("totalWaterTimes").asLong();
    if (!b && totalWaterTimes > 0) {
      JsonNode firstWaterReward = firstWaterTaskForFarm(cookie);
      String code = firstWaterReward.get("code").asText();
      if ("0".equals(code)) {
        System.out.println("首次浇水奖励,获得" + firstWaterReward.get("amount").asLong() + "g💧");
      } else {
        System.out.println("领取首次浇水奖励结果，" + firstWaterReward);
      }
    } else {
      System.out.println("首次浇水奖励已领取");
    }
  }

  public JsonNode firstWaterTaskForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("firstWaterTaskForFarm", URLEncoder.encode("{}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode firstWaterReward = objectMapper.readTree(res);
      return firstWaterReward;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  //// 查询背包道具卡API
  public void doTenWater(String cookie) {
    JsonNode myCardInfoRes = myCardInfoForFarm(cookie);
    long fastCard = myCardInfoRes.get("fastCard").asLong();
    long doubleCard = myCardInfoRes.get("doubleCard").asLong();
    long beanCard = myCardInfoRes.get("beanCard").asLong();
    long signCard = myCardInfoRes.get("signCard").asLong();
    if (jdFruitBeanCard == true && myCardInfoRes.toString().contains("限时翻倍") && beanCard > 0) {
      System.out.println("您设置的是使用水滴换豆卡，且背包有水滴换豆卡" + beanCard + "张, 跳过10次浇水任务");
      return;
    }
    JsonNode farmTask = taskInitForFarm(cookie);
    long totalWaterTaskTimes = farmTask.get("totalWaterTaskInit").get("totalWaterTaskTimes").asLong();
    long totalWaterTaskLimit = farmTask.get("totalWaterTaskInit").get("totalWaterTaskLimit").asLong();
    if (totalWaterTaskTimes < totalWaterTaskLimit) {
      System.out.println("准备浇水10次");
      long waterCount = 0;
      TaskFlag taskFlag = new TaskFlag();
      taskFlag.setFlag(false);
      long times = totalWaterTaskLimit - totalWaterTaskTimes;
      for (long i = 0; i < times; i++) {
        System.out.println("开始第" + i + "次浇水");
        JsonNode waterResult = waterGoodForFarm(cookie);
        System.out.println("本次浇水结果" + waterResult);
        String code = waterResult.get("code").asText();
        if ("0".equals(code)) {
          System.out.println("剩余水滴" + waterResult.get("totalEnergy").asLong() + "");
          boolean finished = waterResult.get("finished").asBoolean();
          if (finished) {
            taskFlag.setFlag(true);
            break;
          } else {
            long totalEnergy = waterResult.get("totalEnergy").asLong();
            if (totalEnergy < 10) {
              System.out.println("水滴不够，结束浇水");
              break;
            }
            //领取阶段性水滴奖励
            gotStageAward(waterResult, cookie);
          }
        } else {
          System.out.println("浇水出现失败异常,跳出不在继续浇水");
          break;
        }
      }
      if (taskFlag.getFlag()) {
        System.out.println("任务完成");
      }
    } else {
      System.out.println("今日已完成10次浇水任务");
    }
  }

  ////领取阶段性水滴奖励
  public void gotStageAward(JsonNode waterResult, String cookie) {
    long waterStatus = waterResult.get("waterStatus").asLong();
    long treeEnergy = waterResult.get("treeEnergy").asLong();
    if (waterStatus == 0 && treeEnergy == 10) {
      System.out.println("果树发芽了,奖励30g水滴");
      //
      JsonNode gotStageAwardForFarmRes = gotStageAwardForFarm("1", cookie);
      System.out.println("浇水阶段奖励1领取结果" + gotStageAwardForFarmRes);
      String code = gotStageAwardForFarmRes.get("code").asText();
      if ("0".equals(code)) {
        System.out.println("果树发芽了,奖励" + gotStageAwardForFarmRes.get("addEnergy").asLong() + "g");
      }
    } else if (waterStatus == 1) {
      System.out.println("果树开花了,奖励40g水滴");
      JsonNode gotStageAwardForFarmRes = gotStageAwardForFarm("2", cookie);
      System.out.println("浇水阶段奖励2领取结果" + gotStageAwardForFarmRes);
      if ("0".equals(gotStageAwardForFarmRes.get("code").asText())) {
        System.out.println("果树开花了,奖励" + gotStageAwardForFarmRes.get("addEnergy").asLong() + "g");
      }
    } else if (waterStatus == 2) {
      System.out.println("果树长出小果子啦, 奖励50g水滴");
      JsonNode gotStageAwardForFarmRes = gotStageAwardForFarm("3", cookie);
      System.out.println("浇水阶段奖励3领取结果" + gotStageAwardForFarmRes);
      if ("0".equals(gotStageAwardForFarmRes.get("code").asText())) {
        System.out.println("果树结果了,奖励" + gotStageAwardForFarmRes.get("addEnergy").asLong() + "g");
      }
    }
  }

  public JsonNode gotStageAwardForFarm(String type, String cookie) {
    try {
      Thread.sleep(1000);
      Map<String, Object> taskInitForFarm = taskGetUrl("gotStageAwardForFarm", URLEncoder.encode("{\"type\": \"" + type + "\"}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode gotStageAwardForFarmRes = objectMapper.readTree(res);
      return gotStageAwardForFarmRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public JsonNode waterGoodForFarm(String cookie) {
    try {
      Thread.sleep(1000);
      Map<String, Object> taskInitForFarm = taskGetUrl("waterGoodForFarm", URLEncoder.encode("{}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode waterResult = objectMapper.readTree(res);
      return waterResult;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public JsonNode myCardInfoForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("myCardInfoForFarm", URLEncoder.encode("{\"version\": 5, \"channel\": 1}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode myCardInfoRes = objectMapper.readTree(res);
      return myCardInfoRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
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
    boolean todaySigned = farmTask.get("signInit").get("todaySigned").asBoolean();
    if (!todaySigned) {
      JsonNode signResult = signForFarm(cookie);
      if (signResult.get("code").asLong() == 0) {
        System.out.println("签到成功: " + signResult.get("amount").asLong() + "g");
      } else {
        System.out.println("签到失败: " + signResult.toString());
      }
    } else {
      long totalSigned = farmTask.get("signInit").get("totalSigned").asLong();
      long signEnergyEachAmount = farmTask.get("signInit").get("signEnergyEachAmount").asLong();
      System.out.println("今天已经签到,连续签到" + totalSigned + ",下次签到可得" + signEnergyEachAmount + "g");
    }
    Boolean canPop = jsonNode.get("todayGotWaterGoalTask").get("canPop").asBoolean();
    System.out.println("被水滴砸中： " + (canPop ? "是" : "否"));
    if (canPop) {
      JsonNode goalResult = gotWaterGoalTaskForFarm(cookie);
      long code = goalResult.get("code").asLong();
      if (code == 0) {
        System.out.println("被水滴砸中，获得" + goalResult.get("addEnergy").asLong() + "g💧");
      }
    }
    System.out.println("签到结束,开始广告浏览任务");
    boolean f = farmTask.get("gotBrowseTaskAdInit").get("f").asBoolean();
    if (!f) {
      Iterator<JsonNode> adverts = farmTask.get("gotBrowseTaskAdInit").get("userBrowseTaskAds").elements();
      long browseReward = 0;
      long browseSuccess = 0;
      long browseFail = 0;
      while (adverts.hasNext()) {
        JsonNode next = adverts.next();
        // //开始浏览广告
        if (next.get("limit").asLong() < next.get("hadFinishedTimes").asLong()) {
          System.out.println(next.get("mainTitle").asText() + "已完成");
          continue;
        }
        System.out.println("正在进行广告浏览任务: " + next.get("mainTitle").asText());
        long advertId = next.get("advertId").asLong();
        JsonNode browseResult = browseAdTaskForFarm(advertId, 0, cookie);
        long code = browseResult.get("code").asLong();
        if (code == 0) {
          System.out.println(next.get("mainTitle").asText() + "浏览任务完成");
          //领取奖励
          JsonNode browseRwardResult = browseAdTaskForFarm(advertId, 1, cookie);
          long code1 = browseRwardResult.get("code").asLong();
          if (code1 == 0) {
            System.out.println(next.get("mainTitle").asText() + "广告奖励成功,获得" + browseRwardResult.get("amount").asLong() + "g");
            browseReward += +browseRwardResult.get("amount").asLong();
            browseSuccess++;
          } else {
            browseFail++;
            System.out.println("领取浏览广告奖励结果: " + browseRwardResult.toString());
          }
        } else {
          browseFail++;
          System.out.println("广告浏览任务结果:  " + browseResult.toString());
        }
      }
      if (browseFail > 0) {
        System.out.println("【广告浏览】完成" + browseSuccess + "个,失败" + browseFail + "获得" + browseReward + "g");
      } else {
        System.out.println("【广告浏览】完成" + browseSuccess + "个,获得" + browseReward + "g");
      }
    } else {
      System.out.println("今天已经做过浏览广告任务");
    }
    ////定时领水
    boolean b = farmTask.get("gotThreeMealInit").get("f").asBoolean();
    if (!b) {
      JsonNode threeMeal = gotThreeMealForFarm(cookie);
      long code = threeMeal.get("code").asLong();
      if (code == 0) {
        System.out.println("定时领水,获得" + threeMeal.get("amount").asLong() + "g");
      } else {
        System.out.println("定时领水失败结果" + threeMeal.toString());
      }
    } else {
      System.out.println("当前不在定时领水时间断或者已经领过");
    }
    //给好友浇水
    boolean b1 = farmTask.get("waterFriendTaskInit").get("f").asBoolean();
    if (!b1) {
      long waterFriendCountKey = farmTask.get("waterFriendTaskInit").get("waterFriendCountKey").asLong();
      long waterFriendMax = farmTask.get("waterFriendTaskInit").get("waterFriendMax").asLong();
      if (waterFriendCountKey < waterFriendMax) {
        doFriendsWater(cookie);
      }
    } else {
      System.out.println("给" + farmTask.get("waterFriendTaskInit").get("waterFriendMax").asLong() + "个好友浇水任务已完成");
    }
    //TODO
    getAwardInviteFriend(cookie);
    clockInIn(cookie);
    ////水滴雨
    executeWaterRains(farmTask, cookie);
    //领取额外水滴奖励
    getExtraAward(cookie);
    //
    turntableFarm(cookie);
  }

  ////天天抽奖活动
  public void turntableFarm(String cookie) {
    JsonNode initForTurntableFarmRes = initForTurntableFarm(cookie);
    String code = initForTurntableFarmRes.get("code").asText();
    if ("0".equals(code)) {
      long timingIntervalHours = initForTurntableFarmRes.get("timingIntervalHours").asLong();
      long timingLastSysTime = initForTurntableFarmRes.get("timingLastSysTime").asLong();
      long sysTime = initForTurntableFarmRes.get("sysTime").asLong();
      boolean timingGotStatus = initForTurntableFarmRes.get("timingGotStatus").asBoolean();
      long remainLotteryTimes = initForTurntableFarmRes.get("remainLotteryTimes").asLong();
      JsonNode turntableInfos = initForTurntableFarmRes.get("turntableInfos");
      if (!timingGotStatus) {
        System.out.println("是否到了领取免费赠送的抽奖机会----" + (sysTime > (timingLastSysTime + 60 * 60 * timingIntervalHours * 1000)));
        if (sysTime > (timingLastSysTime + 60 * 60 * timingIntervalHours * 1000)) {
          JsonNode timingAwardRes = timingAwardForTurntableFarm(cookie);
          System.out.println("领取定时奖励结果" + timingAwardRes);
          initForTurntableFarmRes = initForTurntableFarm(cookie);
          remainLotteryTimes = initForTurntableFarmRes.get("remainLotteryTimes").asLong();
        } else {
          System.out.println("免费赠送的抽奖机会未到时间");
        }
      } else {
        System.out.println("4小时候免费赠送的抽奖机会已领取");
      }
      JsonNode turntableBrowserAds = initForTurntableFarmRes.get("turntableBrowserAds");
      if (!turntableBrowserAds.isNull() && turntableBrowserAds.size() > 0) {
        Iterator<JsonNode> elements = turntableBrowserAds.elements();
        while (elements.hasNext()) {
          JsonNode next = elements.next();
          boolean status = next.get("status").asBoolean();
          if (!status) {
            System.out.println("开始浏览天天抽奖个逛会场任务");
            long adId = next.get("adId").asLong();
            JsonNode browserForTurntableFarmRes = browserForTurntableFarm(1, adId, cookie);
            String code1 = browserForTurntableFarmRes.get("code").asText();
            if ("0".equals(code1)) {
              System.out.println("逛会场任务领取水滴奖励完成");
              initForTurntableFarmRes = initForTurntableFarm(cookie);
              remainLotteryTimes = initForTurntableFarmRes.get("remainLotteryTimes").asLong();
            }
          } else {
            System.out.println("浏览天天抽奖的第...个逛会场任务已完成");
          }
        }
      }
      // //天天抽奖助力
      System.out.println("开始天天抽奖--好友助力--每人每天只有三次助力机会.");
      shareCodeMap.forEach((k, v) -> {
        if (!k.equals(cookie)) {
          JsonNode lotteryMasterHelpRes = lotteryMasterHelp(v, cookie);
          String helpCode = lotteryMasterHelpRes.get("helpResult").get("code").asText();
          if ("0".equals(helpCode)) {
            System.out.println("天天抽奖-助力" + lotteryMasterHelpRes.get("helpResult").get("masterUserInfo").get("nickName").asText() + "成功");
          } else if ("11".equals(helpCode)) {
            System.out.println("天天抽奖-不要重复助力" + lotteryMasterHelpRes.get("helpResult").get("masterUserInfo").get("nickName").asText());
          } else if ("13".equals(helpCode)) {
            System.out.println("天天抽奖-助力$" + lotteryMasterHelpRes.get("helpResult").get("masterUserInfo").get("nickName").asText() + "失败,助力次数耗尽");
          }
        }
      });
      System.out.println("天天抽奖次数remainLotteryTimes" + remainLotteryTimes);
      ////抽奖
      if (remainLotteryTimes > 0) {
        System.out.println("开始抽奖");
        for (long i = 0; i < remainLotteryTimes; i++) {
          JsonNode lotteryRes = lotteryForTurntableFarm(cookie);
          System.out.println("第" + (i + 1) + "次抽奖结果" + lotteryRes);
          String code1 = lotteryRes.get("code").asText();
          if ("0".equals(code1)) {
            Iterator<JsonNode> elements = turntableInfos.elements();
            while (elements.hasNext()) {
              JsonNode next = elements.next();
              String type = next.get("type").asText();
              String type1 = lotteryRes.get("type").asText();
              if (type.equals(type1)) {
                System.out.println("lotteryRes.type： " + type1);
                System.out.println(next.get("name").asText());
              }
            }
            long remainLotteryTimes1 = lotteryRes.get("remainLotteryTimes").asLong();
            if (remainLotteryTimes1 == 0) {
              break;
            }
          }
        }
      } else {
        System.out.println("天天抽奖--抽奖机会为0次");
      }
    } else {
      System.out.println("初始化天天抽奖得好礼失败");
    }
  }

  public JsonNode lotteryForTurntableFarm(String cookie) {
    try {
      Thread.sleep(2000);
      System.out.println("等待了2秒");
      Map<String, Object> taskInitForFarm = taskGetUrl("lotteryForTurntableFarm", URLEncoder.encode("{\"type\":1,\"version\":4,\"channel\":1}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode lotteryRes = objectMapper.readTree(res);
      return lotteryRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  //天天抽奖拿好礼-助力API(每人每天三次助力机会)
  public JsonNode lotteryMasterHelp(String code, String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("initForFarm", URLEncoder.encode("{\"imageUrl\":\"\",\"nickName\":\"\",\"shareCode\":\"" + code + "-3\",\"babelChannel\":\"3\",\"version\":4,\"channel\":1}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode lotteryMasterHelpRes = objectMapper.readTree(res);
      return lotteryMasterHelpRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public JsonNode browserForTurntableFarm(long type, long adId, String cookie) {
    if (type == 1) {
      System.out.println("浏览爆品会场");
    }
    if (type == 2) {
      System.out.println("天天抽奖浏览任务领取水滴");
    }
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("browserForTurntableFarm", URLEncoder.encode("{\"type\": " + type + ", \"adId\": " + adId + ", \"version\": 4, \"channel\": 1}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode browserForTurntableFarmRes = objectMapper.readTree(res);
      return browserForTurntableFarmRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public JsonNode timingAwardForTurntableFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("timingAwardForTurntableFarm", URLEncoder.encode("{\"version\":4,\"channel\":1}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode timingAwardRes = objectMapper.readTree(res);
      return timingAwardRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  // 初始化集卡抽奖活动数据API
  public JsonNode initForTurntableFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("initForTurntableFarm", URLEncoder.encode("{\"version\":4,\"channel\":1}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode initForTurntableFarmRes = objectMapper.readTree(res);
      return initForTurntableFarmRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void getExtraAward(String cookie) {
    JsonNode farmAssistResult = farmAssistInit(cookie);
    String code = farmAssistResult.get("code").asText();
    if ("0".equals(code)) {
      JsonNode assistFriendList = farmAssistResult.get("assistFriendList");
      if (!assistFriendList.isNull() && assistFriendList.size() >= 2) {
        long status = farmAssistResult.get("status").asLong();
        if (status == 2) {
          long num = 0;
          Iterator<JsonNode> elements = assistFriendList.elements();
          while (elements.hasNext()) {
            JsonNode next = elements.next();
            long stageStaus = next.get("stageStaus").asLong();
            if (stageStaus == 2) {
              JsonNode receiveStageEnergy = receiveStageEnergy(cookie);
              String code1 = receiveStageEnergy.get("code").asText();
              if ("0".equals(code1)) {
                System.out.println("领取" + receiveStageEnergy.get("amount").asLong() + "g水");
                num += receiveStageEnergy.get("amount").asLong();
              }
            }
          }
          System.out.println("额外奖励" + num + "g水领取成功");
        } else if (status == 3) {
          System.out.println("已经领取过8好友助力额外奖励");
        }
      } else {
        System.out.println("助力好友未达到2个");
      }
      if (!assistFriendList.isNull() && assistFriendList.size() > 0) {
        Iterator<JsonNode> elements = assistFriendList.elements();
        while (elements.hasNext()) {
          JsonNode next = elements.next();
          String name = next.get("name").asText();
          LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(next.get("time").asLong()), ZoneId.systemDefault());
          System.out.println("京东昵称" + name + "在" + localDateTime + "给您助过力");
        }
      }
      System.out.println("领取额外奖励水滴结束");
    } else {
      JsonNode masterHelpResult = masterHelpTaskInitForFarm(cookie);
      String code1 = masterHelpResult.get("code").asText();
      if ("0".equals(code1)) {
        JsonNode masterHelpPeoples = masterHelpResult.get("masterHelpPeoples");
        if (!masterHelpPeoples.isNull() && masterHelpPeoples.size() >= 5) {
          // 已有五人助力。领取助力后的奖励
          boolean masterGotFinal = masterHelpResult.get("masterGotFinal").asBoolean();
          if (!masterGotFinal) {
            JsonNode masterGotFinished = masterGotFinishedTaskForFarm(cookie);
            String code2 = masterGotFinished.get("code").asText();
            if ("0".equals(code2)) {
              System.out.println("已成功领取好友助力奖励：" + masterGotFinished.get("amount").asLong() + "g水");
            }
          } else {
            System.out.println("已经领取过5好友助力额外奖励");
          }
        } else {
          System.out.println("助力好友未达到5个");
        }
        if (!masterHelpPeoples.isNull() && masterHelpPeoples.size() > 0) {
          Iterator<JsonNode> elements = masterHelpPeoples.elements();
          while (elements.hasNext()) {
            JsonNode next = elements.next();
            String name = next.get("name").asText();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(next.get("time").asLong()), ZoneId.systemDefault());
            System.out.println("京东昵称" + name + "在" + localDateTime + "给您助过力");
          }
        }
        System.out.println("领取额外奖励水滴结束");
      }
    }
  }

  ////领取5人助力后的额外奖励API
  public JsonNode masterGotFinishedTaskForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("masterGotFinishedTaskForFarm", URLEncoder.encode("{}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode masterGotFinished = objectMapper.readTree(res);
      return masterGotFinished;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  ////助力好友信息API
  public JsonNode masterHelpTaskInitForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("masterHelpTaskInitForFarm", URLEncoder.encode("{}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode masterHelpResult = objectMapper.readTree(res);
      return masterHelpResult;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  ////新版领取助力奖励API
  public JsonNode receiveStageEnergy(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("receiveStageEnergy", URLEncoder.encode("{\"version\": 14, \"channel\": 1, \"babelChannel\": \"120\"}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode receiveStageEnergy = objectMapper.readTree(res);
      return receiveStageEnergy;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  //新版助力好友信息API
  public JsonNode farmAssistInit(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("farmAssistInit", URLEncoder.encode("{\"version\": 14, \"channel\": 1, \"babelChannel\": \"120\"}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode farmAssistResult = objectMapper.readTree(res);
      return farmAssistResult;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  ////水滴雨
  public void executeWaterRains(JsonNode farmTask, String cookie) {
    boolean b = !farmTask.get("waterRainInit").get("f").asBoolean();
    if (b) {
      System.out.println("水滴雨任务，每天两次，最多可得10g水滴");
      System.out.println("两次水滴雨任务是否全部完成：" + (farmTask.get("waterRainInit").get("f").asBoolean() ? "是" : "否"));
      if (!farmTask.get("waterRainInit").get("lastTime").isNull()) {
        long time = farmTask.get("waterRainInit").get("lastTime").asLong();
        if (System.currentTimeMillis() < (time + 3 * 60 * 60 * 1000)) {
          b = false;
          long winTimes = farmTask.get("waterRainInit").get("winTimes").asLong();
          long timestamp = time + 3 * 60 * 60 * 1000;
          LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
          System.out.println("第" + (winTimes + 1) + "次水滴雨】未到时间，请" + localDateTime + "再试");
        }
        if (b) {
          System.out.println("开始水滴雨任务,这是第" + (farmTask.get("waterRainInit").get("winTimes").asLong() + 1) + "次，剩余" + (2 - farmTask.get("waterRainInit").get("winTimes").asLong() + 1) + "次");
          JsonNode waterRain = waterRainForFarm(cookie);
          System.out.println("水滴雨waterRain");
          String code = waterRain.get("code").asText();
          if ("0".equals(code)) {
            System.out.println("水滴雨任务执行成功，获得水滴：" + waterRain.get("addEnergy").asLong() + "g");
            System.out.println("第" + (farmTask.get("waterRainInit").get("winTimes").asLong() + 1) + "次水滴雨】获得" + waterRain.get("addEnergy").asLong() + "g水滴");
          }
        }
      }
    } else {
      System.out.println("【水滴雨】已全部完成，获得20g");
    }
  }

  //水滴雨API
  public JsonNode waterRainForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("waterRainForFarm", URLEncoder.encode("{\"type\": 1, \"hongBaoTimes\": 100, \"version\": 3}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode waterRain = objectMapper.readTree(res);
      return waterRain;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  ////打卡领水活动
  public void clockInIn(String cookie) {
    System.out.println("开始打卡领水活动（签到，关注，领券）");
    JsonNode clockInInit = clockInInitForFarm(cookie);
    String code = clockInInit.get("code").asText();
    if ("0".equals(code)) {
      // 签到得水滴
      boolean todaySigned = clockInInit.get("todaySigned").asBoolean();
      if (!todaySigned) {
        System.out.println("开始今日签到");
        JsonNode clockInForFarmRes = clockInForFarm(cookie);
        System.out.println("打卡结果" + clockInForFarmRes);
        String clockCode = clockInForFarmRes.get("code").asText();
        if ("0".equals(clockCode)) {
          System.out.println("第" + clockInForFarmRes.get("signDay").asLong() + "天签到,获得" + clockInForFarmRes.get("amount").asLong() + "g");
          long signDay = clockInForFarmRes.get("signDay").asLong();
          if (signDay == 7) {
            //可以领取惊喜礼包
            System.out.println("开始领取--惊喜礼包38g水滴");
            JsonNode gotClockInGiftRes = gotClockInGift(cookie);
            String giftCode = gotClockInGiftRes.get("code").asText();
            if ("0".equals(giftCode)) {
              System.out.println("惊喜礼包,获得" + gotClockInGiftRes.get("amount").asLong() + "g");
            }
          }
        }
      }
      long totalSigned = clockInInit.get("totalSigned").asLong();
      if (todaySigned && totalSigned == 7) {
        System.out.println("'开始领取--惊喜礼包38g水滴");
        JsonNode gotClockInGiftRes = gotClockInGift(cookie);
        String giftCode = gotClockInGiftRes.get("code").asText();
        if ("0".equals(giftCode)) {
          System.out.println("惊喜礼包,获得" + gotClockInGiftRes.get("amount").asLong() + "g");
        }
      }
      //限时关注得水滴
      //TODO
      if (!clockInInit.get("themes").isNull()) {
        if (clockInInit.get("themes").size() > 0) {
          Iterator<JsonNode> themes = clockInInit.get("themes").elements();
          while (themes.hasNext()) {
            JsonNode next = themes.next();
            boolean hadGot = next.get("hadGot").asBoolean();
            if (!hadGot) {
              System.out.println("关注id" + next.get("id"));
              long id = next.get("id").asLong();
              JsonNode themeStep1 = clockInFollowForFarm(id, "theme", "1", cookie);
              System.out.println("themeStep1--结果: " + themeStep1);
              String themeStep1Code = themeStep1.get("code").asText();
              if ("0".equals(themeStep1Code)) {
                JsonNode themeStep2 = clockInFollowForFarm(id, "theme", "2", cookie);
                System.out.println("themeStep2--结果: " + themeStep2);
                String themeStep2Code = themeStep2.get("code").asText();
                if ("0".equals(themeStep2Code)) {
                  System.out.println("关注" + next.get("name").asText() + "，获得水滴" + themeStep2.get("amount").asLong() + "g");
                }
              }
            }
          }
        }
      }
      //限时领券得水滴
      if (!clockInInit.get("venderCoupons").isNull()) {
        if (clockInInit.get("venderCoupons").size() > 0) {
          Iterator<JsonNode> themes = clockInInit.get("venderCoupons").elements();
          while (themes.hasNext()) {
            JsonNode next = themes.next();
            boolean hadGot = next.get("hadGot").asBoolean();
            if (!hadGot) {
              System.out.println("领券的ID" + next.get("id"));
              long id = next.get("id").asLong();
              JsonNode venderCouponStep1 = clockInFollowForFarm(id, "venderCoupon", "1", cookie);
              System.out.println("venderCouponStep1--结果 " + venderCouponStep1);
              String venderCouponStep1Code = venderCouponStep1.get("code").asText();
              if ("0".equals(venderCouponStep1Code)) {
                JsonNode venderCouponStep2 = clockInFollowForFarm(id, "venderCoupon", "2", cookie);
                System.out.println("venderCouponStep2--结果 " + venderCouponStep2);
                String venderCouponStep2Code = venderCouponStep2.get("code").asText();
                if ("0".equals(venderCouponStep2Code)) {
                  System.out.println("从" + next.get("name").asText() + "领券，获得水滴" + venderCouponStep2.get("amount").asLong() + "g");
                }
              }
            }
          }
        }
      }
      System.out.println("开始打卡领水活动（签到，关注，领券）结束");
    }
  }

  ////关注，领券等API
  public JsonNode clockInFollowForFarm(long id, String type, String step, String cookie) {
    if ("theme".equals(type)) {
      if ("1".equals(step)) {
        try {
          Map<String, Object> taskInitForFarm = taskGetUrl("clockInForFarm", URLEncoder.encode("{\"id\":" + id + ",\"type\":\"" + type + "\",\"step\":\"" + step + "\"}"), cookie);
          String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
          System.out.println(res);
          JsonNode themeStep1 = objectMapper.readTree(res);
          return themeStep1;
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      } else if ("2".equals(step)) {
        try {
          Map<String, Object> taskInitForFarm = taskGetUrl("clockInForFarm", URLEncoder.encode("{\"id\":" + id + ",\"type\":\"" + type + "\",\"step\":\"" + step + "\"}"), cookie);
          String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
          System.out.println(res);
          JsonNode themeStep2 = objectMapper.readTree(res);
          return themeStep2;
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    } else if ("venderCoupon".equals(type)) {
      if ("1".equals(step)) {
        try {
          Map<String, Object> taskInitForFarm = taskGetUrl("clockInForFarm", URLEncoder.encode("{\"id\":" + id + ",\"type\":\"" + type + "\",\"step\":\"" + step + "\"}"), cookie);
          String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
          System.out.println(res);
          JsonNode venderCouponStep1 = objectMapper.readTree(res);
          return venderCouponStep1;
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      } else if ("2".equals(step)) {
        try {
          Map<String, Object> taskInitForFarm = taskGetUrl("clockInForFarm", URLEncoder.encode("{\"id\":" + id + ",\"type\":\"" + type + "\",\"step\":\"" + step + "\"}"), cookie);
          String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
          System.out.println(res);
          JsonNode venderCouponStep2 = objectMapper.readTree(res);
          return venderCouponStep2;
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    }
    return null;
  }

  //领取连续签到7天的惊喜礼包API
  public JsonNode gotClockInGift(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("clockInForFarm", URLEncoder.encode("{\"type\": 2}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode gotClockInGiftRes = objectMapper.readTree(res);
      return gotClockInGiftRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  //连续签到api
  public JsonNode clockInForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("clockInForFarm", URLEncoder.encode("{\"type\": 1}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode clockInForFarmRes = objectMapper.readTree(res);
      return clockInForFarmRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  //打卡领水API
  public JsonNode clockInInitForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("clockInInitForFarm", URLEncoder.encode("{}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode clockInInit = objectMapper.readTree(res);
      return clockInInit;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void doFriendsWater(String cookie) {
    Set<String> needWaterFriends = new HashSet<>();
    JsonNode friendList = friendListInitForFarm(cookie);
    System.out.println("开始给好友浇水");
    JsonNode farmTask = taskInitForFarm(cookie);
    long waterFriendCountKey = farmTask.get("waterFriendTaskInit").get("waterFriendCountKey").asLong();
    long waterFriendMax = farmTask.get("waterFriendTaskInit").get("waterFriendMax").asLong();
    System.out.println("今日已给" + waterFriendCountKey + "个好友浇水");
    if (waterFriendCountKey < waterFriendMax) {
      if (!friendList.get("friends").isNull() && friendList.get("friends").size() > 0) {
        Iterator<JsonNode> friends = friendList.get("friends").elements();
        while (friends.hasNext()) {
          JsonNode next = friends.next();
          long friendState = next.get("friendState").asLong();
          if (friendState == 1) {
            if (needWaterFriends.size() < (waterFriendMax - waterFriendCountKey)) {
              String shareCode = next.get("shareCode").asText();
              needWaterFriends.add(shareCode);
            }
          }
        }
        try {
          System.out.println("需要浇水的好友列表shareCodes" + objectMapper.writeValueAsString(needWaterFriends));
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        long waterFriendsCount = 0;
        String cardInfoStr = "";
        for (String needWaterFriend : needWaterFriends) {
          //为好友浇水
          JsonNode waterFriendForFarmRes = waterFriendForFarm(needWaterFriend, cookie);
          System.out.println("为好友浇水结果: " + waterFriendForFarmRes);
          String code = waterFriendForFarmRes.get("code").asText();
          if ("0".equals(code)) {
            waterFriendsCount++;
            if (!waterFriendForFarmRes.get("code").get("cardInfo").isNull()) {
              System.out.println("为好友浇水获得道具了");
              String type = waterFriendForFarmRes.get("code").get("cardInfo").get("type").asText();
              if ("beanCard".equals(type)) {
                String rule = waterFriendForFarmRes.get("code").get("cardInfo").get("rule").asText();
                System.out.println("水滴换豆卡" + rule);
              } else if ("fastCard".equals(type)) {
                String rule = waterFriendForFarmRes.get("code").get("cardInfo").get("rule").asText();
                System.out.println("快速浇水卡" + rule);
              } else if ("doubleCard".equals(type)) {
                String rule = waterFriendForFarmRes.get("code").get("cardInfo").get("rule").asText();
                System.out.println("水滴翻倍卡" + rule);
              } else if ("signCard".equals(type)) {
                String rule = waterFriendForFarmRes.get("code").get("cardInfo").get("rule").asText();
                System.out.println("加签卡" + rule);
              }
            }
          } else if ("11".equals(code)) {
            System.out.println("水滴不够,跳出浇水");
          }
        }
        System.out.println("好友浇水消耗: " + (waterFriendsCount * 100) + "g");
      } else {
        System.out.println("您的好友列表暂无好友,快去邀请您的好友吧!");
      }
    } else {
      System.out.println("今日已为好友浇水量已达" + waterFriendMax);
    }
  }

  ////为好友浇水API
  public JsonNode waterFriendForFarm(String shareCode, String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("waterFriendForFarm", URLEncoder.encode(" {\"shareCode\": \"" + shareCode + "\", \"version\": 6, \"channel\": 1}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode waterFriendForFarmRes = objectMapper.readTree(res);
      return waterFriendForFarmRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  ////给好友浇水
  public void getAwardInviteFriend(String cookie) {
    //查询好友列表
    JsonNode friendList = friendListInitForFarm(cookie);
    if (friendList != null) {
      System.out.println("今日已邀请好友" + friendList.get("inviteFriendCount").asLong() + "个 / 每日邀请上限" + friendList.get("inviteFriendMax").asLong());
      if (!friendList.get("friends").isNull()) {
        int friendsLength = friendList.get("friends").size();
        System.out.println("开始删除" + friendsLength + "个好友,可拿每天的邀请奖励");
        if (friendsLength > 0) {
          Iterator<JsonNode> friends = friendList.get("friends").elements();
          while (friends.hasNext()) {
            JsonNode next = friends.next();
            System.out.println("开始删除好友: " + next.get("shareCode").asText());
            JsonNode deleteFriendForFarm = deleteFriendForFarm(next.get("shareCode").asText(), cookie);
            if (deleteFriendForFarm != null) {
              long code = deleteFriendForFarm.get("code").asLong();
              if (code == 0) {
                System.out.println("删除好友成功: " + next.get("shareCode").asText());
              }
            }
          }
        }
        //为他人助力,接受邀请成为别人的好友
        receiveFriendInvite(cookie);

      }
    }
  }

  //接收成为对方好友的邀请
  public void receiveFriendInvite(String cookie) {
    shareCodeMap.forEach((k, v) -> {
      if (!k.equals(cookie)) {
        JsonNode inviteFriendRes = inviteFriend(v, cookie);
        if (inviteFriendRes != null) {
          if (!inviteFriendRes.get("helpResult").isNull()) {
            String code = inviteFriendRes.get("helpResult").get("code").asText();
            if ("0".equals(code)) {
              String nickName = inviteFriendRes.get("helpResult").get("masterUserInfo").get("nickName").asText();
              System.out.println("接收邀请成为好友结果成功,您已成为" + nickName + "的好友");
            }
            if ("17".equals(code)) {
              System.out.println("接收邀请成为好友结果失败,对方已是您的好友");
            }
          }
        }
      }
    });
  }

  public JsonNode inviteFriend(String shareCode, String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("initForFarm", URLEncoder.encode("{\"imageUrl\":\"\",\"nickName\":\"\",\"shareCode\":\"" + shareCode + "-inviteFriend\",\"version\":4,\"channel\":2}\n"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode awardInviteFriendRes = objectMapper.readTree(res);
      return awardInviteFriendRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  //领取邀请好友的奖励API
  public JsonNode awardInviteFriendForFarm(JsonNode friends, String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("awardInviteFriendForFarm", URLEncoder.encode("{}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode awardInviteFriendRes = objectMapper.readTree(res);
      return awardInviteFriendRes;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  //删除好友
  public JsonNode deleteFriendForFarm(String shareCode, String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("deleteFriendForFarm", URLEncoder.encode("{ \"shareCode\": \"" + shareCode + "\""), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode jsonNode = objectMapper.readTree(res);
      return jsonNode;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  //获取好友列表
  public JsonNode friendListInitForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("friendListInitForFarm", URLEncoder.encode("{\"version\": 4, \"channel\": 1}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode jsonNode = objectMapper.readTree(res);
      return jsonNode;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public JsonNode gotThreeMealForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("gotThreeMealForFarm", URLEncoder.encode("{}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode jsonNode = objectMapper.readTree(res);
      return jsonNode;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public JsonNode browseAdTaskForFarm(long advertId, long type, String cookie) {
    if (type == 0) {
      try {
        Map<String, Object> taskInitForFarm = taskGetUrl("browseAdTaskForFarm", URLEncoder.encode("{\"advertId\":" + advertId + ",\"type\": " + type + "}"), cookie);
        String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
        System.out.println(res);
        JsonNode jsonNode = objectMapper.readTree(res);
        return jsonNode;
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    } else if (type == 1) {
      try {
        Map<String, Object> taskInitForFarm = taskGetUrl("browseAdTaskForFarm", URLEncoder.encode("{\"advertId\":" + advertId + ",\"type\": " + type + "}"), cookie);
        String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
        System.out.println(res);
        JsonNode jsonNode = objectMapper.readTree(res);
        return jsonNode;
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }
    return null;
  }

  public JsonNode gotWaterGoalTaskForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("gotWaterGoalTaskForFarm", URLEncoder.encode("{\"type\": 3}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode jsonNode = objectMapper.readTree(res);
      return jsonNode;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  //签到
  public JsonNode signForFarm(String cookie) {
    try {
      Map<String, Object> taskInitForFarm = taskGetUrl("signForFarm", URLEncoder.encode("{}"), cookie);
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode jsonNode = objectMapper.readTree(res);
      return jsonNode;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public JsonNode taskInitForFarm(String cookie) {
    Map<String, Object> taskInitForFarm = taskGetUrl("taskInitForFarm", URLEncoder.encode("{\"version\": 14, \"channel\": 1, \"babelChannel\": \"120\"}"), cookie);
    try {
      String res = HttpUtils.doGetHeaders((String) taskInitForFarm.get("url"), (Map<String, String>) taskInitForFarm.get("headers"));
      System.out.println(res);
      JsonNode farmTask = objectMapper.readTree(res);
      return farmTask;
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
    String url = "https://api.m.jd.com/client.action?functionId=" + functionId + "&body=" + body + "&appid=wh5";
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
