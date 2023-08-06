package com.lmw.learn.bean.lookup.method;

/**
 * 展示抽象类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 12:58
 */
public abstract class Display {

	public void display() {
		getCar().display();
	}
	public abstract Car getCar();
}
