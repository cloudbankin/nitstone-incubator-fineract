package org.apache.fineract.portfolio.charge.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 
 * @author Habile
 *
 */
public interface ChargeAdditionalDetailsRepository
		extends JpaRepository<ChargeAdditionalDetails, Long>, JpaSpecificationExecutor<ChargeAdditionalDetails> {

	@Query("select cad from ChargeAdditionalDetails cad where cad.charge = :charge")
	ChargeAdditionalDetails getChargeAdditionalDetails(@Param("charge") Charge charge);

	/*@Query("select cad from ChargeAdditionalDetails cad where cad.charge = :charge")
	ChargeAdditionalDetails getAdditionalDetails(@Param("charge") Charge charge);*/

	@Query("select cad from ChargeAdditionalDetails cad where cad.charge.id = :chargeId")
	ChargeAdditionalDetails retriveAdditionalDetails(@Param("chargeId") Long chargeId);

}