package org.finabile.fineract.portfolio.code.data;

public class FinabileCodeValueData {

	private Long codeValueId;
	private String codeValue;

	private String codeValueAndId;

	public FinabileCodeValueData(Long codeValueId, String codeValue, String codeValueAndId) {
		this.codeValueId = codeValueId;
		this.codeValue = codeValue;
		this.codeValueAndId = codeValueAndId;
	}

	public Long getCodeValueId() {
		return codeValueId;
	}

	public void setCodeValueId(Long codeValueId) {
		this.codeValueId = codeValueId;
	}

	public String getCodeValue() {
		return codeValue;
	}

	public void setCodeValue(String codeValue) {
		this.codeValue = codeValue;
	}

	public String getCodeValueAndId() {
		return codeValueAndId;
	}

	public void setCodeValueAndId(String codeValueAndId) {
		this.codeValueAndId = codeValueAndId;
	}

}
