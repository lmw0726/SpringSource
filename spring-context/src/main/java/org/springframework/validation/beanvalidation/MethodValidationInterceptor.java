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

import java.lang.reflect.Method;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.annotation.Validated;

/**
 * 一个 AOP Alliance {@link MethodInterceptor} 实现，它委托给
 * JSR-303 提供者来对带注解的方法执行方法级别的验证。
 *
 * <p>适用的方法在其参数和/或返回值上具有 JSR-303 约束注解（在后一种情况下在方法级别指定，
 * 通常作为内联注解）。
 *
 * <p>例如：{@code public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)}
 *
 * <p>验证组可以通过包含目标类的类型级别的 Spring {@link Validated} 注解来指定，
 * 应用于该类的所有公共服务方法。默认情况下，JSR-303 仅针对其默认组进行验证。
 *
 * <p>从 Spring 5.0 开始，此功能需要 Bean Validation 1.1+ 提供者。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see MethodValidationPostProcessor
 * @see javax.validation.executable.ExecutableValidator
 */
public class MethodValidationInterceptor implements MethodInterceptor {

	private final Validator validator;


	/**
	 * 使用底层默认的 JSR-303 验证器创建一个新的 MethodValidationInterceptor。
	 */
	public MethodValidationInterceptor() {
		this(Validation.buildDefaultValidatorFactory());
	}

	/**
	 * 使用给定的 JSR-303 ValidatorFactory 创建一个新的 MethodValidationInterceptor。
	 * @param validatorFactory 要使用的 JSR-303 ValidatorFactory
	 */
	public MethodValidationInterceptor(ValidatorFactory validatorFactory) {
		this(validatorFactory.getValidator());
	}

	/**
	 * 使用给定的 JSR-303 Validator 创建一个新的 MethodValidationInterceptor。
	 * @param validator 要使用的 JSR-303 Validator
	 */
	public MethodValidationInterceptor(Validator validator) {
		this.validator = validator;
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		// 避免对 FactoryBean.getObjectType/isSingleton 进行 Validator 调用
		if (isFactoryBeanMetadataMethod(invocation.getMethod())) {
			return invocation.proceed();
		}

		Class<?>[] groups = determineValidationGroups(invocation);

		// 标准 Bean Validation 1.1 API
		ExecutableValidator execVal = this.validator.forExecutables();
		Method methodToValidate = invocation.getMethod();
		Set<ConstraintViolation<Object>> result;

		Object target = invocation.getThis();
		Assert.state(target != null, "Target must not be null");

		try {
			result = execVal.validateParameters(target, methodToValidate, invocation.getArguments(), groups);
		}
		catch (IllegalArgumentException ex) {
			// 可能是接口和实现之间的泛型类型不匹配，如 SPR-12237 / HV-1011 中报告的
			// 让我们尝试在实现类上查找桥接方法...
			methodToValidate = BridgeMethodResolver.findBridgedMethod(
					ClassUtils.getMostSpecificMethod(invocation.getMethod(), target.getClass()));
			result = execVal.validateParameters(target, methodToValidate, invocation.getArguments(), groups);
		}
		if (!result.isEmpty()) {
			throw new ConstraintViolationException(result);
		}

		Object returnValue = invocation.proceed();

		result = execVal.validateReturnValue(target, methodToValidate, returnValue, groups);
		if (!result.isEmpty()) {
			throw new ConstraintViolationException(result);
		}

		return returnValue;
	}

	private boolean isFactoryBeanMetadataMethod(Method method) {
		Class<?> clazz = method.getDeclaringClass();

		// 从基于接口的代理句柄调用，允许进行高效检查？
		if (clazz.isInterface()) {
			return ((clazz == FactoryBean.class || clazz == SmartFactoryBean.class) &&
					!method.getName().equals("getObject"));
		}

		// 从 CGLIB 代理句柄调用，可能实现了 FactoryBean 方法？
		Class<?> factoryBeanType = null;
		if (SmartFactoryBean.class.isAssignableFrom(clazz)) {
			factoryBeanType = SmartFactoryBean.class;
		}
		else if (FactoryBean.class.isAssignableFrom(clazz)) {
			factoryBeanType = FactoryBean.class;
		}
		return (factoryBeanType != null && !method.getName().equals("getObject") &&
				ClassUtils.hasMethod(factoryBeanType, method));
	}

	/**
	 * 确定给定方法调用要验证的验证组。
	 * <p>默认是方法所在目标类上的 {@link Validated} 注解中指定的验证组。
	 * @param invocation 当前的 MethodInvocation
	 * @return 作为 Class 数组的适用验证组
	 */
	protected Class<?>[] determineValidationGroups(MethodInvocation invocation) {
		Validated validatedAnn = AnnotationUtils.findAnnotation(invocation.getMethod(), Validated.class);
		if (validatedAnn == null) {
			Object target = invocation.getThis();
			Assert.state(target != null, "Target must not be null");
			validatedAnn = AnnotationUtils.findAnnotation(target.getClass(), Validated.class);
		}
		return (validatedAnn != null ? validatedAnn.value() : new Class<?>[0]);
	}

}
