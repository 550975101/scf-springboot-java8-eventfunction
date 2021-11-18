package com.tencent.scfspringbootjava8.task;

import com.tencent.scfspringbootjava8.config.CookieConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author by 封心
 * @classname abstract_jd_task
 * @description TODO
 * @date 2021/11/18 11:28
 */
public abstract class abstract_jd_task {

  public abstract void start();

  public abstract void exec(String cookie);

  public void bathExex(List<String> cookies) {
    for (String cookie : cookies) {
      if (StringUtils.hasLength(cookie)) {
        exec(cookie);
      }
    }
  }

}
