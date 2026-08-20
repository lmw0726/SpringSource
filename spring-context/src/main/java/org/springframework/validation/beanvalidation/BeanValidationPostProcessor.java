/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.validation.beanvalidation;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Iterator;
import java.util.Set;

/**
 * 一个简单的 {@link BeanPostProcessor}，用于检查 Spring 管理的 Bean 中的 JSR-303 约束注解，
 * 在调用 Bean 的初始化方法（如果有）之前立即抛出初始化异常（如果存在约束违规）。
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class BeanValidationPostProcessor implements BeanPostProcessor, InitializingBean {

	@Nullable
	private Validator validator;

	private boolean afterInitialization = false;


	/**
	 * 设置用于验证 Bean 的 JSR-303 Validator。
	 * <p>默认使用默认 ValidatorFactory 的默认 Validator。
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * 设置用于验证 Bean 的 JSR-303 ValidatorFactory，使用其默认的 Validator。
	 * <p>默认使用默认 ValidatorFactory 的默认 Validator。
	 * @see javax.validation.ValidatorFactory#getValidator()
	 */
	public void setValidatorFactory(ValidatorFactory validatorFactory) {
		this.validator = validatorFactory.getValidator();
	}

	/**
	 * 选择是在 Bean 初始化之后（即 init 方法之后）执行验证，还是在之前（默认行为）执行验证。
	 * <p>默认为 "false"（初始化之前）。如果希望在受约束的字段被验证之前，
	 * 给初始化方法一个填充这些字段的机会，请将其设置为 "true"（初始化之后）。
	 */
	public void setAfterInitialization(boolean afterInitialization) {
		this.afterInitialization = afterInitialization;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.validator == null) {
			this.validator = Validation.buildDefaultValidatorFactory().getValidator();
		}
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (!this.afterInitialization) {
			doValidate(bean);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (this.afterInitialization) {
			doValidate(bean);
		}
		return bean;
	}


	/**
	 * 对给定的 Bean 执行验证。
	 * @param bean 要验证的 Bean 实例
	 * @see javax.validation.Validator#validate
	 */
	protected void doValidate(Object bean) {
		Assert.state(this.validator != null, "No Validator set");
		Object objectToValidate = AopProxyUtils.getSingletonTarget(bean);
		if (objectToValidate == null) {
			objectToValidate = bean;
		}
		Set<ConstraintViolation<Object>> result = this.validator.validate(objectToValidate);

		if (!result.isEmpty()) {
			StringBuilder sb = new StringBuilder("Bean state is invalid: ");
			for (Iterator<ConstraintViolation<Object>> it = result.iterator(); it.hasNext();) {
				ConstraintViolation<Object> violation = it.next();
				sb.append(violation.getPropertyPath()).append(" - ").append(violation.getMessage());
				if (it.hasNext()) {
					sb.append("; ");
				}
			}
			throw new BeanInitializationException(sb.toString());
		}
	}

}
