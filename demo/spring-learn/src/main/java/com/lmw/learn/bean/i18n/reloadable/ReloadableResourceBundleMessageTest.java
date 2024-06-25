package com.lmw.learn.bean.i18n.reloadable;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * ReloadableResourceBundleMessage测试
 *
 * @author LMW
 * @version 1.0
 * @date 2024-04-15 20:55
 */
public class ReloadableResourceBundleMessageTest {
	public static void main(String[] args) throws InterruptedException {
		// 获取注解配置应用上下文
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ReloadableResourceBundleMessageSourceConfig.class);
		// 获取BeanService对象
		MessageSource bean = context.getBean(MessageSource.class);
		String message = bean.getMessage("user.name", null, Locale.CHINA);
		System.out.println(message);
		// 暂停5秒钟，在 reloadableResource/message_zh_CN.properties 添加user.age=18
		TimeUnit.SECONDS.sleep(5L);
		message = bean.getMessage("user.age", null, Locale.CHINA);
		System.out.println(message);
		message = bean.getMessage("user.name", null, Locale.US);
		System.out.println(message);
		TimeUnit.SECONDS.sleep(100L);
		message = bean.getMessage("message", new Object[]{1}, Locale.CHINA);
		System.out.println(message);

		message = bean.getMessage("message", new Object[]{2}, Locale.US);
		System.out.println(message);

	}
}
