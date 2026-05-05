/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.annotation.Annotation;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * 便捷的 {@link BeanPostProcessor} 实现，委托给 JSR-303 提供者，
 * 对带注解的方法执行方法级校验。
 *
 * <p>适用方法在其参数和/或返回值上具有 JSR-303 约束注解
 * （后一种情况通常在方法级别指定为内联注解），例如：
 *
 * <pre class="code">
 * public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)
 * </pre>
 *
 * <p>具有此类注解方法的目标类需要在类型级别使用 Spring 的
 * {@link Validated} 注解，以便搜索其方法上的内联约束注解。
 * 也可以通过 {@code @Validated} 指定校验组。默认情况下，
 * JSR-303 只会针对其默认组进行校验。
 *
 * <p>自 Spring 5.0 起，此功能需要 Bean Validation 1.1+ 提供者。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see MethodValidationInterceptor
 * @see javax.validation.executable.ExecutableValidator
 */
@SuppressWarnings("serial")
public class MethodValidationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
		implements InitializingBean {

	private Class<? extends Annotation> validatedAnnotationType = Validated.class;

	@Nullable
	private Validator validator;


	/**
	 * 设置 'validated' 注解类型。
	 * 默认 validated 注解类型为 {@link Validated} 注解。
	 * <p>此 setter 属性存在的目的是让开发者可以提供自己的
	 * （非 Spring 特定）注解类型，用于指示类应按应用方法校验的含义
	 * 进行校验。
	 * @param validatedAnnotationType 所需的注解类型
	 */
	public void setValidatedAnnotationType(Class<? extends Annotation> validatedAnnotationType) {
		Assert.notNull(validatedAnnotationType, "'validatedAnnotationType' must not be null");
		this.validatedAnnotationType = validatedAnnotationType;
	}

	/**
	 * 设置用于校验方法的 JSR-303 Validator。
	 * <p>默认值为默认 ValidatorFactory 的默认 Validator。
	 */
	public void setValidator(Validator validator) {
		// 解包为支持 forExecutables 的原生 Validator
		if (validator instanceof LocalValidatorFactoryBean) {
			this.validator = ((LocalValidatorFactoryBean) validator).getValidator();
		}
		else if (validator instanceof SpringValidatorAdapter) {
			this.validator = validator.unwrap(Validator.class);
		}
		else {
			this.validator = validator;
		}
	}

	/**
	 * 设置用于校验方法的 JSR-303 ValidatorFactory，
	 * 使用其默认 Validator。
	 * <p>默认值为默认 ValidatorFactory 的默认 Validator。
	 * @see javax.validation.ValidatorFactory#getValidator()
	 */
	public void setValidatorFactory(ValidatorFactory validatorFactory) {
		this.validator = validatorFactory.getValidator();
	}


	@Override
	public void afterPropertiesSet() {
		Pointcut pointcut = new AnnotationMatchingPointcut(this.validatedAnnotationType, true);
		this.advisor = new DefaultPointcutAdvisor(pointcut, createMethodValidationAdvice(this.validator));
	}

	/**
	 * 为方法校验目的创建 AOP advice，
	 * 与指定 'validated' 注解的 pointcut 一起应用。
	 * @param validator 要委托的 JSR-303 Validator
	 * @return 要使用的拦截器（通常但不一定是 {@link MethodValidationInterceptor}
	 * 或其子类）
	 * @since 4.2
	 */
	protected Advice createMethodValidationAdvice(@Nullable Validator validator) {
		return (validator != null ? new MethodValidationInterceptor(validator) : new MethodValidationInterceptor());
	}

}
