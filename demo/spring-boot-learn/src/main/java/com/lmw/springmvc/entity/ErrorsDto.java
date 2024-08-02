package com.lmw.springmvc.entity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 空的请求体，只是为了演示使用Errors能够接收到校验异常
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-02 23:08
 */
public class ErrorsDto implements Serializable {
	private static final long serialVersionUID = 308205253392133259L;
	@NotBlank(message = "字符串 s 不能为空")
	private String s;

	@NotNull(message = "数字 i 不能为空")
	private Integer i;

	public String getS() {
		return s;
	}

	public void setS(String s) {
		this.s = s;
	}

	public Integer getI() {
		return i;
	}

	public void setI(Integer i) {
		this.i = i;
	}
}
