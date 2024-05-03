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

package org.springframework.web.servlet.tags;

import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.io.IOException;

/**
 * {@code <eval>} 标签用于评估一个 Spring 表达式（SpEL），并打印结果或将其分配给变量。支持标准的 JSP 评估上下文，包括隐式变量和作用域属性。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>是否必需？</th>
 * <th>运行时表达式？</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>expression</td>
 * <td>true</td>
 * <td>true</td>
 * <td>要评估的表达式。</td>
 * </tr>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>为此标签设置 HTML 转义，作为布尔值。覆盖当前页面的默认 HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>为此标签设置 JavaScript 转义，作为布尔值。默认值为 false。</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>变量的作用域。支持 'application'、'session'、'request' 和 'page' 作用域。默认为 'page' 作用域。除非也定义了 var 属性，否则此属性无效。</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>要导出评估结果的变量名称。如果未指定，评估结果将转换为字符串并写入输出。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0.1
 */
@SuppressWarnings("serial")
public class EvalTag extends HtmlEscapingAwareTag {

	/**
	 * {@link javax.servlet.jsp.PageContext} 属性，用于页面级别的 {@link EvaluationContext} 实例。
	 */
	private static final String EVALUATION_CONTEXT_PAGE_ATTRIBUTE =
			"org.springframework.web.servlet.tags.EVALUATION_CONTEXT";

	/**
	 * SpEL 表达式解析器。
	 */
	private final ExpressionParser expressionParser = new SpelExpressionParser();

	/**
	 * 表达式
	 */
	@Nullable
	private Expression expression;

	/**
	 * 变量值
	 */
	@Nullable
	private String var;

	/**
	 * 范围
	 */
	private int scope = PageContext.PAGE_SCOPE;

	/**
	 * 是否对JavaScript进行转义
	 */
	private boolean javaScriptEscape = false;


	/**
	 * 设置要评估的表达式。
	 */
	public void setExpression(String expression) {
		this.expression = this.expressionParser.parseExpression(expression);
	}

	/**
	 * 设置要公开评估结果的变量名称。默认为将结果呈现到当前的 JspWriter。
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * 设置要将评估结果导出到的作用域。
	 * 只有在也定义了 var 时，此属性才有意义。
	 */
	public void setScope(String scope) {
		this.scope = TagUtils.getScope(scope);
	}

	/**
	 * 设置 JavaScript 转义，作为布尔值。默认值为 "false"。
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}


	@Override
	public int doStartTagInternal() throws JspException {
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() throws JspException {
		// 获取页面上下文中的评估上下文。
		EvaluationContext evaluationContext =
				(EvaluationContext) this.pageContext.getAttribute(EVALUATION_CONTEXT_PAGE_ATTRIBUTE);
		if (evaluationContext == null) {
			// 如果评估上下文为空，则创建一个新的评估上下文。
			evaluationContext = createEvaluationContext(this.pageContext);
			// 将新创建的评估上下文存储在页面上下文中。
			this.pageContext.setAttribute(EVALUATION_CONTEXT_PAGE_ATTRIBUTE, evaluationContext);
		}
		if (this.var != null) {
			// 如果有 var 属性，则评估表达式
			Object result = (this.expression != null ? this.expression.getValue(evaluationContext) : null);
			// 将结果存储在页面上下文中指定的作用域中。
			this.pageContext.setAttribute(this.var, result, this.scope);
		} else {
			try {
				// 如果没有 var 属性，如果表达式存在，评估表达式获取结果值
				String result = (this.expression != null ?
						this.expression.getValue(evaluationContext, String.class) : null);
				// 将结果转换为字符串表示。
				result = ObjectUtils.getDisplayString(result);
				// 对结果进行 HTML 转义。
				result = htmlEscape(result);
				// 如果启用了 JavaScript 转义，则对结果进行 JavaScript 转义。
				result = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(result) : result);
				// 将结果输出到页面输出流。
				this.pageContext.getOut().print(result);
			} catch (IOException ex) {
				// 处理 IO 异常。
				throw new JspException(ex);
			}
		}
		// 返回继续处理页面的指令。
		return EVAL_PAGE;
	}

	private EvaluationContext createEvaluationContext(PageContext pageContext) {
		// 创建标准评估上下文。
		StandardEvaluationContext context = new StandardEvaluationContext();
		// 添加 JSP 属性访问器，使评估上下文能够访问页面上下文中的属性。
		context.addPropertyAccessor(new JspPropertyAccessor(pageContext));
		// 添加 Map 访问器，使评估上下文能够访问 Map 中的属性。
		context.addPropertyAccessor(new MapAccessor());
		// 添加环境访问器，使评估上下文能够访问环境中的属性。
		context.addPropertyAccessor(new EnvironmentAccessor());
		// 设置 Bean 解析器，以解析 Bean 工厂中的 bean。
		context.setBeanResolver(new BeanFactoryResolver(getRequestContext().getWebApplicationContext()));
		// 获取转换服务，如果存在则设置类型转换器。
		ConversionService conversionService = getConversionService(pageContext);
		if (conversionService != null) {
			context.setTypeConverter(new StandardTypeConverter(conversionService));
		}
		// 返回配置完成的评估上下文。
		return context;
	}

	@Nullable
	private ConversionService getConversionService(PageContext pageContext) {
		return (ConversionService) pageContext.getRequest().getAttribute(ConversionService.class.getName());
	}


	@SuppressWarnings("deprecation")
	private static class JspPropertyAccessor implements PropertyAccessor {

		/**
		 * 页面上下文
		 */
		private final PageContext pageContext;

		@Nullable
		private final javax.servlet.jsp.el.VariableResolver variableResolver;

		public JspPropertyAccessor(PageContext pageContext) {
			this.pageContext = pageContext;
			this.variableResolver = pageContext.getVariableResolver();
		}

		@Override
		@Nullable
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		@Override
		public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
			return (target == null &&
					(resolveImplicitVariable(name) != null || this.pageContext.findAttribute(name) != null));
		}

		@Override
		public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
			// 解析隐式变量。
			Object implicitVar = resolveImplicitVariable(name);
			// 如果隐式变量不为 null，则返回包含隐式变量的 TypedValue。
			if (implicitVar != null) {
				return new TypedValue(implicitVar);
			}
			// 否则，返回从页面上下文中查找到的名为 name 的属性对应的 TypedValue。
			return new TypedValue(this.pageContext.findAttribute(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) {
			throw new UnsupportedOperationException();
		}

		@Nullable
		private Object resolveImplicitVariable(String name) throws AccessException {
			if (this.variableResolver == null) {
				// 如果变量解析器不存在，返回null。
				return null;
			}
			try {
				// 解析变量名称
				return this.variableResolver.resolveVariable(name);
			} catch (Exception ex) {
				throw new AccessException(
						"Unexpected exception occurred accessing '" + name + "' as an implicit variable", ex);
			}
		}
	}

}
