package com.fel.common;

import com.fel.FelEngine;
import com.fel.FelEngineImpl;
import com.fel.function.FunMgr;
import com.fel.function.operator.big.*;
import com.fel.parser.AntlrParser;
import com.fel.parser.NodeAdaptor;
import com.fel.security.RegexSecurityMgr;
import com.fel.security.SecurityMgr;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class FelBuilder {

	/**
	 * 构建安全管理器
	 * @return
	 */
	public static SecurityMgr newSecurityMgr() {
		Set<String> disables = new HashSet<String>();
		disables.add(System.class.getCanonicalName() + ".*");
		disables.add(Runtime.class.getCanonicalName() + ".*");
		disables.add(Process.class.getCanonicalName() + ".*");
		disables.add(File.class.getCanonicalName() + ".*");
		disables.add("java.net.*");
		disables.add("com.fel.compile.*");
		disables.add("com.fel.security.*");
		return new RegexSecurityMgr(null, disables);
	}

	public static void main(String[] args) {
		System.out.println(System.class.getCanonicalName());
		System.out.println(Long.toBinaryString(0xFFFFFFFFl));
		System.out.println(Long.toBinaryString(Long.MAX_VALUE).length());
		System.out.println(Long.MAX_VALUE);
	}

	public static FelEngine bigNumberEngine() {
		return bigNumberEngine(100);
	}

	public static FelEngine engine() {
		return new FelEngineImpl();
	}

	public static FelEngine bigNumberEngine(int setPrecision) {
		FelEngine engine = new FelEngineImpl();
		FunMgr funMgr = engine.getFunMgr();
		engine.setParser(new AntlrParser(engine, new NodeAdaptor() {
			@Override
			protected Number newFloatNumber(String text) {
				char lastChar = text.charAt(text.length() - 1);
				if (lastChar == 'l' || lastChar == 'L' || lastChar == 'd' || lastChar == 'D' || lastChar == 'f'
						|| lastChar == 'F') {
					text = text.substring(0, text.length() - 1);
				}
				return new BigDecimal(text);
			}
		}));
		funMgr.add(new BigAdd());
		funMgr.add(new BigSub());
		funMgr.add(new BigMul());
		funMgr.add(new BigDiv(setPrecision));
		funMgr.add(new BigMod());
		funMgr.add(new BigGreaterThan());
		funMgr.add(new BigGreaterThanEqual());
		funMgr.add(new BigLessThan());
		funMgr.add(new BigLessThanEqual());
		return engine;
	}

}
