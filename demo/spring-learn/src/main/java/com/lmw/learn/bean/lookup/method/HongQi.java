package com.lmw.learn.bean.lookup.method;

/**
 * 汽车接口实现类，红旗类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 12:57
 */
public class HongQi implements Car {
	@Override
	public void display() {
		System.out.println("我是红旗");
	}
}
