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

package org.springframework.scheduling.support;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Arrays;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <a href="https://www.manpagez.com/man/5/crontab/">crontab 表达式</a>
 * 的表示，可以计算下一次匹配的时间。
 *
 * <p>{@code CronExpression} 实例通过
 * {@link #parse(String)} 创建；下一次匹配通过
 * {@link #next(Temporal)} 确定。
 *
 * @author Arjen Poutsma
 * @since 5.3
 * @see CronTrigger
 */
public final class CronExpression {

	static final int MAX_ATTEMPTS = 366;

	private static final String[] MACROS = new String[] {
			"@yearly", "0 0 0 1 1 *",
			"@annually", "0 0 0 1 1 *",
			"@monthly", "0 0 0 1 * *",
			"@weekly", "0 0 0 * * 0",
			"@daily", "0 0 0 * * *",
			"@midnight", "0 0 0 * * *",
			"@hourly", "0 0 * * * *"
	};


	private final CronField[] fields;

	private final String expression;


	private CronExpression(
			CronField seconds,
			CronField minutes,
			CronField hours,
			CronField daysOfMonth,
			CronField months,
			CronField daysOfWeek,
			String expression) {

		// 逆序排列，以便先进行大范围的调整
		// 为确保最终结果为 0 纳秒，我们添加了一个额外字段
		this.fields = new CronField[]{daysOfWeek, months, daysOfMonth, hours, minutes, seconds, CronField.zeroNanos()};
		this.expression = expression;
	}


	/**
	 * 将给定的
	 * <a href="https://www.manpagez.com/man/5/crontab/">crontab 表达式</a>
	 * 字符串解析为 {@code CronExpression}。
	 * 该字符串包含六个以单个空格分隔的时间和日期字段：
	 * <pre>
	 * &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; 秒 (0-59)
	 * &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; 分 (0 - 59)
	 * &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; 时 (0 - 23)
	 * &#9474; &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; 日 (1 - 31)
	 * &#9474; &#9474; &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; 月 (1 - 12) (或 JAN-DEC)
	 * &#9474; &#9474; &#9474; &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; 星期 (0 - 7)
	 * &#9474; &#9474; &#9474; &#9474; &#9474; &#9474;          (0 或 7 表示周日，或 MON-SUN)
	 * &#9474; &#9474; &#9474; &#9474; &#9474; &#9474;
	 * &#42; &#42; &#42; &#42; &#42; &#42;
	 * </pre>
	 *
	 * <p>适用以下规则：
	 * <ul>
	 * <li>
	 * 字段可以是星号 ({@code *})，它始终表示 "从第一个到最后一个"。
	 * 对于 "日" 或 "星期" 字段，可以使用问号 ({@code ?}) 代替星号。
	 * </li>
	 * <li>
	 * 数字范围用连字符 ({@code -}) 分隔的两个数字表示。指定的范围是包含端点的。
	 * </li>
	 * <li>在范围（或 {@code *}) 之后使用 {@code /n} 指定
	 * 范围内数值的间隔。
	 * </li>
	 * <li>
	 * "月" 和 "星期" 字段也可以使用英文名称。
	 * 使用特定日期或月份的前三个字母（不区分大小写）。
	 * </li>
	 * <li>
	 * "日" 和 "星期" 字段可以包含 {@code L} 字符，表示 "最后"，
	 * 在每个字段中有不同的含义：
	 * <ul>
	 * <li>
	 * 在 "日" 字段中，{@code L} 表示 "当月最后一天"。
	 * 如果后跟负偏移量（即 {@code L-n}），则表示 "{@code n} 天前的最后一天"。
	 * 如果后跟 {@code W}（即 {@code LW}），则表示 "当月最后一个工作日"。
	 * </li>
	 * <li>
	 * 在 "星期" 字段中，{@code L} 表示 "当周最后一天"。
	 * 如果前缀为数字或三字母名称（即 {@code dL} 或 {@code DDDL}），
	 * 则表示 "当月第 {@code d}（或 {@code DDD}）个星期的最后一天"。
	 * </li>
	 * </ul>
	 * </li>
	 * <li>
	 * "日" 字段可以是 {@code nW}，表示 "离 {@code n} 日最近的工作日"。
	 * 如果 {@code n} 是星期六，则取前一天的星期五。
	 * 如果 {@code n} 是星期日，则取后一天的星期一，
	 * 当 {@code n} 为 {@code 1} 且是星期六时也是如此
	 * （即 {@code 1W} 表示 "当月第一个工作日"）。
	 * </li>
	 * <li>
	 * "星期" 字段可以是 {@code d#n}（或 {@code DDD#n}），
	 * 表示 "当月第 {@code n} 个星期 {@code d}（或 {@code DDD}）"。
	 * </li>
	 * </ul>
	 *
	 * <p>表达式示例：
	 * <ul>
	 * <li>{@code "0 0 * * * *"} = 每天每小时的整点。</li>
	 * <li><code>"*&#47;10 * * * * *"</code> = 每十秒一次。</li>
	 * <li>{@code "0 0 8-10 * * *"} = 每天 8、9 和 10 点。</li>
	 * <li>{@code "0 0 6,19 * * *"} = 每天上午 6:00 和晚上 7:00。</li>
	 * <li>{@code "0 0/30 8-10 * * *"} = 每天 8:00、8:30、9:00、9:30、10:00 和 10:30。</li>
	 * <li>{@code "0 0 9-17 * * MON-FRI"} = 工作日的整点九点到五点</li>
	 * <li>{@code "0 0 0 25 12 ?"} = 每年圣诞节午夜</li>
	 * <li>{@code "0 0 0 L * *"} = 每月最后一天午夜</li>
	 * <li>{@code "0 0 0 L-3 * *"} = 每月倒数第三天午夜</li>
	 * <li>{@code "0 0 0 1W * *"} = 每月第一个工作日午夜</li>
	 * <li>{@code "0 0 0 LW * *"} = 每月最后一个工作日午夜</li>
	 * <li>{@code "0 0 0 * * 5L"} = 每月最后一个星期五午夜</li>
	 * <li>{@code "0 0 0 * * THUL"} = 每月最后一个星期四午夜</li>
	 * <li>{@code "0 0 0 ? * 5#2"} = 每月第二个星期五午夜</li>
	 * <li>{@code "0 0 0 ? * MON#1"} = 每月第一个星期一午夜</li>
	 * </ul>
	 *
	 * <p>还支持以下宏：
	 * <ul>
	 * <li>{@code "@yearly"}（或 {@code "@annually"}）每年运行一次，即 {@code "0 0 0 1 1 *"}，</li>
	 * <li>{@code "@monthly"} 每月运行一次，即 {@code "0 0 0 1 * *"}，</li>
	 * <li>{@code "@weekly"} 每周运行一次，即 {@code "0 0 0 * * 0"}，</li>
	 * <li>{@code "@daily"}（或 {@code "@midnight"}）每天运行一次，即 {@code "0 0 0 * * *"}，</li>
	 * <li>{@code "@hourly"} 每小时运行一次，即 {@code "0 0 * * * *"}。</li>
	 * </ul>
	 * @param expression 要解析的表达式字符串
	 * @return 解析后的 {@code CronExpression} 对象
	 * @throws IllegalArgumentException 当表达式不符合 cron 格式时抛出
	 */
	public static CronExpression parse(String expression) {
		Assert.hasLength(expression, "Expression string must not be empty");

		expression = resolveMacros(expression);

		String[] fields = StringUtils.tokenizeToStringArray(expression, " ");
		if (fields.length != 6) {
			throw new IllegalArgumentException(String.format(
					"Cron expression must consist of 6 fields (found %d in \"%s\")", fields.length, expression));
		}
		try {
			CronField seconds = CronField.parseSeconds(fields[0]);
			CronField minutes = CronField.parseMinutes(fields[1]);
			CronField hours = CronField.parseHours(fields[2]);
			CronField daysOfMonth = CronField.parseDaysOfMonth(fields[3]);
			CronField months = CronField.parseMonth(fields[4]);
			CronField daysOfWeek = CronField.parseDaysOfWeek(fields[5]);

			return new CronExpression(seconds, minutes, hours, daysOfMonth, months, daysOfWeek, expression);
		}
		catch (IllegalArgumentException ex) {
			String msg = ex.getMessage() + " in cron expression \"" + expression + "\"";
			throw new IllegalArgumentException(msg, ex);
		}
	}

	/**
	 * 判断给定字符串是否表示有效的 cron 表达式。
	 * @param expression 要评估的表达式
	 * @return 如果给定表达式是有效的 cron 表达式则返回 {@code true}
	 * @since 5.3.8
	 */
	public static boolean isValidExpression(@Nullable String expression) {
		if (expression == null) {
			return false;
		}
		try {
			parse(expression);
			return true;
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}


	private static String resolveMacros(String expression) {
		expression = expression.trim();
		for (int i = 0; i < MACROS.length; i = i + 2) {
			if (MACROS[i].equalsIgnoreCase(expression)) {
				return MACROS[i + 1];
			}
		}
		return expression;
	}


	/**
	 * 计算匹配此表达式的下一个 {@link Temporal}。
	 * @param temporal 种子值
	 * @param <T> temporal 的类型
	 * @return 匹配此表达式的下一个 temporal，如果找不到则返回 {@code null}
	 */
	@Nullable
	public <T extends Temporal & Comparable<? super T>> T next(T temporal) {
		return nextOrSame(ChronoUnit.NANOS.addTo(temporal, 1));
	}


	@Nullable
	private <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal) {
		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			T result = nextOrSameInternal(temporal);
			if (result == null || result.equals(temporal)) {
				return result;
			}
			temporal = result;
		}
		return null;
	}

	@Nullable
	private <T extends Temporal & Comparable<? super T>> T nextOrSameInternal(T temporal) {
		for (CronField field : this.fields) {
			temporal = field.nextOrSame(temporal);
			if (temporal == null) {
				return null;
			}
		}
		return temporal;
	}


	@Override
	public int hashCode() {
		return Arrays.hashCode(this.fields);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof CronExpression) {
			CronExpression other = (CronExpression) o;
			return Arrays.equals(this.fields, other.fields);
		}
		else {
			return false;
		}
	}

	/**
	 * 返回用于创建此 {@code CronExpression} 的表达式字符串。
	 * @return 表达式字符串
	 */
	@Override
	public String toString() {
		return this.expression;
	}

}
