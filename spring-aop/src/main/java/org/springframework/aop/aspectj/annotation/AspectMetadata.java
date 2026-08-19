/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.TypePatternClassFilter;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.ComposablePointcut;

/**
 * AspectJ 切面类的元数据，带有额外的 Spring AOP 切点
 * 用于 per 子句。
 *
 * <p>使用 AspectJ 5 AJType 反射 API，使我们能够使用不同的
 * AspectJ 实例化模型，如 "singleton"、"pertarget" 和 "perthis"。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.aop.aspectj.AspectJExpressionPointcut
 */
@SuppressWarnings("serial")
public class AspectMetadata implements Serializable {

	/**
	 * 此切面在 Spring 中定义的名称（bean 名称） -
	 * 允许我们确定两条通知是否来自同一个切面，
	 * 从而确定它们的相对优先级。
	 */
	private final String aspectName;

	/**
	 * 切面类，单独存储以便在反序列化时重新解析
	 * 对应的 AjType。
	 */
	private final Class<?> aspectClass;

	/**
	 * AspectJ 反射信息。
	 * <p>在反序列化时重新解析，因为它本身不可序列化。
	 */
	private transient AjType<?> ajType;

	/**
	 * 对应于切面 per 子句的 Spring AOP 切点。
	 * 在 singleton 情况下将是 Pointcut.TRUE 规范实例，
	 * 否则是一个 AspectJExpressionPointcut。
	 */
	private final Pointcut perClausePointcut;


	/**
	 * 为给定的切面类创建新的 AspectMetadata 实例。
	 * @param aspectClass 切面类
	 * @param aspectName 切面的名称
	 */
	public AspectMetadata(Class<?> aspectClass, String aspectName) {
		this.aspectName = aspectName;

		Class<?> currClass = aspectClass;
		AjType<?> ajType = null;
		while (currClass != Object.class) {
			AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
			if (ajTypeToCheck.isAspect()) {
				ajType = ajTypeToCheck;
				break;
			}
			currClass = currClass.getSuperclass();
		}
		if (ajType == null) {
			throw new IllegalArgumentException("Class '" + aspectClass.getName() + "' is not an @AspectJ aspect");
		}
		if (ajType.getDeclarePrecedence().length > 0) {
			throw new IllegalArgumentException("DeclarePrecedence not presently supported in Spring AOP");
		}
		this.aspectClass = ajType.getJavaClass();
		this.ajType = ajType;

		switch (this.ajType.getPerClause().getKind()) {
			case SINGLETON:
				this.perClausePointcut = Pointcut.TRUE;
				return;
			case PERTARGET:
			case PERTHIS:
				AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
				ajexp.setLocation(aspectClass.getName());
				ajexp.setExpression(findPerClause(aspectClass));
				ajexp.setPointcutDeclarationScope(aspectClass);
				this.perClausePointcut = ajexp;
				return;
			case PERTYPEWITHIN:
				// 适用于类型模式（type pattern）
				this.perClausePointcut = new ComposablePointcut(new TypePatternClassFilter(findPerClause(aspectClass)));
				return;
			default:
				throw new AopConfigException(
						"PerClause " + ajType.getPerClause().getKind() + " not supported by Spring AOP for " + aspectClass);
		}
	}

	/**
	 * 从 {@code pertarget(contents)} 形式的字符串中提取内容。
	 */
	private String findPerClause(Class<?> aspectClass) {
		String str = aspectClass.getAnnotation(Aspect.class).value();
		int beginIndex = str.indexOf('(') + 1;
		int endIndex = str.length() - 1;
		return str.substring(beginIndex, endIndex);
	}


	/**
	 * 返回 AspectJ 反射信息。
	 */
	public AjType<?> getAjType() {
		return this.ajType;
	}

	/**
	 * 返回切面类。
	 */
	public Class<?> getAspectClass() {
		return this.aspectClass;
	}

	/**
	 * 返回切面名称。
	 */
	public String getAspectName() {
		return this.aspectName;
	}

	/**
	 * 返回单例切面的 Spring 切点表达式。
	 * （例如，如果是单例，则返回 {@code Pointcut.TRUE}）。
	 */
	public Pointcut getPerClausePointcut() {
		return this.perClausePointcut;
	}

	/**
	 * 返回切面是否定义为 "perthis" 或 "pertarget"。
	 */
	public boolean isPerThisOrPerTarget() {
		PerClauseKind kind = getAjType().getPerClause().getKind();
		return (kind == PerClauseKind.PERTARGET || kind == PerClauseKind.PERTHIS);
	}

	/**
	 * 返回切面是否定义为 "pertypewithin"。
	 */
	public boolean isPerTypeWithin() {
		PerClauseKind kind = getAjType().getPerClause().getKind();
		return (kind == PerClauseKind.PERTYPEWITHIN);
	}

	/**
	 * 返回切面是否需要延迟实例化。
	 */
	public boolean isLazilyInstantiated() {
		return (isPerThisOrPerTarget() || isPerTypeWithin());
	}


	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		this.ajType = AjTypeSystem.getAjType(this.aspectClass);
	}

}
