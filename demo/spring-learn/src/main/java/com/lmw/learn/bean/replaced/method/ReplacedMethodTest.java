package com.lmw.learn.bean.replaced.method;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 替换方法测试
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 13:38
 */
public class ReplacedMethodTest {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:replace/method/replaceMethod.xml");
		OriginalMethod display = (OriginalMethod) context.getBean("method");
		display.display("参数1");
	}
}
