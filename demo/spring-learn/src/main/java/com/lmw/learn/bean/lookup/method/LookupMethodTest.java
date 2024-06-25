package com.lmw.learn.bean.lookup.method;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * LookupMethod测试类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 12:55
 */
@Configurable()
public class LookupMethodTest {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:replace/method/replaceMethod.xml");
		Display display = (Display) context.getBean("display");
		display.display();
	}
}
