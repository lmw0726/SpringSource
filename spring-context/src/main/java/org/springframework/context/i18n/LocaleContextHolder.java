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

package org.springframework.context.i18n;

import org.springframework.core.NamedInheritableThreadLocal;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.TimeZone;

/**
 * 简单的持有者类，将 LocaleContext 实例与当前线程关联起来。
 * 如果 {@code inheritable} 标志设置为 {@code true}，则 LocaleContext 将被当前线程生成的任何子线程继承。
 *
 * <p>在 Spring 中用作当前 Locale 的中央持有者，无论何时都是必需的：例如，在 MessageSourceAccessor 中。
 * DispatcherServlet 自动在此处公开其当前 Locale。其他应用程序也可以公开自己的 Locale，以便使诸如
 * MessageSourceAccessor 等类自动使用该 Locale。
 *
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @see LocaleContext
 * @see org.springframework.context.support.MessageSourceAccessor
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 1.2
 */
public final class LocaleContextHolder {
	/**
	 * 线程本地变量，用于持有 LocaleContext 实例。
	 * 该变量保证了在每个线程中都有独立的 LocaleContext 实例。
	 */
	private static final ThreadLocal<LocaleContext> localeContextHolder =
			new NamedThreadLocal<>("LocaleContext");

	/**
	 * 继承性线程本地变量，用于持有 LocaleContext 实例。
	 * 该变量保证了在每个子线程中都有独立的 LocaleContext 实例，并且从父线程继承。
	 */
	private static final ThreadLocal<LocaleContext> inheritableLocaleContextHolder =
			new NamedInheritableThreadLocal<>("LocaleContext");

	/**
	 * 框架级别共享的默认 Locale。
	 */
	@Nullable
	private static Locale defaultLocale;

	/**
	 * 框架级别共享的默认时区。
	 */
	@Nullable
	private static TimeZone defaultTimeZone;


	private LocaleContextHolder() {
	}


	/**
	 * 重置当前线程的 LocaleContext。
	 */
	public static void resetLocaleContext() {
		localeContextHolder.remove();
		inheritableLocaleContextHolder.remove();
	}

	/**
	 * 将给定的 LocaleContext 与当前线程关联，
	 * 并且不将其公开为子线程可继承的上下文。
	 * <p>给定的 LocaleContext 可能是 TimeZoneAwareLocaleContext，
	 * 其中包含具有关联时区信息的语言环境。
	 *
	 * @param localeContext 当前的 LocaleContext，
	 *                      或者 {@code null} 来重置线程绑定的上下文
	 * @see SimpleLocaleContext
	 * @see SimpleTimeZoneAwareLocaleContext
	 */
	public static void setLocaleContext(@Nullable LocaleContext localeContext) {
		setLocaleContext(localeContext, false);
	}

	/**
	 * 将给定的 LocaleContext 与当前线程关联。
	 * <p>给定的 LocaleContext 可能是 TimeZoneAwareLocaleContext，
	 * 其中包含具有关联时区信息的语言环境。
	 *
	 * @param localeContext 当前的 LocaleContext，
	 *                      或者 {@code null} 来重置线程绑定的上下文
	 * @param inheritable   是否将 LocaleContext 公开为子线程可继承的
	 *                      上下文（使用 InheritableThreadLocal）
	 * @see SimpleLocaleContext
	 * @see SimpleTimeZoneAwareLocaleContext
	 */
	public static void setLocaleContext(@Nullable LocaleContext localeContext, boolean inheritable) {
		// 如果 localeContext 为 null，则重置 LocaleContext
		if (localeContext == null) {
			resetLocaleContext();
		} else {
			// 如果 inheritable 为 true，则使用可继承的 LocaleContextHolder
			if (inheritable) {
				inheritableLocaleContextHolder.set(localeContext);
				// 去除当前的本地上下文持有者
				localeContextHolder.remove();
			} else {
				// 否则，使用普通的 LocaleContextHolder
				localeContextHolder.set(localeContext);
				// 去除当前的可继承的本地上下文持有者
				inheritableLocaleContextHolder.remove();
			}
		}
	}

	/**
	 * 返回与当前线程关联的 LocaleContext（如果有）。
	 *
	 * @return 当前的 LocaleContext，如果没有则返回 {@code null}
	 */
	@Nullable
	public static LocaleContext getLocaleContext() {
		// 获取当前的 LocaleContext
		LocaleContext localeContext = localeContextHolder.get();
		// 如果当前的 LocaleContext 为 null，则尝试获取可继承的 LocaleContextHolder 中的值
		if (localeContext == null) {
			localeContext = inheritableLocaleContextHolder.get();
		}
		return localeContext;
	}

	/**
	 * 将给定的 Locale 与当前线程关联，保留已经设置的任何 TimeZone。
	 * <p>将隐式地为给定的 Locale 创建 LocaleContext，
	 * <i>不</i>将其暴露为子线程可继承的。
	 *
	 * @param locale 当前的 Locale，如果要重置线程绑定上下文的 locale 部分，则为 {@code null}
	 * @see #setTimeZone(TimeZone)
	 * @see SimpleLocaleContext#SimpleLocaleContext(Locale)
	 */
	public static void setLocale(@Nullable Locale locale) {
		setLocale(locale, false);
	}

	/**
	 * 将给定的 Locale 与当前线程关联，保留已经设置的任何 TimeZone。
	 * <p>将隐式地为给定的 Locale 创建 LocaleContext。
	 *
	 * @param locale      当前的 Locale，如果要重置线程绑定上下文的 locale 部分，则为 {@code null}
	 * @param inheritable 是否将 LocaleContext 暴露为可继承的，供子线程使用（使用 {@link InheritableThreadLocal}）
	 * @see #setTimeZone(TimeZone, boolean)
	 * @see SimpleLocaleContext#SimpleLocaleContext(Locale)
	 */
	public static void setLocale(@Nullable Locale locale, boolean inheritable) {
		// 获取当前的 LocaleContext
		LocaleContext localeContext = getLocaleContext();
		// 判断 LocaleContext 是否为 TimeZoneAwareLocaleContext
		TimeZone timeZone = (localeContext instanceof TimeZoneAwareLocaleContext ?
				((TimeZoneAwareLocaleContext) localeContext).getTimeZone() : null);
		// 根据情况创建新的 LocaleContext
		if (timeZone != null) {
			// 如果存在时区信息，则创建包含时区的 SimpleTimeZoneAwareLocaleContext
			localeContext = new SimpleTimeZoneAwareLocaleContext(locale, timeZone);
		} else if (locale != null) {
			// 如果不存在时区信息但存在 locale，则创建 SimpleLocaleContext
			localeContext = new SimpleLocaleContext(locale);
		} else {
			// 否则设为 null
			localeContext = null;
		}
		// 设置 LocaleContext，可以选择是否继承
		setLocaleContext(localeContext, inheritable);
	}

	/**
	 * 在框架级别设置共享的默认 locale，作为替代 JVM 广泛使用的默认 locale。
	 * <p><b>注意:</b> 这对于设置与 JVM 广泛默认 locale 不同的应用程序级默认 locale 很有用。
	 * 但是，这需要每个这样的应用程序针对本地部署的 Spring Framework jar 进行操作。
	 * 在这种情况下不要将 Spring 作为共享库部署在服务器级别上！
	 *
	 * @param locale 默认 locale（或 {@code null} 表示无，默认查找 {@link Locale#getDefault()}）
	 * @see #getLocale()
	 * @see Locale#getDefault()
	 * @since 4.3.5
	 */
	public static void setDefaultLocale(@Nullable Locale locale) {
		LocaleContextHolder.defaultLocale = locale;
	}

	/**
	 * 返回与当前线程关联的 Locale（如果有），否则返回系统默认的 Locale。
	 * 这实际上是 {@link java.util.Locale#getDefault()} 的替代品，可以选择尊重用户级别的 Locale 设置。
	 * <p>注意: 此方法有一个回退到共享默认 Locale，无论是在框架级别还是 JVM 广泛系统级别。
	 * 如果您想检查原始 LocaleContext 内容（可能通过 {@code null} 表示无特定 locale），请使用 {@link #getLocaleContext()} 并调用 {@link LocaleContext#getLocale()}
	 *
	 * @return 当前的 Locale，如果没有特定的 Locale 与当前线程关联，则返回系统默认的 Locale
	 * @see #getLocaleContext()
	 * @see LocaleContext#getLocale()
	 * @see #setDefaultLocale(Locale)
	 * @see java.util.Locale#getDefault()
	 */
	public static Locale getLocale() {
		return getLocale(getLocaleContext());
	}

	/**
	 * 返回与给定用户上下文关联的 Locale（如果有），否则返回系统默认的 Locale。
	 * 这实际上是 {@link java.util.Locale#getDefault()} 的替代品，可以选择尊重用户级别的 Locale 设置。
	 *
	 * @param localeContext 要检查的用户级别 locale 上下文
	 * @return 当前的 Locale，如果没有特定的 Locale 与当前线程关联，则返回系统默认的 Locale
	 * @see #getLocale()
	 * @see LocaleContext#getLocale()
	 * @see #setDefaultLocale(Locale)
	 * @see java.util.Locale#getDefault()
	 * @since 5.0
	 */
	public static Locale getLocale(@Nullable LocaleContext localeContext) {
		// 如果 localeContext 不为 null，则获取其中的 Locale
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			// 如果获取的 Locale 不为 null，则返回该 Locale
			if (locale != null) {
				return locale;
			}
		}
		// 如果 defaultLocale 不为 null，则返回 defaultLocale；
		// 否则返回系统默认 Locale
		return (defaultLocale != null ? defaultLocale : Locale.getDefault());
	}

	/**
	 * 将给定的 TimeZone 关联到当前线程，
	 * 保留可能已经设置的任何 Locale。
	 * <p>将隐式创建给定 Locale 的 LocaleContext，
	 * <i>不会</i>将其公开为子线程可继承的。
	 *
	 * @param timeZone 当前的 TimeZone，或 {@code null} 以重置
	 *                 线程绑定上下文的时区部分
	 * @see #setLocale(Locale)
	 * @see SimpleTimeZoneAwareLocaleContext#SimpleTimeZoneAwareLocaleContext(Locale, TimeZone)
	 */
	public static void setTimeZone(@Nullable TimeZone timeZone) {
		setTimeZone(timeZone, false);
	}

	/**
	 * 将给定的 TimeZone 关联到当前线程，
	 * 保留可能已经设置的任何 Locale。
	 * <p>将隐式创建给定 Locale 的 LocaleContext。
	 *
	 * @param timeZone    当前的 TimeZone，或 {@code null} 以重置
	 *                    线程绑定上下文的时区部分
	 * @param inheritable 是否将 LocaleContext 公开为可继承的，
	 *                    供子线程使用（使用 {@link InheritableThreadLocal}）
	 * @see #setLocale(Locale, boolean)
	 * @see SimpleTimeZoneAwareLocaleContext#SimpleTimeZoneAwareLocaleContext(Locale, TimeZone)
	 */
	public static void setTimeZone(@Nullable TimeZone timeZone, boolean inheritable) {
		// 获取当前 LocaleContext
		LocaleContext localeContext = getLocaleContext();
		// 如果 localeContext 不为 null，则获取其中的 Locale
		Locale locale = (localeContext != null ? localeContext.getLocale() : null);
		// 根据传入的 timeZone 创建一个新的 SimpleTimeZoneAwareLocaleContext，如果 timeZone 为 null，则创建 SimpleLocaleContext
		if (timeZone != null) {
			localeContext = new SimpleTimeZoneAwareLocaleContext(locale, timeZone);
		} else if (locale != null) {
			localeContext = new SimpleLocaleContext(locale);
		} else {
			localeContext = null;
		}
		// 将新创建的 LocaleContext 设置到上下文中，根据 inheritable 参数决定是使用 inheritableLocaleContextHolder 还是 localeContextHolder
		setLocaleContext(localeContext, inheritable);
	}

	/**
	 * 设置框架级别的共享默认时区，作为 JVM 广泛默认时区的替代品。
	 * <p><b>注意：</b> 这对于设置应用程序级别的默认时区非常有用，该时区与 JVM 广泛默认时区不同。
	 * 但是，这要求每个这样的应用程序都使用本地部署的 Spring Framework jars 进行操作。
	 * 不要在此类情况下将 Spring 部署为共享库！
	 *
	 * @param timeZone 默认时区（或 {@code null} 表示无，默认使用 {@link TimeZone#getDefault()} 进行查找）
	 * @see #getTimeZone()
	 * @see TimeZone#getDefault()
	 * @since 4.3.5
	 */
	public static void setDefaultTimeZone(@Nullable TimeZone timeZone) {
		defaultTimeZone = timeZone;
	}

	/**
	 * 返回与当前线程关联的 TimeZone（如果有），否则返回系统默认 TimeZone。
	 * 这实际上是 {@link java.util.TimeZone#getDefault()} 的替代品，
	 * 可以选择尊重用户级别的 TimeZone 设置。
	 * <p>注意：此方法具有对共享默认 TimeZone 的回退，
	 * 无论是在框架级别还是在 JVM 广泛系统级别。
	 * 如果要检查原始 LocaleContext 内容
	 * （可能通过 {@code null} 指示没有特定的时区），请使用
	 * {@link #getLocaleContext()} 并在向下转换为 {@link TimeZoneAwareLocaleContext} 后调用
	 * {@link TimeZoneAwareLocaleContext#getTimeZone()}。
	 *
	 * @return 当前 TimeZone，如果没有将特定的 TimeZone 关联到当前线程，则返回系统默认 TimeZone
	 * @see #getLocaleContext()
	 * @see TimeZoneAwareLocaleContext#getTimeZone()
	 * @see #setDefaultTimeZone(TimeZone)
	 * @see java.util.TimeZone#getDefault()
	 */
	public static TimeZone getTimeZone() {
		return getTimeZone(getLocaleContext());
	}

	/**
	 * 返回与给定用户上下文关联的 TimeZone（如果有），否则返回系统默认 TimeZone。
	 * 这实际上是 {@link java.util.TimeZone#getDefault()} 的替代品，
	 * 可以选择尊重用户级别的 TimeZone 设置。
	 *
	 * @param localeContext 要检查的用户级别区域设置上下文
	 * @return 当前 TimeZone，如果没有将特定的 TimeZone 关联到当前线程，则返回系统默认 TimeZone
	 * @see #getTimeZone()
	 * @see TimeZoneAwareLocaleContext#getTimeZone()
	 * @see #setDefaultTimeZone(TimeZone)
	 * @see java.util.TimeZone#getDefault()
	 * @since 5.0
	 */
	public static TimeZone getTimeZone(@Nullable LocaleContext localeContext) {
		// 如果 localeContext 是 TimeZoneAwareLocaleContext 类型，则获取其中的 TimeZone
		if (localeContext instanceof TimeZoneAwareLocaleContext) {
			TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			// 如果 timeZone 不为 null，则返回该 timeZone
			if (timeZone != null) {
				return timeZone;
			}
		}
		// 返回默认的 timeZone，如果 defaultTimeZone 不为 null，则返回 defaultTimeZone，否则返回系统默认的 TimeZone
		return (defaultTimeZone != null ? defaultTimeZone : TimeZone.getDefault());
	}

}
