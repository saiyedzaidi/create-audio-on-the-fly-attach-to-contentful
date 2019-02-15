package com.sapient.jackbot;

import java.util.Map;

public class Properties {

	private Map<String, String> localProperties;

	public Properties() {
	}

	public Properties(Map<String, String> localProps) {
		this.localProperties = localProps;
	}

	public String getValue(String name) {
		String value = System.getenv(name);
		if (value == null) {
			if (localProperties != null) {
				value = localProperties.get(name);
			}
			if (value == null) {
				throw new RuntimeException("Missing property " + name);
			}
		}
		return value;
	}

}
