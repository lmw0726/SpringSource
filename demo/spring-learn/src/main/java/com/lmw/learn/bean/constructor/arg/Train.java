package com.lmw.learn.bean.constructor.arg;

/**
 * 列车类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 14:12
 */
public class Train {
	private String name;
	private Long speed;
	private Integer busload;

	public Train(String name, Long speed, Integer busload) {
		this.name = name;
		this.speed = speed;
		this.busload = busload;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getSpeed() {
		return speed;
	}

	public void setSpeed(Long speed) {
		this.speed = speed;
	}

	public Integer getBusload() {
		return busload;
	}

	public void setBusload(Integer busload) {
		this.busload = busload;
	}

	@Override
	public String toString() {
		return "Train{" +
				"name='" + name + '\'' +
				", speed=" + speed +
				", busload=" + busload +
				'}';
	}
}
