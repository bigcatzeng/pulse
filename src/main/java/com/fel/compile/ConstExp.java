package com.fel.compile;

import com.fel.Expression;
import com.fel.context.FelContext;

public final class ConstExp implements Expression {
	public ConstExp(Object o) {
		this.value = o;
	}

	private final Object value;

	@Override
	public final Object eval(FelContext context) {
		return value;
	}
}
