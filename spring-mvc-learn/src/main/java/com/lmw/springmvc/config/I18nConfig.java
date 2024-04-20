package com.lmw.springmvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 国际化配置
 *
 * @author LaiMingWei
 * @version 1.0.0
 * @date 2022/8/11 14:39
 */
@EnableWebMvc
@Configuration
public class I18nConfig {

	/**
	 * 注册本地化变更拦截器
	 *
	 * @return 本地化变更拦截器
	 */
	@Bean
	public LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
		resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
		return resolver;
	}

	@Bean
	public ResourceBundleMessageSource messageSource() {
		ResourceBundleMessageSource mes = new ResourceBundleMessageSource();
		mes.setBasenames("classpath:messages/message"
				,"classpath:ValidationMessages"
		);
		mes.setUseCodeAsDefaultMessage(false);
		mes.setDefaultEncoding(StandardCharsets.UTF_8.name());
		return mes;
	}


	@Bean
	public LocalValidatorFactoryBean localValidatorFactoryBean(){
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
//		validator.setProviderClass(HibernateValidator.class);
		validator.setValidationMessageSource(messageSource());
		return validator;
	}
}
