package com.fel.interpreter;

import com.fel.context.FelContext;
import com.fel.parser.FelNode;

public class ConstInterpreter implements Interpreter {

	private Object value;

	public ConstInterpreter(FelContext context, FelNode node) {
		this.value = node.eval(context);
	}

	public Object interpret(FelContext context, FelNode node) {
		return value;
	}

}
