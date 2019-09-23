package org.finabile.fineract.portfolio.tax.domain;

import org.apache.fineract.portfolio.tax.domain.TaxComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaxComponentAdditionalDetailsRepository extends JpaRepository<TaxComponentAdditionalDetails, Long>,
		JpaSpecificationExecutor<TaxComponentAdditionalDetails> {

	@Query("select tcad from TaxComponentAdditionalDetails tcad where tcad.taxComponent = :taxComponent")
	public TaxComponentAdditionalDetails getTaxComponentAdditionalDetails(
			@Param("taxComponent") final TaxComponent taxComponent);

	@Query("select tcad from TaxComponentAdditionalDetails tcad where tcad.taxComponent.id = :taxComponentId")
	public TaxComponentAdditionalDetails getTaxComponentAdditionalDetailsById(
			@Param("taxComponentId") final Long taxComponentId);
}
