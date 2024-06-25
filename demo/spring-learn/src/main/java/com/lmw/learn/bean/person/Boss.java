package com.lmw.learn.bean.person;

import com.lmw.learn.bean.Person;

import java.math.BigDecimal;

/**
 * 老板类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/06/05 22:20
 */
public class Boss extends Person {
	/**
	 * 公司名
	 */
	private String companyName;
	/**
	 * 资产
	 */
	private BigDecimal asset;

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public BigDecimal getAsset() {
		return asset;
	}

	public void setAsset(BigDecimal asset) {
		this.asset = asset;
	}

	@Override
	public String toString() {
		return "Boss{" +'\'' +
				"age=" + super.getAge() + '\'' +
				", name='" + super.getName() + '\'' +
				", companyName='" + companyName + '\'' +
				", asset=" + asset +
				'}';
	}
}
