package org.finabile.fineract.portfolio.tax.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.tax.domain.TaxComponent;
import org.finabile.fineract.portfolio.tax.api.HabileTaxApiConstants;

@SuppressWarnings("serial")
@Entity
@Table(name = "hab_tax_component_addtional_details")
public class TaxComponentAdditionalDetails extends AbstractPersistableCustom<Long> {

	@OneToOne
	@JoinColumn(name = "tax_component_id", nullable = false)
	private TaxComponent taxComponent;

	@Column(name = "tax_component_type")
	private String taxComponentType;

	public TaxComponentAdditionalDetails(final TaxComponent taxComponent, final String taxComponentType) {
		this.taxComponent = taxComponent;
		this.taxComponentType = taxComponentType;
	}

	public Map<String, Object> update(final JsonCommand command) {

		final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

		if (command.isChangeInStringParameterNamed(HabileTaxApiConstants.taxComponentTypeParamName,
				this.taxComponentType)) {
			final String newValue = command
					.stringValueOfParameterNamed(HabileTaxApiConstants.taxComponentTypeParamName);
			actualChanges.put(HabileTaxApiConstants.taxComponentTypeParamName, newValue);
			this.taxComponentType = newValue;
		}

		return actualChanges;
	}

	public TaxComponent getTaxComponent() {
		return taxComponent;
	}

	public void setTaxComponent(TaxComponent taxComponent) {
		this.taxComponent = taxComponent;
	}

	public String getTaxComponentType() {
		return taxComponentType;
	}

	public void setTaxComponentType(String taxComponentType) {
		this.taxComponentType = taxComponentType;
	}

}
