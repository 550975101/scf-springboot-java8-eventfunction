package com.tencent.scfspringbootjava8.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author by 封心
 * @classname CookieConfig
 * @description TODO
 * @date 2021/11/18 11:21
 */
@Component
@ConfigurationProperties("jd.cookie")
public class CookieConfig {
  private List<String> list;

  public List<String> getList() {
    return list;
  }

  public void setList(List<String> list) {
    this.list = list;
  }
}
