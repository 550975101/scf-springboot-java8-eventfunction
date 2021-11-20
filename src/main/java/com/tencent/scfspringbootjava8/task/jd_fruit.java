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
    public CookieConfig config;


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
        System.out.println("被水滴砸中： " + (canPop ? "是" : "fou"));
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
            doFriendsWater(cookie);
        }
    }

    ////给好友浇水
    public void doFriendsWater(String cookie) {
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
                //;//为他人助力,接受邀请成为别人的好友

            }
        }
    }

    //接收成为对方好友的邀请
    public void receiveFriendInvite() {

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
