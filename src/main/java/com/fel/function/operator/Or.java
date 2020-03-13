package com.fel.function.operator;

import com.fel.context.FelContext;
import com.fel.parser.FelNode;

import java.util.List;

public class Or extends And {
	
	/** 
	 * 求逻辑或(||)
	 * @see And#logic(FelContext, List)
	 */
	Boolean logic(FelContext context, List<FelNode> children) {
		Boolean leftValue = toBoolean(context, children.get(0));
		if (leftValue.booleanValue()) {
			return leftValue;
		}
		return toBoolean(context, children.get(1));
	}
	
	@Override
	public String getName() {
		return "||";
	}

}
