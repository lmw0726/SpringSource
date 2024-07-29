package com.lmw.springmvc.entity;

import java.io.Serializable;
import java.util.Map;

/**
 * 带有@RequestParam的Map实体
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-29 21:33
 */
public class RequestParamNameMap implements Serializable {
	private static final long serialVersionUID = 5656452541594951328L;
	/**
	 * 映射
	 */
	private Map<String, Object> map;

	public Map<String, Object> getMap() {
		return map;
	}

	public void setMap(Map<String, Object> map) {
		this.map = map;
	}
}
