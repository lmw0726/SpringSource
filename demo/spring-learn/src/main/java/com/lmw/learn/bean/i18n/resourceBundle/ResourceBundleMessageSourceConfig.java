package com.lmw.learn.bean.i18n.resourceBundle;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

/**
 * ResourceBundleMessageSource配置
 *
 * @author LMW
 * @version 1.0
 * @date 2024-04-13 16:25
 */
@Configuration
@ComponentScan("com.lmw.learn.bean.i18n.resourceBundle")
public class ResourceBundleMessageSourceConfig {
	@Bean
	public MessageSource messageSource(){
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		// 基本包名格式为：文件夹 / properties文件前缀
		messageSource.setBasename("resourceBundle/message");
		// 设置默认编码，否则读取到的中文将会是乱码
		messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
		return messageSource;
	}
}
