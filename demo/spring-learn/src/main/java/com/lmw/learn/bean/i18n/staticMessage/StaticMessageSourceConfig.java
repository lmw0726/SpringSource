package com.lmw.learn.bean.i18n.staticMessage;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

/**
 * StaticMessageSource配置
 *
 * @author LaiMingWei
 * @version 1.0.0
 * @date 2024-04-13 9:10
 */
@Configuration
@ComponentScan("com.lmw.learn.bean.i18n.staticMessage")
public class StaticMessageSourceConfig {
	@Bean
	public MessageSource messageSource() {
		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage("user.name", Locale.CHINA, "LMW");
		messageSource.addMessage("user.name", Locale.US, "MWL");
		messageSource.addMessage("message", Locale.CHINA, "您有 {0} 条消息");
		messageSource.addMessage("message", Locale.US, "You have {0} message");
		return messageSource;
	}
}
