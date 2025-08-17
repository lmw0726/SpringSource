/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

/**
 * 用于解析外部管理的构造函数和方法上可自动装配参数的公共代理。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 5.2
 * @see #isAutowirable
 * @see #resolveDependency
 */
public final class ParameterResolutionDelegate {

	private static final AnnotatedElement EMPTY_ANNOTATED_ELEMENT = new AnnotatedElement() {
		@Override
		@Nullable
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			return null;
		}
		@Override
		public Annotation[] getAnnotations() {
			return new Annotation[0];
		}
		@Override
		public Annotation[] getDeclaredAnnotations() {
			return new Annotation[0];
		}
	};


	private ParameterResolutionDelegate() {
	}


	/**
	 * 判断所提供的 {@link Parameter} 是否 <em>可能</em> 可以从 {@link AutowireCapableBeanFactory} 自动装配。
	 * <p>如果所提供的参数使用了 {@link Autowired @Autowired}、{@link Qualifier @Qualifier} 或 {@link Value @Value} 注解或元注解，则返回 {@code true}。
	 * <p>注意，即使此方法返回 {@code false}，{@link #resolveDependency} 仍然可能能够解析所提供参数的依赖。
	 * @param parameter 需要自动装配依赖的参数（不能为空）
	 * @param parameterIndex 参数在声明该参数的构造函数或方法中的索引
	 * @see #resolveDependency
	 */
	public static boolean isAutowirable(Parameter parameter, int parameterIndex) {
		Assert.notNull(parameter, "Parameter must not be null");
		AnnotatedElement annotatedParameter = getEffectiveAnnotatedParameter(parameter, parameterIndex);
		return (AnnotatedElementUtils.hasAnnotation(annotatedParameter, Autowired.class) ||
				AnnotatedElementUtils.hasAnnotation(annotatedParameter, Qualifier.class) ||
				AnnotatedElementUtils.hasAnnotation(annotatedParameter, Value.class));
	}

	/**
	 * 从提供的 {@link AutowireCapableBeanFactory} 中解析所提供 {@link Parameter} 的依赖。
	 * <p>为单个方法参数提供全面的自动装配支持，其功能与 Spring 对自动装配字段和方法的依赖注入相当，
	 * 包括对 {@link Autowired @Autowired}、{@link Qualifier @Qualifier} 和 {@link Value @Value} 的支持，
	 * 并支持 {@code @Value} 声明中的属性占位符和 SpEL 表达式。
	 * <p>除非参数使用 {@link Autowired @Autowired} 注解或元注解且其 {@link Autowired#required required} 标志设置为 {@code false}，
	 * 否则依赖项是必需的。
	 * <p>如果没有显式声明 <em>限定符</em>，将使用参数名称作为解决歧义的限定符。
	 * @param parameter 需要解析依赖的参数（不能为空）
	 * @param parameterIndex 参数在声明该参数的构造函数或方法中的索引
	 * @param containingClass 包含该参数的具体类；可能与声明参数的类不同，可能是其子类，并可能替换类型变量（不能为空）
	 * @param beanFactory 用于解析依赖的 {@code AutowireCapableBeanFactory}（不能为空）
	 * @return 解析得到的对象，如果未找到则返回 {@code null}
	 * @throws BeansException 如果依赖解析失败
	 * @see #isAutowirable
	 * @see Autowired#required
	 * @see SynthesizingMethodParameter#forExecutable(Executable, int)
	 * @see AutowireCapableBeanFactory#resolveDependency(DependencyDescriptor, String)
	 */
	@Nullable
	public static Object resolveDependency(
			Parameter parameter, int parameterIndex, Class<?> containingClass, AutowireCapableBeanFactory beanFactory)
			throws BeansException {

		Assert.notNull(parameter, "Parameter must not be null");
		Assert.notNull(containingClass, "Containing class must not be null");
		Assert.notNull(beanFactory, "AutowireCapableBeanFactory must not be null");

		AnnotatedElement annotatedParameter = getEffectiveAnnotatedParameter(parameter, parameterIndex);
		Autowired autowired = AnnotatedElementUtils.findMergedAnnotation(annotatedParameter, Autowired.class);
		boolean required = (autowired == null || autowired.required());

		MethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(
				parameter.getDeclaringExecutable(), parameterIndex);
		DependencyDescriptor descriptor = new DependencyDescriptor(methodParameter, required);
		descriptor.setContainingClass(containingClass);
		return beanFactory.resolveDependency(descriptor, null);
	}

	/**
	 * 由于 JDK 9 之前的 {@code javac} 存在的一个 bug，直接在 {@link Parameter} 上查找注解
	 * 会导致内部类构造函数失败。
	 * <h4>JDK &lt; 9 的 javac Bug</h4>
	 * <p>在编译字节码中，参数注解数组会排除内部类构造函数的隐式 <em>外部实例</em> 参数。
	 * <h4>解决方法</h4>
	 * <p>此方法通过允许调用者访问前一个 {@link Parameter} 对象（即 {@code index - 1}）的注解来解决这个偏移一的错误。
	 * 如果提供的 {@code index} 为零，则此方法返回一个空的 {@code AnnotatedElement}。
	 * <h4>警告</h4>
	 * <p>此方法返回的 {@code AnnotatedElement} 不应被强制转换为 {@code Parameter} 使用，
	 * 因为其元数据（如 {@link Parameter#getName()}、{@link Parameter#getType()} 等）可能与内部类构造函数中给定索引的声明参数不匹配。
	 * @return 提供的 {@code parameter} 或者在上述 bug 生效时的 <em>有效</em> {@code Parameter}
	 */
	private static AnnotatedElement getEffectiveAnnotatedParameter(Parameter parameter, int index) {
		Executable executable = parameter.getDeclaringExecutable();
		if (executable instanceof Constructor && ClassUtils.isInnerClass(executable.getDeclaringClass()) &&
				executable.getParameterAnnotations().length == executable.getParameterCount() - 1) {
			// JDK <9 的 javac 中存在 Bug：注解数组会排除内部类的外部实例参数
			// 因此需要将实际参数索引减 1 来访问正确的参数
			return (index == 0 ? EMPTY_ANNOTATED_ELEMENT : executable.getParameters()[index - 1]);
		}
		return parameter;
	}

}
