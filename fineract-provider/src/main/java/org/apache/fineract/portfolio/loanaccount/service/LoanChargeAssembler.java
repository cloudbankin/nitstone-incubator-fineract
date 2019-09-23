/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanaccount.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.portfolio.address.data.AddressData;
import org.apache.fineract.portfolio.address.service.AddressReadPlatformServiceImpl;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeAdditionalDetails;
import org.apache.fineract.portfolio.charge.domain.ChargeAdditionalDetailsRepository;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargePaymentMode;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
/*import org.apache.fineract.portfolio.charge.domain.ChargeSlabDetails;
import org.apache.fineract.portfolio.charge.domain.ChargeSlabDetailsRepository;*/
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.exception.LoanChargeCannotBeAddedException;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeAdditionalDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeAdditionalDetailsRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDisbursementDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTrancheDisbursementCharge;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.finabile.fineract.portfolio.code.data.FinabileCodeValueData;
import org.finabile.fineract.portfolio.code.service.FinabileCodeValueReadPlatformService;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Service
public class LoanChargeAssembler {

    private final FromJsonHelper fromApiJsonHelper;
    private final ChargeRepositoryWrapper chargeRepository;
    private final LoanChargeRepository loanChargeRepository;
    private final LoanProductRepository loanProductRepository;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final ConfigurationDomainService configurationDomainService;
    private final FinabileCodeValueReadPlatformService finabileCodeValueReadPlatformService;
    private final AddressReadPlatformServiceImpl addressReadPlatformServiceImpl;
    private final ChargeAdditionalDetailsRepository chargeAdditionalDetailsRepository;
    private final LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepository;
    /*private final ChargeSlabDetailsRepository chargeSlabDetailsRepository;*/
    

    @Autowired
    public LoanChargeAssembler(final FromJsonHelper fromApiJsonHelper, final ChargeRepositoryWrapper chargeRepository,
            final LoanChargeRepository loanChargeRepository,final FinabileCodeValueReadPlatformService finabileCodeValueReadPlatformService,
            final ConfigurationDomainService configurationDomainService,/*final ChargeSlabDetailsRepository chargeSlabDetailsRepository,*/final AddressReadPlatformServiceImpl addressReadPlatformServiceImpl,
            final ClientRepositoryWrapper clientRepositoryWrapper,final LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepository,final ChargeAdditionalDetailsRepository chargeAdditionalDetailsRepository, final LoanProductRepository loanProductRepository) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.chargeRepository = chargeRepository;
        this.clientRepositoryWrapper = clientRepositoryWrapper;
        this.loanChargeRepository = loanChargeRepository;
        this.loanProductRepository = loanProductRepository;
        this.configurationDomainService=configurationDomainService;
        this.finabileCodeValueReadPlatformService=finabileCodeValueReadPlatformService;
        this.addressReadPlatformServiceImpl=addressReadPlatformServiceImpl;
        this.chargeAdditionalDetailsRepository=chargeAdditionalDetailsRepository;
        this.loanChargeAdditionalDetailsRepository=loanChargeAdditionalDetailsRepository;
        /*this.chargeSlabDetailsRepository=chargeSlabDetailsRepository;*/
    }

    public Set<LoanCharge> fromParsedJson(final JsonElement element, List<LoanDisbursementDetails> disbursementDetails) {
        JsonArray jsonDisbursement = this.fromApiJsonHelper.extractJsonArrayNamed("disbursementData", element);
        List<Long> disbursementChargeIds = new ArrayList<>();
/** Habile changes for tax calculation */
		final Long clientId = this.fromApiJsonHelper.extractLongNamed("clientId", element);
		Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);
		Office office = client.getOffice();

		final Long clientAddressTypeValueId = this.configurationDomainService.retrieveAddressTypeForClient();
		FinabileCodeValueData clientCodeValueData = this.finabileCodeValueReadPlatformService
				.getAddressTypeValue(clientAddressTypeValueId);
		AddressData clientAddress = this.addressReadPlatformServiceImpl.retrieveByEntityIdAndAddressType(client.getId(),
				"client", clientCodeValueData.getCodeValue());

		final Long officeAddressTypeValueId = this.configurationDomainService.retrieveAddressTypeForOffice();
		FinabileCodeValueData officeCodeValueData = this.finabileCodeValueReadPlatformService
				.getAddressTypeValue(officeAddressTypeValueId);
		AddressData officeAddress = this.addressReadPlatformServiceImpl.retrieveByEntityIdAndAddressType(office.getId(),
				"office", officeCodeValueData.getCodeValue());

		String taxComponentType = null;
		if (clientAddress.getStateName().equalsIgnoreCase(officeAddress.getStateName())) {
			taxComponentType = "Intra State";
		} else {
			taxComponentType = "Inter State";
		}
		/** Habile changes end */

        if (jsonDisbursement != null && jsonDisbursement.size() > 0) {
            for (int i = 0; i < jsonDisbursement.size(); i++) {
                final JsonObject jsonObject = jsonDisbursement.get(i).getAsJsonObject();
                if (jsonObject != null && jsonObject.getAsJsonPrimitive(LoanApiConstants.loanChargeIdParameterName) != null) {
                    String chargeIds = jsonObject.getAsJsonPrimitive(LoanApiConstants.loanChargeIdParameterName).getAsString();
                    if (chargeIds != null) {
                        if (chargeIds.indexOf(",") != -1) {
                            String[] chargeId = chargeIds.split(",");
                            for (String loanChargeId : chargeId) {
                                disbursementChargeIds.add(Long.parseLong(loanChargeId));
                            }
                        } else {
                            disbursementChargeIds.add(Long.parseLong(chargeIds));
                        }
                    }

                }
            }
        }

        final Set<LoanCharge> loanCharges = new HashSet<>();
        final BigDecimal principal = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("principal", element);
        final Integer numberOfRepayments = this.fromApiJsonHelper.extractIntegerWithLocaleNamed("numberOfRepayments", element);
        final Long productId = this.fromApiJsonHelper.extractLongNamed("productId", element);
        final LoanProduct loanProduct = this.loanProductRepository.findOne(productId);
        if (loanProduct == null) { throw new LoanProductNotFoundException(productId); }
        final boolean isMultiDisbursal = loanProduct.isMultiDisburseLoan();
        LocalDate expectedDisbursementDate = null;

        if (element.isJsonObject()) {
            final JsonObject topLevelJsonElement = element.getAsJsonObject();
            final String dateFormat = this.fromApiJsonHelper.extractDateFormatParameter(topLevelJsonElement);
            final Locale locale = this.fromApiJsonHelper.extractLocaleParameter(topLevelJsonElement);
            if (topLevelJsonElement.has("charges") && topLevelJsonElement.get("charges").isJsonArray()) {
                final JsonArray array = topLevelJsonElement.get("charges").getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {

                    final JsonObject loanChargeElement = array.get(i).getAsJsonObject();

                    final Long id = this.fromApiJsonHelper.extractLongNamed("id", loanChargeElement);
                    final Long chargeId = this.fromApiJsonHelper.extractLongNamed("chargeId", loanChargeElement);
                    BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalNamed("amount", loanChargeElement, locale);
                    final Integer chargeTimeType = this.fromApiJsonHelper.extractIntegerNamed("chargeTimeType", loanChargeElement, locale);
                    final Integer chargeCalculationType = this.fromApiJsonHelper.extractIntegerNamed("chargeCalculationType",
                            loanChargeElement, locale);
                    final LocalDate dueDate = this.fromApiJsonHelper
                            .extractLocalDateNamed("dueDate", loanChargeElement, dateFormat, locale);
                    final Integer chargePaymentMode = this.fromApiJsonHelper.extractIntegerNamed("chargePaymentMode", loanChargeElement,
                            locale);
                    if (id == null) {
                        final Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(chargeId);

                        if (chargeDefinition.isOverdueInstallment()) {

                            final String defaultUserMessage = "Installment charge cannot be added to the loan.";
                            throw new LoanChargeCannotBeAddedException("loanCharge", "overdue.charge", defaultUserMessage, null,
                                    chargeDefinition.getName());
                        }

                        ChargeTimeType chargeTime = null;
                        if (chargeTimeType != null) {
                            chargeTime = ChargeTimeType.fromInt(chargeTimeType);
                        }
                        ChargeCalculationType chargeCalculation = null;
                        if (chargeCalculationType != null) {
                            chargeCalculation = ChargeCalculationType.fromInt(chargeCalculationType);
                        }
                        ChargePaymentMode chargePaymentModeEnum = null;
                        if (chargePaymentMode != null) {
                            chargePaymentModeEnum = ChargePaymentMode.fromInt(chargePaymentMode);
                        }
                        /** Habile change Its used to get the slab amount */
						/*if (chargeDefinition != null && chargeDefinition.getChargeCalculation() == 7) {
							amount = getChargeAmountBySlab(chargeDefinition, principal, amount);
						}*/

						ChargeAdditionalDetails chargeAdditionalDetails = this.chargeAdditionalDetailsRepository
								.getChargeAdditionalDetails(chargeDefinition);

						LoanChargeAdditionalDetails loanChargeAdditionalDetails = null;
						if (chargeAdditionalDetails != null) {
							loanChargeAdditionalDetails = LoanChargeAdditionalDetails.fromJson(null,
									chargeAdditionalDetails.getIsEnabledFeeCalculationBasedOnTenure(),
									chargeAdditionalDetails.getIsEnabledAutoPaid(),
									chargeAdditionalDetails.getIsTaxIncluded(),
									chargeAdditionalDetails.getCreatedDate(), null, null,
									chargeAdditionalDetails.getCreatedById(), null);
							loanChargeAdditionalDetails.setTaxComponentType(taxComponentType);
						}

						if (!isMultiDisbursal) {

							final LoanCharge loanCharge = LoanCharge.createNewWithoutLoan(chargeDefinition, principal,
									amount, chargeTime, chargeCalculation, dueDate, chargePaymentModeEnum,
									numberOfRepayments, loanChargeAdditionalDetails);
							/**
							 * Habile changes to add loan charge to loan charge additional details and vice
							 * versa
							 */
							loanChargeAdditionalDetails.setLoanCharge(loanCharge);
							// loanCharge.setLoanChargeAdditionalDetails(loanChargeAdditionalDetails);
							/** Habile changes end */
							loanCharges.add(loanCharge);
						}
                        /*if (!isMultiDisbursal) {
                            final LoanCharge loanCharge = LoanCharge.createNewWithoutLoan(chargeDefinition, principal, amount, chargeTime,
                                    chargeCalculation, dueDate, chargePaymentModeEnum, numberOfRepayments);
                            loanCharges.add(loanCharge);
                        }*/ else {
                            if (topLevelJsonElement.has("disbursementData") && topLevelJsonElement.get("disbursementData").isJsonArray()) {
                                final JsonArray disbursementArray = topLevelJsonElement.get("disbursementData").getAsJsonArray();
                                if (disbursementArray.size() > 0) {
                                    JsonObject disbursementDataElement = disbursementArray.get(0).getAsJsonObject();
                                    expectedDisbursementDate = this.fromApiJsonHelper.extractLocalDateNamed(
                                            LoanApiConstants.disbursementDateParameterName, disbursementDataElement, dateFormat, locale);
                                }
                            }
                            
                            if ( ChargeTimeType.DISBURSEMENT.getValue().equals(chargeDefinition.getChargeTimeType())) {
                                for (LoanDisbursementDetails disbursementDetail : disbursementDetails) {
                                    LoanTrancheDisbursementCharge loanTrancheDisbursementCharge = null;
                                    if (chargeDefinition.isPercentageOfApprovedAmount()
                                            && disbursementDetail.expectedDisbursementDateAsLocalDate().equals(expectedDisbursementDate)) {
                                        final LoanCharge loanCharge = LoanCharge.createNewWithoutLoan(chargeDefinition, principal, amount,
                                                chargeTime, chargeCalculation, dueDate, chargePaymentModeEnum, numberOfRepayments, loanChargeAdditionalDetails);
                                        loanCharges.add(loanCharge);
                                        if (loanCharge.isTrancheDisbursementCharge()) {
                                            loanTrancheDisbursementCharge = new LoanTrancheDisbursementCharge(loanCharge,
                                                    disbursementDetail);
                                            loanCharge.updateLoanTrancheDisbursementCharge(loanTrancheDisbursementCharge);
                                        }
                                    } else {
                                        if (disbursementDetail.expectedDisbursementDateAsLocalDate().equals(expectedDisbursementDate)) {
                                            final LoanCharge loanCharge = LoanCharge.createNewWithoutLoan(chargeDefinition,
                                                    disbursementDetail.principal(), amount, chargeTime, chargeCalculation,
                                                    disbursementDetail.expectedDisbursementDateAsLocalDate(), chargePaymentModeEnum,
                                                    numberOfRepayments,
													loanChargeAdditionalDetails);
                                            loanCharges.add(loanCharge);
                                            if (loanCharge.isTrancheDisbursementCharge()) {
                                                loanTrancheDisbursementCharge = new LoanTrancheDisbursementCharge(loanCharge,
                                                        disbursementDetail);
                                                loanCharge.updateLoanTrancheDisbursementCharge(loanTrancheDisbursementCharge);
                                            }
                                        }
                                    }
                                }
                            } else if (ChargeTimeType.TRANCHE_DISBURSEMENT.getValue().equals(chargeDefinition.getChargeTimeType())) {
                                LoanTrancheDisbursementCharge loanTrancheDisbursementCharge = null;
                                for (LoanDisbursementDetails disbursementDetail : disbursementDetails) {
                                    if (ChargeTimeType.TRANCHE_DISBURSEMENT.getValue().equals(chargeDefinition.getChargeTimeType())) {
                                        final LoanCharge loanCharge = LoanCharge.createNewWithoutLoan(chargeDefinition,
                                                disbursementDetail.principal(), amount, chargeTime, chargeCalculation,
                                                disbursementDetail.expectedDisbursementDateAsLocalDate(), chargePaymentModeEnum,
                                                numberOfRepayments, loanChargeAdditionalDetails);
                                        loanCharges.add(loanCharge);
                                        loanTrancheDisbursementCharge = new LoanTrancheDisbursementCharge(loanCharge, disbursementDetail);
                                        loanCharge.updateLoanTrancheDisbursementCharge(loanTrancheDisbursementCharge);
                                    }
                                }
                            } else {
                                final LoanCharge loanCharge = LoanCharge.createNewWithoutLoan(chargeDefinition, principal, amount,
                                        chargeTime, chargeCalculation, dueDate, chargePaymentModeEnum, numberOfRepayments, loanChargeAdditionalDetails);
                                loanCharges.add(loanCharge);
                            }
                        }
                    } else {
                        final Long loanChargeId = id;
                        final LoanCharge loanCharge = this.loanChargeRepository.findOne(loanChargeId);
                        if (loanCharge != null) {
                            if(!loanCharge.isTrancheDisbursementCharge()
                                    || disbursementChargeIds.contains(loanChargeId)){
                            	/** Habile change start Its used to get the slab amount *//*
								if (loanCharge.getCharge() != null
										&& loanCharge.getCharge().getChargeCalculation() == 7) {
									amount = getChargeAmountBySlab(loanCharge.getCharge(), principal, amount);
								}
								if (loanCharge.getCharge() != null) {
									LoanChargeAdditionalDetails loanChargeAdditioanlDetails = this.loanChargeAdditionalDetailsRepository
											.getLoanChargeAdditionalDetails(loanCharge);

									loanCharge.setLoanChargeAdditionalDetails(loanChargeAdditioanlDetails);
								}
								*//** Habile change end */
                            	loanCharge.update(amount, dueDate, numberOfRepayments);
                                loanCharges.add(loanCharge);
                            }
                        }
                    }
                }
            }
        }

        return loanCharges;
    }
        
       /* *//**
    	 * Habile changes start
    	 * 
    	 * This method is used to get the charge amount based slab details
    	 * 
    	 * @param chargeDefinition
    	 * @param loanPrincipal
    	 * @return
    	 *//*
    	private BigDecimal getChargeAmountBySlab(Charge chargeDefinition, BigDecimal loanPrincipal,
    			BigDecimal chargeAmount) {
    		Long chargeId = chargeDefinition.getId();
    		BigDecimal amount = BigDecimal.ZERO;

    		ChargeSlabDetails chargeSlabDetails = chargeSlabDetailsRepository.getChargeAmountBySlab(chargeId,
    				loanPrincipal);
    		if (chargeSlabDetails != null) {
    			BigDecimal div = new BigDecimal(100);
    			if (chargeSlabDetails.getFeeEnum() == 1) {
    				amount = chargeSlabDetails.getAmount();
    			} else if (chargeSlabDetails.getFeeEnum() == 2) {
    				amount = (loanPrincipal.multiply(chargeSlabDetails.getAmount()).divide(div));
    			}
    		} else {
    			amount = chargeAmount;
    		}
    		return amount;
    	}

    	*//** Habile changes end */

    public Set<Charge> getNewLoanTrancheCharges(final JsonElement element) {
        final Set<Charge> associatedChargesForLoan = new HashSet<>();
        if (element.isJsonObject()) {
            final JsonObject topLevelJsonElement = element.getAsJsonObject();
            if (topLevelJsonElement.has("charges") && topLevelJsonElement.get("charges").isJsonArray()) {
                final JsonArray array = topLevelJsonElement.get("charges").getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    final JsonObject loanChargeElement = array.get(i).getAsJsonObject();
                    final Long id = this.fromApiJsonHelper.extractLongNamed("id", loanChargeElement);
                    final Long chargeId = this.fromApiJsonHelper.extractLongNamed("chargeId", loanChargeElement);
                    if (id == null) {
                        final Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(chargeId);
                        if (chargeDefinition.getChargeTimeType() == ChargeTimeType.TRANCHE_DISBURSEMENT.getValue()) {
                            associatedChargesForLoan.add(chargeDefinition);
                        }
                    }
                }
            }
        }
        return associatedChargesForLoan;
    }
}