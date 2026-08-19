/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.aop.aspectj;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;

import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link ParameterNameDiscoverer} 实现，尝试从切点表达式、returning 子句和 throwing 子句
 * 推断通知方法的参数名称。如果无法获得明确的解释，则返回 {@code null}。
 *
 * <p>此类按以下方式解释参数：
 * <ol>
 * <li>如果方法的第一个参数类型为 {@link JoinPoint}
 * 或 {@link ProceedingJoinPoint}，则假定用于将
 * {@code thisJoinPoint} 传递给通知，并且参数名称
 * 将被赋值为 {@code "thisJoinPoint"}。</li>
 * <li>如果方法的第一个参数类型为
 * {@code JoinPoint.StaticPart}，则假定用于将
 * {@code "thisJoinPointStaticPart"} 传递给通知，并且参数名称
 * 将被赋值为 {@code "thisJoinPointStaticPart"}。</li>
 * <li>如果已设置 {@link #setThrowingName(String) throwingName}，并且
 * 没有类型为 {@code Throwable+} 的未绑定参数，则抛出
 * {@link IllegalArgumentException}。如果存在多个
 * 类型为 {@code Throwable+} 的未绑定参数，则抛出
 * {@link AmbiguousBindingException}。如果恰好存在一个
 * 类型为 {@code Throwable+} 的未绑定参数，则相应的
 * 参数名称将被赋值为 &lt;throwingName&gt;。</li>
 * <li>如果仍有未绑定参数，则检查切点表达式。
 * 设 {@code a} 为以绑定形式使用的基于注解的切点表达式的数量
 * (&#64;annotation、&#64;this、&#64;target、&#64;args、
 * &#64;within、&#64;withincode)。绑定形式的使用本身需要推断：
 * 如果切点内的表达式是单个符合 Java 变量名约定的字符串字面量，
 * 则假定它是一个变量名。如果 {@code a} 为零，我们进入下一阶段。
 * 如果 {@code a} &gt; 1，则抛出 {@code AmbiguousBindingException}。
 * 如果 {@code a} == 1，并且没有类型为 {@code Annotation+} 的未绑定参数，
 * 则抛出 {@code IllegalArgumentException}。如果恰好存在一个
 * 这样的参数，则相应的参数名称将被赋值为切点表达式中的值。</li>
 * <li>如果已设置 returningName，并且没有未绑定参数，
 * 则抛出 {@code IllegalArgumentException}。如果存在多个
 * 未绑定参数，则抛出 {@code AmbiguousBindingException}。
 * 如果恰好存在一个未绑定参数，则相应的参数名称
 * 将被赋值为 &lt;returningName&gt;。</li>
 * <li>如果仍有未绑定参数，则再次检查切点表达式中
 * 以绑定形式使用的 {@code this}、{@code target} 和
 * {@code args} 切点表达式（绑定形式的推断方式
 * 与基于注解的切点相同）。如果仍有多个
 * 基本类型的未绑定参数（只能在 {@code args} 中绑定），
 * 则抛出 {@code AmbiguousBindingException}。
 * 如果恰好存在一个基本类型的参数，并且恰好找到一个 {@code args}
 * 绑定变量，我们将相应的参数名称赋值为该变量名。
 * 如果没有找到 {@code args} 绑定变量，则抛出
 * {@code IllegalStateException}。如果存在多个
 * {@code args} 绑定变量，则抛出 {@code AmbiguousBindingException}。
 * 此时，如果仍有多个未绑定参数，我们抛出
 * {@code AmbiguousBindingException}。如果没有剩余的
 * 未绑定参数，则完成。如果恰好存在一个剩余的未绑定参数，
 * 并且 {@code this}、{@code target} 或 {@code args}
 * 中只有一个候选变量名未绑定，则将其赋值为相应的参数名称。
 * 如果存在多种可能性，则抛出 {@code AmbiguousBindingException}。</li>
 * </ol>
 *
 * <p>抛出 {@code IllegalArgumentException} 或
 * {@code AmbiguousBindingException} 的行为是可配置的，
 * 允许此发现器作为责任链的一部分使用。默认情况下，
 * 该条件将被记录，并且 {@code getParameterNames(..)} 方法将简单地返回
 * {@code null}。如果 {@link #setRaiseExceptions(boolean) raiseExceptions}
 * 属性设置为 {@code true}，则这些条件将分别作为
 * {@code IllegalArgumentException} 和 {@code AmbiguousBindingException} 抛出。
 *
 * <p>这很清楚了吗？;)
 *
 * <p>简短版本：如果可以推断出明确的绑定，就会推断。
 * 如果通知要求无法满足，则返回 {@code null}。
 * 通过将 {@link #setRaiseExceptions(boolean) raiseExceptions}
 * 属性设置为 {@code true}，在无法发现参数名称的情况下，
 * 将抛出描述性异常而不是返回 {@code null}。
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AspectJAdviceParameterNameDiscoverer implements ParameterNameDiscoverer {

	private static final String THIS_JOIN_POINT = "thisJoinPoint";
	private static final String THIS_JOIN_POINT_STATIC_PART = "thisJoinPointStaticPart";

	// 绑定算法的各个步骤……
	private static final int STEP_JOIN_POINT_BINDING = 1;
	private static final int STEP_THROWING_BINDING = 2;
	private static final int STEP_ANNOTATION_BINDING = 3;
	private static final int STEP_RETURNING_BINDING = 4;
	private static final int STEP_PRIMITIVE_ARGS_BINDING = 5;
	private static final int STEP_THIS_TARGET_ARGS_BINDING = 6;
	private static final int STEP_REFERENCE_PCUT_BINDING = 7;
	private static final int STEP_FINISHED = 8;

	private static final Set<String> singleValuedAnnotationPcds = new HashSet<>();
	private static final Set<String> nonReferencePointcutTokens = new HashSet<>();


	static {
		singleValuedAnnotationPcds.add("@this");
		singleValuedAnnotationPcds.add("@target");
		singleValuedAnnotationPcds.add("@within");
		singleValuedAnnotationPcds.add("@withincode");
		singleValuedAnnotationPcds.add("@annotation");

		Set<PointcutPrimitive> pointcutPrimitives = PointcutParser.getAllSupportedPointcutPrimitives();
		for (PointcutPrimitive primitive : pointcutPrimitives) {
			nonReferencePointcutTokens.add(primitive.getName());
		}
		nonReferencePointcutTokens.add("&&");
		nonReferencePointcutTokens.add("!");
		nonReferencePointcutTokens.add("||");
		nonReferencePointcutTokens.add("and");
		nonReferencePointcutTokens.add("or");
		nonReferencePointcutTokens.add("not");
	}


	/** 与通知关联的切点表达式，作为简单字符串。 */
	@Nullable
	private String pointcutExpression;

	private boolean raiseExceptions;

	/** 如果通知是 afterReturning，并且绑定了返回值，则这是使用的参数名称。 */
	@Nullable
	private String returningName;

	/** 如果通知是 afterThrowing，并且绑定了抛出的值，则这是使用的参数名称。 */
	@Nullable
	private String throwingName;

	private Class<?>[] argumentTypes = new Class<?>[0];

	private String[] parameterNameBindings = new String[0];

	private int numberOfRemainingUnboundArguments;


	/**
	 * 创建一个新的发现器，尝试从给定的切点表达式发现参数名称。
	 */
	public AspectJAdviceParameterNameDiscoverer(@Nullable String pointcutExpression) {
		this.pointcutExpression = pointcutExpression;
	}


	/**
	 * 指示在无法推断通知参数名称的情况下，是否必须
	 * 抛出相应的 {@link IllegalArgumentException} 和 {@link AmbiguousBindingException}。
	 * @param raiseExceptions 如果要抛出异常，则为 {@code true}
	 */
	public void setRaiseExceptions(boolean raiseExceptions) {
		this.raiseExceptions = raiseExceptions;
	}

	/**
	 * 如果 {@code afterReturning} 通知绑定了返回值，
	 * 则必须指定 returning 变量名称。
	 * @param returningName returning 变量的名称
	 */
	public void setReturningName(@Nullable String returningName) {
		this.returningName = returningName;
	}

	/**
	 * 如果 {@code afterThrowing} 通知绑定了抛出的值，
	 * 则必须指定 throwing 变量名称。
	 * @param throwingName throwing 变量的名称
	 */
	public void setThrowingName(@Nullable String throwingName) {
		this.throwingName = throwingName;
	}


	/**
	 * 推断通知方法的参数名称。
	 * <p>有关所用算法的详细信息，请参阅
	 * {@link AspectJAdviceParameterNameDiscoverer 类级别 javadoc}。
	 * @param method 目标 {@link Method}
	 * @return 参数名称
	 */
	@Override
	@Nullable
	public String[] getParameterNames(Method method) {
		this.argumentTypes = method.getParameterTypes();
		this.numberOfRemainingUnboundArguments = this.argumentTypes.length;
		this.parameterNameBindings = new String[this.numberOfRemainingUnboundArguments];

		int minimumNumberUnboundArgs = 0;
		if (this.returningName != null) {
			minimumNumberUnboundArgs++;
		}
		if (this.throwingName != null) {
			minimumNumberUnboundArgs++;
		}
		if (this.numberOfRemainingUnboundArguments < minimumNumberUnboundArgs) {
			throw new IllegalStateException(
					"Not enough arguments in method to satisfy binding of returning and throwing variables");
		}

		try {
			int algorithmicStep = STEP_JOIN_POINT_BINDING;
			while ((this.numberOfRemainingUnboundArguments > 0) && algorithmicStep < STEP_FINISHED) {
				switch (algorithmicStep++) {
					case STEP_JOIN_POINT_BINDING:
						if (!maybeBindThisJoinPoint()) {
							maybeBindThisJoinPointStaticPart();
						}
						break;
					case STEP_THROWING_BINDING:
						maybeBindThrowingVariable();
						break;
					case STEP_ANNOTATION_BINDING:
						maybeBindAnnotationsFromPointcutExpression();
						break;
					case STEP_RETURNING_BINDING:
						maybeBindReturningVariable();
						break;
					case STEP_PRIMITIVE_ARGS_BINDING:
						maybeBindPrimitiveArgsFromPointcutExpression();
						break;
					case STEP_THIS_TARGET_ARGS_BINDING:
						maybeBindThisOrTargetOrArgsFromPointcutExpression();
						break;
					case STEP_REFERENCE_PCUT_BINDING:
						maybeBindReferencePointcutParameter();
						break;
					default:
						throw new IllegalStateException("Unknown algorithmic step: " + (algorithmicStep - 1));
				}
			}
		}
		catch (AmbiguousBindingException | IllegalArgumentException ex) {
			if (this.raiseExceptions) {
				throw ex;
			}
			else {
				return null;
			}
		}

		if (this.numberOfRemainingUnboundArguments == 0) {
			return this.parameterNameBindings;
		}
		else {
			if (this.raiseExceptions) {
				throw new IllegalStateException("Failed to bind all argument names: " +
						this.numberOfRemainingUnboundArguments + " argument(s) could not be bound");
			}
			else {
				// 约定：失败时返回 null，以便参与责任链
				return null;
			}
		}
	}

	/**
	 * 在 Spring 中，通知方法永远不能是构造方法。
	 * @return {@code null}
	 * @throws UnsupportedOperationException 如果
	 * {@link #setRaiseExceptions(boolean) raiseExceptions} 已设置为 {@code true}
	 */
	@Override
	@Nullable
	public String[] getParameterNames(Constructor<?> ctor) {
		if (this.raiseExceptions) {
			throw new UnsupportedOperationException("An advice method can never be a constructor");
		}
		else {
			// 我们返回 null 而不是抛出异常，以便在责任链中表现良好
			return null;
		}
	}


	private void bindParameterName(int index, String name) {
		this.parameterNameBindings[index] = name;
		this.numberOfRemainingUnboundArguments--;
	}

	/**
	 * 如果第一个参数类型为 JoinPoint 或 ProceedingJoinPoint，
	 * 则将 "thisJoinPoint" 绑定为参数名称并返回 true，否则返回 false。
	 */
	private boolean maybeBindThisJoinPoint() {
		if ((this.argumentTypes[0] == JoinPoint.class) || (this.argumentTypes[0] == ProceedingJoinPoint.class)) {
			bindParameterName(0, THIS_JOIN_POINT);
			return true;
		}
		else {
			return false;
		}
	}

	private void maybeBindThisJoinPointStaticPart() {
		if (this.argumentTypes[0] == JoinPoint.StaticPart.class) {
			bindParameterName(0, THIS_JOIN_POINT_STATIC_PART);
		}
	}

	/**
	 * 如果指定了 throwing 名称，并且恰好剩余一个选择
	 *（即 Throwable 的子类型的参数），则绑定它。
	 */
	private void maybeBindThrowingVariable() {
		if (this.throwingName == null) {
			return;
		}

		// 所以有绑定工作要做……
		int throwableIndex = -1;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(Throwable.class, i)) {
				if (throwableIndex == -1) {
					throwableIndex = i;
				}
				else {
					// 找到的第二个候选参数——绑定存在歧义
					throw new AmbiguousBindingException("Binding of throwing parameter '" +
							this.throwingName + "' is ambiguous: could be bound to argument " +
							throwableIndex + " or argument " + i);
				}
			}
		}

		if (throwableIndex == -1) {
			throw new IllegalStateException("Binding of throwing parameter '" + this.throwingName
					+ "' could not be completed as no available arguments are a subtype of Throwable");
		}
		else {
			bindParameterName(throwableIndex, this.throwingName);
		}
	}

	/**
	 * 如果指定了 returning 变量，并且只有一个选择剩余，则绑定它。
	 */
	private void maybeBindReturningVariable() {
		if (this.numberOfRemainingUnboundArguments == 0) {
			throw new IllegalStateException(
					"Algorithm assumes that there must be at least one unbound parameter on entry to this method");
		}

		if (this.returningName != null) {
			if (this.numberOfRemainingUnboundArguments > 1) {
				throw new AmbiguousBindingException("Binding of returning parameter '" + this.returningName +
						"' is ambiguous, there are " + this.numberOfRemainingUnboundArguments + " candidates.");
			}

			// 一切就绪……找到未绑定的参数并绑定它。
			for (int i = 0; i < this.parameterNameBindings.length; i++) {
				if (this.parameterNameBindings[i] == null) {
					bindParameterName(i, this.returningName);
					break;
				}
			}
		}
	}


	/**
	 * 解析字符串切点表达式，查找：
	 * &#64;this、&#64;target、&#64;args、&#64;within、&#64;withincode、&#64;annotation。
	 * 如果找到这些切点表达式之一，尝试提取候选变量名称
	 *（在 args 的情况下，可能是多个变量名称）。
	 * <p>AspectJ 在这方面提供更多支持就更好了... :)
	 */
	private void maybeBindAnnotationsFromPointcutExpression() {
		List<String> varNames = new ArrayList<>();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
		for (int i = 0; i < tokens.length; i++) {
			String toMatch = tokens[i];
			int firstParenIndex = toMatch.indexOf('(');
			if (firstParenIndex != -1) {
				toMatch = toMatch.substring(0, firstParenIndex);
			}
			if (singleValuedAnnotationPcds.contains(toMatch)) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			}
			else if (tokens[i].startsWith("@args(") || tokens[i].equals("@args")) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				maybeExtractVariableNamesFromArgs(body.text, varNames);
			}
		}

		bindAnnotationsFromVarNames(varNames);
	}

	/**
	 * 将给定的提取变量名称列表匹配到参数槽。
	 */
	private void bindAnnotationsFromVarNames(List<String> varNames) {
		if (!varNames.isEmpty()) {
			// 我们有工作要做……
			int numAnnotationSlots = countNumberOfUnboundAnnotationArguments();
			if (numAnnotationSlots > 1) {
				throw new AmbiguousBindingException("Found " + varNames.size() +
						" potential annotation variable(s), and " +
						numAnnotationSlots + " potential argument slots");
			}
			else if (numAnnotationSlots == 1) {
				if (varNames.size() == 1) {
					// 完全匹配
					findAndBind(Annotation.class, varNames.get(0));
				}
				else {
					// 有多个候选变量，但只有一个槽位
					throw new IllegalArgumentException("Found " + varNames.size() +
							" candidate annotation binding variables" +
							" but only one potential argument binding slot");
				}
			}
			else {
				// 没有槽位，因此假定这些候选变量实际上只是类型名称
			}
		}
	}

	/*
	 * 如果标记开头符合 Java 标识符约定，则接受。
	 */
	@Nullable
	private String maybeExtractVariableName(@Nullable String candidateToken) {
		if (AspectJProxyUtils.isVariableName(candidateToken)) {
			return candidateToken;
		}
		return null;
	}

	/**
	 * 给定一个 args 切点体（可以是 {@code args} 或 {@code at_args}），
	 * 将任何候选变量名称添加到给定的列表中。
	 */
	private void maybeExtractVariableNamesFromArgs(@Nullable String argsSpec, List<String> varNames) {
		if (argsSpec == null) {
			return;
		}
		String[] tokens = StringUtils.tokenizeToStringArray(argsSpec, ",");
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = StringUtils.trimWhitespace(tokens[i]);
			String varName = maybeExtractVariableName(tokens[i]);
			if (varName != null) {
				varNames.add(varName);
			}
		}
	}

	/**
	 * 解析字符串切点表达式，查找 this()、target() 和 args() 表达式。
	 * 如果找到其中一个，尝试提取候选变量名称并绑定它。
	 */
	private void maybeBindThisOrTargetOrArgsFromPointcutExpression() {
		if (this.numberOfRemainingUnboundArguments > 1) {
			throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments
					+ " unbound args at this(),target(),args() binding stage, with no way to determine between them");
		}

		List<String> varNames = new ArrayList<>();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("this") ||
					tokens[i].startsWith("this(") ||
					tokens[i].equals("target") ||
					tokens[i].startsWith("target(")) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			}
			else if (tokens[i].equals("args") || tokens[i].startsWith("args(")) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				List<String> candidateVarNames = new ArrayList<>();
				maybeExtractVariableNamesFromArgs(body.text, candidateVarNames);
				// 我们可能已经找到了一些在前面的基本类型参数绑定步骤中已绑定的变量名，
				// 把它们过滤掉……
				for (String varName : candidateVarNames) {
					if (!alreadyBound(varName)) {
						varNames.add(varName);
					}
				}
			}
		}


		if (varNames.size() > 1) {
			throw new AmbiguousBindingException("Found " + varNames.size() +
					" candidate this(), target() or args() variables but only one unbound argument slot");
		}
		else if (varNames.size() == 1) {
			for (int j = 0; j < this.parameterNameBindings.length; j++) {
				if (isUnbound(j)) {
					bindParameterName(j, varNames.get(0));
					break;
				}
			}
		}
		// 否则 varNames.size 必然为 0，我们没有任何可绑定的内容。
	}

	private void maybeBindReferencePointcutParameter() {
		if (this.numberOfRemainingUnboundArguments > 1) {
			throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments
					+ " unbound args at reference pointcut binding stage, with no way to determine between them");
		}

		List<String> varNames = new ArrayList<>();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
		for (int i = 0; i < tokens.length; i++) {
			String toMatch = tokens[i];
			if (toMatch.startsWith("!")) {
				toMatch = toMatch.substring(1);
			}
			int firstParenIndex = toMatch.indexOf('(');
			if (firstParenIndex != -1) {
				toMatch = toMatch.substring(0, firstParenIndex);
			}
			else {
				if (tokens.length < i + 2) {
					// 没有 "(" 且后面没有内容
					continue;
				}
				else {
					String nextToken = tokens[i + 1];
					if (nextToken.charAt(0) != '(') {
						// 下一个标记也不是 "("，不可能是切点……
						continue;
					}
				}

			}

			// 消费切点体
			PointcutBody body = getPointcutBody(tokens, i);
			i += body.numTokensConsumed;

			if (!nonReferencePointcutTokens.contains(toMatch)) {
				// 那么它可能是一个引用切点
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			}
		}

		if (varNames.size() > 1) {
			throw new AmbiguousBindingException("Found " + varNames.size() +
					" candidate reference pointcut variables but only one unbound argument slot");
		}
		else if (varNames.size() == 1) {
			for (int j = 0; j < this.parameterNameBindings.length; j++) {
				if (isUnbound(j)) {
					bindParameterName(j, varNames.get(0));
					break;
				}
			}
		}
		// 否则 varNames.size 必然为 0，我们没有任何可绑定的内容。
	}

	/*
	 * 我们在给定索引处的标记数组中找到了绑定切点的开头。
	 * 现在我们需要提取切点体并返回它。
	 */
	private PointcutBody getPointcutBody(String[] tokens, int startIndex) {
		int numTokensConsumed = 0;
		String currentToken = tokens[startIndex];
		int bodyStart = currentToken.indexOf('(');
		if (currentToken.charAt(currentToken.length() - 1) == ')') {
			// 这是一个整体……获取第一个 ( 和最后一个 ) 之间的文本
			return new PointcutBody(0, currentToken.substring(bodyStart + 1, currentToken.length() - 1));
		}
		else {
			StringBuilder sb = new StringBuilder();
			if (bodyStart >= 0 && bodyStart != (currentToken.length() - 1)) {
				sb.append(currentToken.substring(bodyStart + 1));
				sb.append(' ');
			}
			numTokensConsumed++;
			int currentIndex = startIndex + numTokensConsumed;
			while (currentIndex < tokens.length) {
				if (tokens[currentIndex].equals("(")) {
					currentIndex++;
					continue;
				}

				if (tokens[currentIndex].endsWith(")")) {
					sb.append(tokens[currentIndex], 0, tokens[currentIndex].length() - 1);
					return new PointcutBody(numTokensConsumed, sb.toString().trim());
				}

				String toAppend = tokens[currentIndex];
				if (toAppend.startsWith("(")) {
					toAppend = toAppend.substring(1);
				}
				sb.append(toAppend);
				sb.append(' ');
				currentIndex++;
				numTokensConsumed++;
			}

		}

		// 我们查找了但失败了……
		return new PointcutBody(numTokensConsumed, null);
	}

	/**
	 * 将 args 与基本类型的未绑定参数匹配。
	 */
	private void maybeBindPrimitiveArgsFromPointcutExpression() {
		int numUnboundPrimitives = countNumberOfUnboundPrimitiveArguments();
		if (numUnboundPrimitives > 1) {
			throw new AmbiguousBindingException("Found '" + numUnboundPrimitives +
					"' unbound primitive arguments with no way to distinguish between them.");
		}
		if (numUnboundPrimitives == 1) {
			// 查找 args 变量，如果恰好找到一个就绑定它……
			List<String> varNames = new ArrayList<>();
			String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
			for (int i = 0; i < tokens.length; i++) {
				if (tokens[i].equals("args") || tokens[i].startsWith("args(")) {
					PointcutBody body = getPointcutBody(tokens, i);
					i += body.numTokensConsumed;
					maybeExtractVariableNamesFromArgs(body.text, varNames);
				}
			}
			if (varNames.size() > 1) {
				throw new AmbiguousBindingException("Found " + varNames.size() +
						" candidate variable names but only one candidate binding slot when matching primitive args");
			}
			else if (varNames.size() == 1) {
				// 1 个基本类型参数，且只有一个候选……
				for (int i = 0; i < this.argumentTypes.length; i++) {
					if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
						bindParameterName(i, varNames.get(0));
						break;
					}
				}
			}
		}
	}

	/*
	 * 如果给定参数索引的参数名称绑定尚未分配，则返回 true。
	 */
	private boolean isUnbound(int i) {
		return this.parameterNameBindings[i] == null;
	}

	private boolean alreadyBound(String varName) {
		for (int i = 0; i < this.parameterNameBindings.length; i++) {
			if (!isUnbound(i) && varName.equals(this.parameterNameBindings[i])) {
				return true;
			}
		}
		return false;
	}

	/*
	 * 如果给定的参数类型是给定超类型的子类，
	 * 则返回 {@code true}。
	 */
	private boolean isSubtypeOf(Class<?> supertype, int argumentNumber) {
		return supertype.isAssignableFrom(this.argumentTypes[argumentNumber]);
	}

	private int countNumberOfUnboundAnnotationArguments() {
		int count = 0;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(Annotation.class, i)) {
				count++;
			}
		}
		return count;
	}

	private int countNumberOfUnboundPrimitiveArguments() {
		int count = 0;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
				count++;
			}
		}
		return count;
	}

	/*
	 * 找到具有给定类型的参数索引，并在该位置绑定给定的
	 * {@code varName}。
	 */
	private void findAndBind(Class<?> argumentType, String varName) {
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(argumentType, i)) {
				bindParameterName(i, varName);
				return;
			}
		}
		throw new IllegalStateException("Expected to find an unbound argument of type '" +
				argumentType.getName() + "'");
	}


	/**
	 * 简单的结构体，用于保存从切点体提取的文本，
	 * 以及提取时消耗的标记数量。
	 */
	private static class PointcutBody {

		private int numTokensConsumed;

		@Nullable
		private String text;

		public PointcutBody(int tokens, @Nullable String text) {
			this.numTokensConsumed = tokens;
			this.text = text;
		}
	}


	/**
	 * 在尝试解析方法的参数名称时检测到歧义绑定时抛出。
	 */
	@SuppressWarnings("serial")
	public static class AmbiguousBindingException extends RuntimeException {

		/**
		 * 使用指定消息构造新的 AmbiguousBindingException。
		 * @param msg 详细消息
		 */
		public AmbiguousBindingException(String msg) {
			super(msg);
		}
	}

}
