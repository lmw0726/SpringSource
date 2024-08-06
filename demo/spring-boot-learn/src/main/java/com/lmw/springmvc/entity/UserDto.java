package com.lmw.springmvc.entity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 用户请求模型
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-05 22:58
 */
public class UserDto implements Serializable {
	private static final long serialVersionUID = 1697469902030409109L;

	@NotBlank(message = "名称不能为空")
	private String name;
	@NotNull(message = "年龄不能为空")
	private Integer age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "UserDto{" +
				"name='" + name + '\'' +
				", age=" + age +
				'}';
	}
}
