package com.lmw.learn.bean.i18n.staticMessage;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Locale;

/**
 * 静态消息测试
 *
 * @author LaiMingWei
 * @version 1.0.0
 * @date 2024-04-13 9:14
 */
public class StaticMessageSourceTest {
	public static void main(String[] args) {
		// 获取注解配置应用上下文
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(StaticMessageSourceConfig.class);
		// 获取BeanService对象
		MessageSource bean = context.getBean(MessageSource.class);
		String message = bean.getMessage("user.name", null, Locale.CHINA);
		System.out.println(message);
		message = bean.getMessage("user.name", null, Locale.US);
		System.out.println(message);

		message = bean.getMessage("message", new Object[]{1}, Locale.CHINA);
		System.out.println(message);

		message = bean.getMessage("message", new Object[]{2}, Locale.US);
		System.out.println(message);
	}
}
