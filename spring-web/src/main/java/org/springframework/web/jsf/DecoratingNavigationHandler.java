/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.jsf;

import org.springframework.lang.Nullable;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;

/**
 * JSF NavigationHandler 的基类实现，允许装饰原始 NavigationHandler。
 *
 * <p>支持标准 JSF 风格的装饰（通过构造函数参数）以及带有明确 NavigationHandler 参数的重载
 * {@code handleNavigation} 方法（传入原始 NavigationHandler）。子类必须实现这个重载的
 * {@code handleNavigation} 方法。标准的 JSF 调用将自动委托给重载方法，传入构造函数注入的
 * NavigationHandler 作为参数。
 * <p>
 *
 * @author Juergen Hoeller
 * @see #handleNavigation(javax.faces.context.FacesContext, String, String, NavigationHandler)
 * @see DelegatingNavigationHandlerProxy
 * @since 1.2.7
 */
public abstract class DecoratingNavigationHandler extends NavigationHandler {
	/**
	 * 装饰的导航处理器
	 */
	@Nullable
	private NavigationHandler decoratedNavigationHandler;


	/**
	 * 创建一个不固定原始 NavigationHandler 的 DecoratingNavigationHandler。
	 */
	protected DecoratingNavigationHandler() {
	}

	/**
	 * 创建一个固定原始 NavigationHandler 的 DecoratingNavigationHandler。
	 *
	 * @param originalNavigationHandler 要装饰的原始 NavigationHandler
	 */
	protected DecoratingNavigationHandler(NavigationHandler originalNavigationHandler) {
		this.decoratedNavigationHandler = originalNavigationHandler;
	}

	/**
	 * 返回由此处理程序装饰的固定原始 NavigationHandler（如果有）
	 * （即，如果通过构造函数传入）。
	 */
	@Nullable
	public final NavigationHandler getDecoratedNavigationHandler() {
		return this.decoratedNavigationHandler;
	}


	/**
	 * 此标准 JSF {@code handleNavigation} 方法的实现委托给重载的变体，传入构造函数注入的
	 * NavigationHandler 作为参数。
	 *
	 * @see #handleNavigation(javax.faces.context.FacesContext, String, String, javax.faces.application.NavigationHandler)
	 */
	@Override
	public final void handleNavigation(FacesContext facesContext, String fromAction, String outcome) {
		handleNavigation(facesContext, fromAction, outcome, this.decoratedNavigationHandler);
	}

	/**
	 * 带有明确 NavigationHandler 参数的特殊 {@code handleNavigation} 变体。可以直接调用，
	 * 由具有明确原始处理程序的代码调用，或者从标准 {@code handleNavigation} 方法调用，
	 * 作为普通 JSF 定义的 NavigationHandler。
	 * <p>实现应该调用 {@code callNextHandlerInChain} 来委托给链中的下一个处理程序。
	 * 这将始终调用最合适的下一个处理程序（请参阅 {@code callNextHandlerInChain} 的 Javadoc）。
	 * 或者，也可以直接调用装饰的 NavigationHandler 或传入的原始 NavigationHandler；
	 * 但是，在处理链中的潜在位置上做出反应的灵活性不如直接调用 {@code callNextHandlerInChain}。
	 *
	 * @param facesContext              当前的 JSF 上下文
	 * @param fromAction                用于检索指定结果的操作绑定表达式，
	 *                                  或者如果结果是通过其他方式获取的，则为 {@code null}
	 * @param outcome                   先前调用的应用程序操作返回的逻辑结果（可能为 {@code null}）
	 * @param originalNavigationHandler 原始 NavigationHandler，或者 {@code null}（如果没有）
	 * @see #callNextHandlerInChain
	 */
	public abstract void handleNavigation(FacesContext facesContext, @Nullable String fromAction,
										  @Nullable String outcome, @Nullable NavigationHandler originalNavigationHandler);


	/**
	 * 当子类意图委托给 NavigationHandler 链中的下一个处理程序时调用的方法。
	 * 将始终调用最合适的下一个处理程序，要么是通过构造函数参数传入的装饰的 NavigationHandler，
	 * 要么是通过此方法传入的原始 NavigationHandler - 根据此实例在链中的位置而定。
	 * <p>如果指定了通过构造函数参数传入的装饰的 NavigationHandler，此方法将调用它，
	 * 并将原始 NavigationHandler 传递给链中的下一个元素：这确保了最后一个处理程序链中的原始处理程序
	 * 可以将委托传回。如果目标是 DecoratingNavigationHandler，则传入此方法的原始 NavigationHandler
	 * 将传递到链中的下一个元素：这确保了原始处理程序的委托传递给链中的下一个元素。
	 * 如果目标是标准 NavigationHandler，则原始处理程序将简单地不会被传递；在这种情况下，
	 * 进一步向下传递到原始处理程序是不可能的。
	 * <p>如果没有通过构造函数参数指定装饰的 NavigationHandler，此实例将是链中的最后一个元素。
	 * 因此，此方法将调用通过此方法传入的原始 NavigationHandler。如果没有传入原始 NavigationHandler
	 * （例如，如果此实例是具有标准 NavigationHandler 的链中的最后一个元素），则此方法相当于 no-op。
	 *
	 * @param facesContext              当前的 JSF 上下文
	 * @param fromAction                用于检索指定结果的操作绑定表达式，或者如果结果是通过其他方式获取的，则为 {@code null}
	 * @param outcome                   先前调用的应用程序操作返回的逻辑结果（可能为 {@code null}）
	 * @param originalNavigationHandler 原始 NavigationHandler，或者 {@code null}（如果没有）
	 */
	protected final void callNextHandlerInChain(FacesContext facesContext, @Nullable String fromAction,
												@Nullable String outcome, @Nullable NavigationHandler originalNavigationHandler) {
		// 获取装饰导航处理器
		NavigationHandler decoratedNavigationHandler = getDecoratedNavigationHandler();

		if (decoratedNavigationHandler instanceof DecoratingNavigationHandler) {
			// 如果通过构造函数参数指定了 DecoratingNavigationHandler：
			// 调用它，并传递原始 NavigationHandler。
			DecoratingNavigationHandler decHandler = (DecoratingNavigationHandler) decoratedNavigationHandler;
			decHandler.handleNavigation(facesContext, fromAction, outcome, originalNavigationHandler);
		} else if (decoratedNavigationHandler != null) {
			// 如果通过构造函数参数指定了标准 NavigationHandler：
			// 通过标准 API 调用它，不传递原始 NavigationHandler。
			// 被调用的处理程序将无法重定向到原始处理程序。
			decoratedNavigationHandler.handleNavigation(facesContext, fromAction, outcome);
		} else if (originalNavigationHandler != null) {
			// 如果没有通过构造函数参数指定 NavigationHandler：
			// 调用原始处理程序，标记为链的结束。
			originalNavigationHandler.handleNavigation(facesContext, fromAction, outcome);
		}
	}

}
