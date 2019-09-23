package org.apache.fineract.portfolio.address.domain;

import java.util.Collection;

import org.apache.fineract.infrastructure.codes.data.CodeValueData;


@SuppressWarnings("unused")
public class AddressOptionData {
	
	private Collection<CodeValueData> addressTypes;
	private Collection<CodeValueData> countryOptions;
	private Collection<CodeValueData> stateOptions;

	public static AddressOptionData fromSingleData(Collection<CodeValueData> addressTypes,
			Collection<CodeValueData> countryOptions, Collection<CodeValueData> stateOptions) {
		return new AddressOptionData(addressTypes, countryOptions, stateOptions);
	}

	AddressOptionData(Collection<CodeValueData> addressTypes, Collection<CodeValueData> countryOptions,
			Collection<CodeValueData> stateOptions) {
		this.addressTypes = addressTypes;
		this.countryOptions = countryOptions;
		this.stateOptions = stateOptions;
	}

	public Collection<CodeValueData> getAddressTypes() {
		return addressTypes;
	}

	public void setAddressTypes(Collection<CodeValueData> addressTypes) {
		this.addressTypes = addressTypes;
	}

	public Collection<CodeValueData> getCountryOptions() {
		return countryOptions;
	}

	public void setCountryOptions(Collection<CodeValueData> countryOptions) {
		this.countryOptions = countryOptions;
	}

	public Collection<CodeValueData> getStateOptions() {
		return stateOptions;
	}

	public void setStateOptions(Collection<CodeValueData> stateOptions) {
		this.stateOptions = stateOptions;
	}
	
}
