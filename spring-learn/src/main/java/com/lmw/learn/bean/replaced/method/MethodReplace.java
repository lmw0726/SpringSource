package com.lmw.learn.bean.replaced.method;

import org.springframework.beans.factory.support.MethodReplacer;

import java.lang.reflect.Method;

/**
 * 将要替换的方法
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 13:40
 */
public class MethodReplace implements MethodReplacer {
	@Override
	public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
		System.out.println("我是替换方法");
		System.out.println("接受到的参数为：" + args[0]);
		return null;
	}
}
