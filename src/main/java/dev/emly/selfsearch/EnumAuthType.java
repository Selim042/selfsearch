package dev.emly.selfsearch;

public enum EnumAuthType {
	NONE(0), API_KEY(1), USER_AND_PASS(2), ID_AND_SECRET(2);

	private int fieldCount;

	EnumAuthType(int fieldCount) {
		this.fieldCount = fieldCount;
	}

	public int getFieldCount() {
		return fieldCount;
	}
}
