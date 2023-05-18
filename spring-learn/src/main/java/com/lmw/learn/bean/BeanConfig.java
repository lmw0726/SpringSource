package com.lmw.learn.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

/**
 * 配置类
 *
 * @author MingWei
 * @version 1.0
 * @date 2022/8/17 23:07
 */
@Configuration(value="beanConfig1")
@ComponentScan("com.lmw.learn.bean")
public class BeanConfig {
	@Bean(name = {"a","aa","aaa"})
	public BeanService.A getA(){
		return new BeanService.A();
	}
}
