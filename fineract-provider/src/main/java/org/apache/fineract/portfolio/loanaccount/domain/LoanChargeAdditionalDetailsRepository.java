package org.apache.fineract.portfolio.loanaccount.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanChargeAdditionalDetailsRepository extends JpaRepository<LoanChargeAdditionalDetails, Long>, JpaSpecificationExecutor<LoanChargeAdditionalDetails> {
	
	@Query("select lcad from LoanChargeAdditionalDetails lcad where lcad.loanCharge = :loanCharge")
	LoanChargeAdditionalDetails getLoanChargeAdditionalDetails(@Param("loanCharge") LoanCharge loanCharge);
}
