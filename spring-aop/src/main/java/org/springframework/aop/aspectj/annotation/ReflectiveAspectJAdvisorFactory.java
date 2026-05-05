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

package org.springframework.aop.aspectj.annotation;

import org.aopalliance.aop.Advice;
import org.aspectj.lang.annotation.*;
import org.springframework.aop.Advisor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.*;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConvertingComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;
import org.springframework.util.comparator.InstanceComparator;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 工厂：给定遵循 AspectJ 注解语法的 AspectJ 类，
 * 可创建 Spring AOP Advisor，并使用反射调用相应的通知方法。
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory implements Serializable {

	// 排除 @Pointcut 方法
	private static final MethodFilter adviceMethodFilter = ReflectionUtils.USER_DECLARED_METHODS
			.and(method -> (AnnotationUtils.getAnnotation(method, Pointcut.class) == null));

	private static final Comparator<Method> adviceMethodComparator;

	static {
		// 注意：虽然 @After 排在 @AfterReturning 和 @AfterThrowing 之前，
		// 但由于 AspectJAfterAdvice.invoke(MethodInvocation) 会在 `try` 块中
		// 调用 proceed()，并且只会在相应的 `finally` 块中调用 @After 通知方法，
		// 因此 @After 通知方法实际上会在 @AfterReturning 和 @AfterThrowing 方法之后被调用。
		Comparator<Method> adviceKindComparator = new ConvertingComparator<>(
				new InstanceComparator<>(
						Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
				(Converter<Method, Annotation>) method -> {
					AspectJAnnotation<?> ann = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
					return (ann != null ? ann.getAnnotation() : null);
				});
		Comparator<Method> methodNameComparator = new ConvertingComparator<>(Method::getName);
		adviceMethodComparator = adviceKindComparator.thenComparing(methodNameComparator);
	}


	@Nullable
	private final BeanFactory beanFactory;


	/**
	 * 创建新的 {@code ReflectiveAspectJAdvisorFactory}。
	 */
	public ReflectiveAspectJAdvisorFactory() {
		this(null);
	}

	/**
	 * 创建新的 {@code ReflectiveAspectJAdvisorFactory}，将给定的
	 * {@link BeanFactory} 传播到创建的 {@link AspectJExpressionPointcut} 实例，
	 * 用于 bean 切点处理以及一致的 {@link ClassLoader} 解析。
	 * @param beanFactory 要传播的 BeanFactory（可能为 {@code null}）
	 * @since 4.3.6
	 * @see AspectJExpressionPointcut#setBeanFactory
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBeanClassLoader()
	 */
	public ReflectiveAspectJAdvisorFactory(@Nullable BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
		// 获取切面类（@Aspect 标注的类）
		Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
		// 获取切面名称（beanName）
		String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
		// 校验切面类是否合法（比如是否真的是一个切面）
		validate(aspectClass);

		// ===================== 核心设计 =====================
		// 用装饰器包装原始工厂，保证切面实例只创建一次（懒加载单例）
		// 我们需要用装饰器包装 MetadataAwareAspectInstanceFactory，以便它只实例化一次。
		MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory =
				new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);

		// 用于存放最终生成的 Advisor 列表
		List<Advisor> advisors = new ArrayList<>();
		// 遍历切面类中的所有“通知方法”（@Before / @Around / @After 等）
		for (Method method : getAdvisorMethods(aspectClass)) {
			// 将每个方法解析为 Advisor（Spring AOP 核心对象）
			// 这里 declarationOrder 固定为 0（原因见下方说明）
			// 在 Spring Framework 5.2.7 之前，advisors.size() 会作为 declarationOrderInAspect
			// 传递给 getAdvisor(...)，用于表示声明方法列表中的“当前位置”。
			// 然而，自 Java 7 起，“当前位置”不再有效，因为 JDK 不再
			// 按源代码中声明的顺序返回声明方法。
			// 因此，现在我们将通过反射发现的所有通知方法的 declarationOrderInAspect
			// 硬编码为 0，以便支持跨 JVM 启动的可靠通知排序。
			// 具体来说，值 0 与 AspectJPrecedenceComparator.getAspectDeclarationOrder(Advisor)
			// 中使用的默认值一致。
			Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, 0, aspectName);
			// 如果该方法可以生成 Advisor（说明是有效的增强方法）
			if (advisor != null) {
				// 加入结果集合
				advisors.add(advisor);
			}
		}

		// ===================== 特殊处理：延迟实例化切面 =====================
		// 如果存在 Advisor，并且该切面是“懒加载实例化”的（如 perthis / pertarget）
		// 如果是 per target 切面，则发出虚拟的实例化切面。
		if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
			// 创建一个“合成 Advisor”，用于在真正执行前初始化切面实例
			Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
			advisors.add(0, instantiationAdvisor);
		}

		// ===================== 处理 @DeclareParents =====================
		// 遍历切面类的所有字段
		for (Field field : aspectClass.getDeclaredFields()) {
			// 查找是否有 @DeclareParents（引介增强）
			Advisor advisor = getDeclareParentsAdvisor(field);
			// 如果存在，则加入 Advisor 列表
			if (advisor != null) {
				advisors.add(advisor);
			}
		}
		// 返回最终 Advisor 列表
		return advisors;
	}

	private List<Method> getAdvisorMethods(Class<?> aspectClass) {
		// 创建一个集合，用于存放筛选出来的“通知方法”（@Before / @After / @Around 等）
		List<Method> methods = new ArrayList<>();
		// 使用 Spring 的反射工具类，遍历 aspectClass 中的所有方法
		// methods::add → 符合条件的方法就加入到 methods 集合
		// adviceMethodFilter → 过滤器，只保留“通知方法”（即带有 AOP 注解的方法）
		ReflectionUtils.doWithMethods(aspectClass, methods::add, adviceMethodFilter);
		// 如果方法数量大于 1，则需要排序（保证执行顺序稳定）
		if (methods.size() > 1) {
			// 按照 adviceMethodComparator 排序（基于注解类型、优先级等规则）
			methods.sort(adviceMethodComparator);
		}
		// 返回最终筛选并排序后的通知方法列表
		return methods;
	}

	/**
	 * 为给定引介字段构建 {@link org.springframework.aop.aspectj.DeclareParentsAdvisor}。
	 * <p>生成的 Advisor 需要针对目标进行求值。
	 * @param introductionField 要内省的字段
	 * @return Advisor 实例；如果不是 Advisor，则返回 {@code null}
	 */
	@Nullable
	private Advisor getDeclareParentsAdvisor(Field introductionField) {
		// 从字段上获取 @DeclareParents 注解（用于引介增强）
		DeclareParents declareParents = introductionField.getAnnotation(DeclareParents.class);
		// 如果没有该注解，说明这个字段不是“引介字段”
		if (declareParents == null) {
			// 不是引介增强，直接返回 null
			return null;
		}

		// 如果没有指定 defaultImpl（默认实现类）
		if (DeclareParents.class == declareParents.defaultImpl()) {
			// ❌ 抛异常：@DeclareParents 必须指定 defaultImpl
			throw new IllegalStateException("'defaultImpl' attribute must be set on DeclareParents");
		}
		// ===================== 构建引介 Advisor =====================
		// 创建 DeclareParentsAdvisor（引介增强的 Advisor）
		return new DeclareParentsAdvisor(
				introductionField.getType(), declareParents.value(), declareParents.defaultImpl());
	}


	@Override
	@Nullable
	public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
			int declarationOrderInAspect, String aspectName) {
		// ===================== 校验切面类是否合法 =====================
		// 获取切面类，并再次进行校验（是否是合法 @Aspect、是否支持等）
		validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());

		// ===================== 构建切点（Pointcut） =====================
		// 根据当前方法（如 @Before / @Around）解析出切点表达式（AspectJ 表达式）
		AspectJExpressionPointcut expressionPointcut = getPointcut(
				candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
		// 如果该方法不是一个合法的增强方法（没有切点表达式等）
		if (expressionPointcut == null) {
			// 返回 null，表示该方法不能转为 Advisor
			return null;
		}

		// ===================== 构建 Advisor =====================
		// 创建 Advisor（Spring AOP核心对象）
		// 内部包含：
		// - Pointcut（切点）
		// - Advice（增强逻辑）
		// - 切面实例工厂
		// - 排序信息等
		return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
				this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
	}

	@Nullable
	private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
		AspectJAnnotation<?> aspectJAnnotation =
				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
		if (aspectJAnnotation == null) {
			return null;
		}

		AspectJExpressionPointcut ajexp =
				new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
		ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
		if (this.beanFactory != null) {
			ajexp.setBeanFactory(this.beanFactory);
		}
		return ajexp;
	}


	@Override
	@Nullable
	public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {

		Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
		validate(candidateAspectClass);

		AspectJAnnotation<?> aspectJAnnotation =
				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
		if (aspectJAnnotation == null) {
			return null;
		}

		// 如果执行到这里，说明我们有一个 AspectJ 方法。
		// 检查它是否是带 AspectJ 注解的类
		if (!isAspect(candidateAspectClass)) {
			throw new AopConfigException("Advice must be declared inside an aspect type: " +
					"Offending method '" + candidateAdviceMethod + "' in class [" +
					candidateAspectClass.getName() + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found AspectJ method: " + candidateAdviceMethod);
		}

		AbstractAspectJAdvice springAdvice;

		switch (aspectJAnnotation.getAnnotationType()) {
			case AtPointcut:
				if (logger.isDebugEnabled()) {
					logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
				}
				return null;
			case AtAround:
				springAdvice = new AspectJAroundAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtBefore:
				springAdvice = new AspectJMethodBeforeAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtAfter:
				springAdvice = new AspectJAfterAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtAfterReturning:
				springAdvice = new AspectJAfterReturningAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
				if (StringUtils.hasText(afterReturningAnnotation.returning())) {
					springAdvice.setReturningName(afterReturningAnnotation.returning());
				}
				break;
			case AtAfterThrowing:
				springAdvice = new AspectJAfterThrowingAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
				if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
					springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
				}
				break;
			default:
				throw new UnsupportedOperationException(
						"Unsupported advice type on method: " + candidateAdviceMethod);
		}

		// 现在配置通知...
		springAdvice.setAspectName(aspectName);
		springAdvice.setDeclarationOrder(declarationOrder);
		String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
		if (argNames != null) {
			springAdvice.setArgumentNamesFromStringArray(argNames);
		}
		springAdvice.calculateArgumentBindings();

		return springAdvice;
	}


	/**
	 * 实例化切面的合成 advisor。
	 * 由非单例切面上的 per-clause 切点触发。
	 * 该通知没有效果。
	 */
	@SuppressWarnings("serial")
	protected static class SyntheticInstantiationAdvisor extends DefaultPointcutAdvisor {

		public SyntheticInstantiationAdvisor(final MetadataAwareAspectInstanceFactory aif) {
			super(aif.getAspectMetadata().getPerClausePointcut(), (MethodBeforeAdvice)
					(method, args, target) -> aif.getAspectInstance());
		}
	}

}
