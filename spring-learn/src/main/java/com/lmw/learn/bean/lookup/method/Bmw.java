package com.lmw.learn.bean.lookup.method;

/**
 * 汽车接口实现类，宝马类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 12:56
 */
public class Bmw implements Car {
	@Override
	public void display() {
		System.out.println("我是 BMW");
	}
}
