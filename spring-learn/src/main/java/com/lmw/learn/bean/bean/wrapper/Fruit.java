package com.lmw.learn.bean.bean.wrapper;

import java.util.Map;

/**
 * 水果类
 *
 * @author LMW
 * @version 1.0
 * @date 2024-02-29 23:23
 */
public class Fruit {
	/**
	 * 颜色
	 */
	private String color;
	/**
	 * 大小
	 */
	private String size;

	/**
	 * 其他属性
	 */
	private Map<String, String> other;

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public Map<String, String> getOther() {
		return other;
	}

	public void setOther(Map<String, String> other) {
		this.other = other;
	}
}
