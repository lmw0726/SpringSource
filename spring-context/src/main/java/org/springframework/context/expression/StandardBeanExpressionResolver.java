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

package org.springframework.context.expression;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanExpressionException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard implementation of the
 * {@link org.springframework.beans.factory.config.BeanExpressionResolver}
 * interface, parsing and evaluating Spring EL using Spring's expression module.
 *
 * <p>All beans in the containing {@code BeanFactory} are made available as
 * predefined variables with their common bean name, including standard context
 * beans such as "environment", "systemProperties" and "systemEnvironment".
 *
 * @author Juergen Hoeller
 * @see BeanExpressionContext#getBeanFactory()
 * @see org.springframework.expression.ExpressionParser
 * @see org.springframework.expression.spel.standard.SpelExpressionParser
 * @see org.springframework.expression.spel.support.StandardEvaluationContext
 * @since 3.0
 */
public class StandardBeanExpressionResolver implements BeanExpressionResolver {

	/**
	 * 默认表达式前缀：“#{”。
	 */
	public static final String DEFAULT_EXPRESSION_PREFIX = "#{";

	/**
	 * 默认表达式后缀：“}”。
	 */
	public static final String DEFAULT_EXPRESSION_SUFFIX = "}";


	/**
	 * 表达式前缀
	 */
	private String expressionPrefix = DEFAULT_EXPRESSION_PREFIX;

	/**
	 * 表达式后缀
	 */
	private String expressionSuffix = DEFAULT_EXPRESSION_SUFFIX;

	/**
	 * 表达式解析器
	 */
	private ExpressionParser expressionParser;

	/**
	 * 表单时字符串与表达式类 Map缓存
	 */
	private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>(256);

	/**
	 * bean表达式上下文 与 标准评估上下文 Map缓存
	 */
	private final Map<BeanExpressionContext, StandardEvaluationContext> evaluationCache = new ConcurrentHashMap<>(8);

	/**
	 * bean表达式解析器上下文
	 */
	private final ParserContext beanExpressionParserContext = new ParserContext() {
		@Override
		public boolean isTemplate() {
			return true;
		}

		@Override
		public String getExpressionPrefix() {
			return expressionPrefix;
		}

		@Override
		public String getExpressionSuffix() {
			return expressionSuffix;
		}
	};


	/**
	 * 使用默认设置创建新的 {@code StandardBeanExpressionResolver}。
	 */
	public StandardBeanExpressionResolver() {
		this.expressionParser = new SpelExpressionParser();
	}

	/**
	 * 使用给定的bean类加载器创建一个新的 {@code StandardBeanExpressionResolver}，将其用作表达式编译的基础。
	 *
	 * @param beanClassLoader 工厂的bean类加载器
	 */
	public StandardBeanExpressionResolver(@Nullable ClassLoader beanClassLoader) {
		//SpringEL表达式解析器
		this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, beanClassLoader));
	}


	/**
	 * 设置表达式字符串开头的前缀。默认为 “#{”。
	 *
	 * @see #DEFAULT_EXPRESSION_PREFIX
	 */
	public void setExpressionPrefix(String expressionPrefix) {
		Assert.hasText(expressionPrefix, "Expression prefix must not be empty");
		this.expressionPrefix = expressionPrefix;
	}

	/**
	 * 设置表达式字符串结尾的后缀。默认为 “}”。
	 *
	 * @see #DEFAULT_EXPRESSION_SUFFIX
	 */
	public void setExpressionSuffix(String expressionSuffix) {
		Assert.hasText(expressionSuffix, "Expression suffix must not be empty");
		this.expressionSuffix = expressionSuffix;
	}

	/**
	 * 指定用于表达式解析的EL解析器。
	 * <p> 默认为 {@link org.springframework.expression.spel.standard.SpelExpressionParser}，与标准统一EL样式表达式语法兼容。
	 */
	public void setExpressionParser(ExpressionParser expressionParser) {
		Assert.notNull(expressionParser, "ExpressionParser must not be null");
		this.expressionParser = expressionParser;
	}


	@Override
	@Nullable
	public Object evaluate(@Nullable String value, BeanExpressionContext beanExpressionContext) throws BeansException {
		if (!StringUtils.hasLength(value)) {
			//如果值是空的，直接返回原始值
			return value;
		}
		try {
			//获取值对应的表达式
			Expression expr = this.expressionCache.get(value);
			if (expr == null) {
				//如果表达式不存在，解析表达式
				expr = this.expressionParser.parseExpression(value, this.beanExpressionParserContext);
				//添加到值-表达式缓存中
				this.expressionCache.put(value, expr);
			}
			StandardEvaluationContext sec = this.evaluationCache.get(beanExpressionContext);
			if (sec == null) {
				//如果标准评估上下文为空，使用bean表达式上下文创建新的标准评估上下文
				sec = new StandardEvaluationContext(beanExpressionContext);
				//添加bean表达式上下文访问器
				sec.addPropertyAccessor(new BeanExpressionContextAccessor());
				//添加bean工厂访问器
				sec.addPropertyAccessor(new BeanFactoryAccessor());
				//添加Map访问器
				sec.addPropertyAccessor(new MapAccessor());
				//添加环境访问器
				sec.addPropertyAccessor(new EnvironmentAccessor());
				//设置bean解析器
				sec.setBeanResolver(new BeanFactoryResolver(beanExpressionContext.getBeanFactory()));
				//设置类型定位器
				sec.setTypeLocator(new StandardTypeLocator(beanExpressionContext.getBeanFactory().getBeanClassLoader()));
				//设置类型转换器
				sec.setTypeConverter(new StandardTypeConverter(() -> {
					//获取类型转换类
					ConversionService cs = beanExpressionContext.getBeanFactory().getConversionService();
					//如果
					return (cs != null ? cs : DefaultConversionService.getSharedInstance());
				}));
				//自定义评估上下文
				customizeEvaluationContext(sec);
				this.evaluationCache.put(beanExpressionContext, sec);
			}
			return expr.getValue(sec);
		} catch (Throwable ex) {
			throw new BeanExpressionException("Expression parsing failed", ex);
		}
	}

	/**
	 * 用于自定义表达式求值上下文的模板方法。
	 * <p> 默认实现为空。
	 */
	protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
	}

}
