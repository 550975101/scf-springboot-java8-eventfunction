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
        JsonNode jsonNode = initForFarm(cookie);
        if (!jsonNode.get("farmUserPro").isNull()) {
            String fruitName = jsonNode.get("farmUserPro").get("name").asText();
            System.out.println("水果名称: " + fruitName);
            String shareCode = jsonNode.get("farmUserPro").get("shareCode").asText();
            System.out.println("助力码: " + shareCode);
            System.out.println("将助力码添加至助力池: ");
            shareCodeMap.put(cookie, shareCode);
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
            doDailyTask(jsonNode, cookie);
            //浇水10次
            doTenWater(cookie);
            //领取首次浇水奖励
            getFirstWaterAward(cookie);
        }
    }

    public void getFirstWaterAward(String cookie) {
        JsonNode farmTask = taskInitForFarm(cookie);
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
        }
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
