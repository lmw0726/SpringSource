package com.lmw.learn.bean.i18n.reloadable;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

/**
 * ReloadableResourceBundleMessageSource配置
 *
 * @author LMW
 * @version 1.0
 * @date 2024-04-15 20:50
 */
@Configuration
@ComponentScan("com.lmw.learn.bean.i18n.reloadable")
public class ReloadableResourceBundleMessageSourceConfig {
	@Bean
	public MessageSource messageSource(){
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		// 基本包名格式为：文件夹 / properties文件前缀
		messageSource.setBasenames("classpath:reloadableResource/message","file:E:\\Project\\SpringSource\\spring-learn\\src\\main\\resources\\reloadableResource\\message");
		messageSource.setCacheMillis(200L);
		// 设置默认编码，否则读取到的中文将会是乱码
		messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
		return messageSource;
	}
}
