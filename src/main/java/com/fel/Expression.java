package com.fel;

import com.fel.context.FelContext;

public interface Expression {
	/**
	 * 求表达式的值
	 * @param context
	 * @return
	 */
	Object eval(FelContext context);
	
}
