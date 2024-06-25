package com.lmw.learn.bean.properties;

/**
 * 飞机类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 15:19
 */
public class Flight {
	private String name;
	private String type;
	private long speed;
	private int busload;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getSpeed() {
		return speed;
	}

	public void setSpeed(long speed) {
		this.speed = speed;
	}

	public int getBusload() {
		return busload;
	}

	public void setBusload(int busload) {
		this.busload = busload;
	}

	@Override
	public String toString() {
		return "Flight{" +
				"name='" + name + '\'' +
				", type='" + type + '\'' +
				", speed=" + speed +
				", busload=" + busload +
				'}';
	}
}
