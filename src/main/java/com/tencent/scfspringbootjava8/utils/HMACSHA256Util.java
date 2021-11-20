package com.tencent.scfspringbootjava8.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HMACSHA256Util {

    public static String HMACSHA256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] array = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte item : array) {
            sb.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }


    public static void main(String[] args) throws Exception {
        String str = "lite-android&{\"actId\":\"hbdfw\",\"needGoldToast\":\"true\"}&android&3.1.0&richManIndex&1637401085515&846c4c32dae910ef";
        String salt = "12aea658f76e453faf803d15c40a72e0";
        String s = HMACSHA256(str, salt);
        System.out.println(s);
    }
}
