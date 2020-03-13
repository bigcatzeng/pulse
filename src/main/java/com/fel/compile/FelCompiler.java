package com.fel.compile;

import com.fel.Expression;

public interface FelCompiler {
	
	/**
	 * 
	 * 编译代码，并创建Expression
	 * @param expr
	 * @return
	 */
	public Expression compile(JavaSource src);

}
