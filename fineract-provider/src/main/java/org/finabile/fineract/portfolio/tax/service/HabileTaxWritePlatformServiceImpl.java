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
package org.finabile.fineract.portfolio.tax.service;

import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.tax.domain.TaxComponent;
import org.apache.fineract.portfolio.tax.domain.TaxComponentRepository;
import org.apache.fineract.portfolio.tax.domain.TaxComponentRepositoryWrapper;
import org.apache.fineract.portfolio.tax.domain.TaxGroupRepository;
import org.apache.fineract.portfolio.tax.domain.TaxGroupRepositoryWrapper;
import org.apache.fineract.portfolio.tax.serialization.TaxValidator;
import org.apache.fineract.portfolio.tax.service.TaxAssembler;
import org.apache.fineract.portfolio.tax.service.TaxWritePlatformService;
import org.finabile.fineract.portfolio.tax.api.HabileTaxApiConstants;
import org.finabile.fineract.portfolio.tax.domain.TaxComponentAdditionalDetails;
import org.finabile.fineract.portfolio.tax.domain.TaxComponentAdditionalDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class HabileTaxWritePlatformServiceImpl implements HabileTaxWritePlatformService {

	private final TaxValidator validator;
	private final TaxAssembler taxAssembler;
	private final TaxComponentRepository taxComponentRepository;
	private final TaxComponentRepositoryWrapper taxComponentRepositoryWrapper;
//    private final TaxGroupRepository taxGroupRepository;
//    private final TaxGroupRepositoryWrapper taxGroupRepositoryWrapper;

	/** Habile changes start */
	private final TaxComponentAdditionalDetailsRepository taxComponentAdditionalDetailsRepository;
	private final TaxWritePlatformService taxWritePlatformService;
	private final FromJsonHelper fromApiJsonHelper;

	/** Habile changes end */

	@Autowired
	public HabileTaxWritePlatformServiceImpl(final TaxValidator validator, final TaxAssembler taxAssembler,
			final TaxComponentRepository taxComponentRepository, final TaxGroupRepository taxGroupRepository,
			final TaxComponentRepositoryWrapper taxComponentRepositoryWrapper,
			final TaxGroupRepositoryWrapper taxGroupRepositoryWrapper,
			final TaxComponentAdditionalDetailsRepository taxComponentAdditionalDetailsRepository,
			final TaxWritePlatformService taxWritePlatformService, final FromJsonHelper fromApiJsonHelper) {
		this.validator = validator;
		this.taxAssembler = taxAssembler;
		this.taxComponentRepository = taxComponentRepository;
		// this.taxGroupRepository = taxGroupRepository;
		this.taxComponentRepositoryWrapper = taxComponentRepositoryWrapper;
		// this.taxGroupRepositoryWrapper = taxGroupRepositoryWrapper;
		this.taxComponentAdditionalDetailsRepository = taxComponentAdditionalDetailsRepository;
		this.taxWritePlatformService = taxWritePlatformService;
		this.fromApiJsonHelper = fromApiJsonHelper;
	}

	@Override
	public CommandProcessingResult createTaxComponent(final JsonCommand command) {
		// this.validator.validateForTaxComponentCreate(command.json());
		TaxComponent taxComponent = null;
		String e = command.json();
		JsonObject jsonObject = new JsonParser().parse(e).getAsJsonObject();
		jsonObject.remove(HabileTaxApiConstants.taxComponentTypeParamName);
		String apiRequestBodyAsJson = jsonObject.toString();

		// String apiRequestBodyAsJson = gsonObj.toJson(jsonMap);
		String apiRequest = StringEscapeUtils.unescapeJava(apiRequestBodyAsJson.toString());
		final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequest.toString());

		final CommandWrapper wrapper = builder.createTaxComponent().build();
		final String json = wrapper.getJson();
		// CommandProcessingResult reScheduleCreateresult = null;
		JsonCommand changedCommand = null;
		JsonElement parsedCommand = this.fromApiJsonHelper.parse(json);
		changedCommand = JsonCommand.from(json, parsedCommand, this.fromApiJsonHelper, wrapper.getEntityName(),
				wrapper.getEntityId(), wrapper.getSubentityId(), wrapper.getGroupId(), wrapper.getClientId(),
				wrapper.getLoanId(), wrapper.getSavingsId(), wrapper.getTransactionId(), wrapper.getHref(),
				wrapper.getProductId(), wrapper.getCreditBureauId(), wrapper.getOrganisationCreditBureauId());

		final CommandProcessingResult result = this.taxWritePlatformService.createTaxComponent(changedCommand);
		taxComponent = this.taxComponentRepository.getOne(result.resourceId());
		String taxComponentType = null;
		if (command.parameterExists(HabileTaxApiConstants.taxComponentTypeParamName)) {
			taxComponentType = command.stringValueOfParameterNamed(HabileTaxApiConstants.taxComponentTypeParamName);
		}
		final TaxComponentAdditionalDetails taxComponentAdditionalDetails = new TaxComponentAdditionalDetails(
				taxComponent, taxComponentType);
		this.taxComponentAdditionalDetailsRepository.save(taxComponentAdditionalDetails);

		// this.taxComponentRepository.save(taxComponent);
		return new CommandProcessingResultBuilder() //
				.withCommandId(command.commandId()) //
				.withEntityId(taxComponentAdditionalDetails.getId()) //
				.build();
	}

	@Override
	public CommandProcessingResult updateTaxComponent(final Long id, final JsonCommand command) {
//		this.validator.validateForTaxComponentUpdate(command.json());
//		final TaxComponent taxComponent = this.taxComponentRepositoryWrapper.findOneWithNotFoundDetection(id);
//		this.validator.validateStartDate(taxComponent.startDate(), command);
//		Map<String, Object> changes = taxComponent.update(command);
		TaxComponent taxComponent = null;
		String e = command.json();
		JsonObject jsonObject = new JsonParser().parse(e).getAsJsonObject();
		jsonObject.remove(HabileTaxApiConstants.taxComponentTypeParamName);
		String apiRequestBodyAsJson = jsonObject.toString();

		// String apiRequestBodyAsJson = gsonObj.toJson(jsonMap);
		String apiRequest = StringEscapeUtils.unescapeJava(apiRequestBodyAsJson.toString());
		final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequest.toString());

		final CommandWrapper wrapper = builder.updateTaxComponent(id).build();
		final String json = wrapper.getJson();
		// CommandProcessingResult reScheduleCreateresult = null;
		JsonCommand changedCommand = null;
		JsonElement parsedCommand = this.fromApiJsonHelper.parse(json);
		changedCommand = JsonCommand.from(json, parsedCommand, this.fromApiJsonHelper, wrapper.getEntityName(),
				wrapper.getEntityId(), wrapper.getSubentityId(), wrapper.getGroupId(), wrapper.getClientId(),
				wrapper.getLoanId(), wrapper.getSavingsId(), wrapper.getTransactionId(), wrapper.getHref(),
				wrapper.getProductId(), wrapper.getCreditBureauId(), wrapper.getOrganisationCreditBureauId());

		this.taxWritePlatformService.updateTaxComponent(id, changedCommand);
		taxComponent = this.taxComponentRepository.getOne(id);
		final TaxComponentAdditionalDetails taxComponentAdditionalDetails = this.taxComponentAdditionalDetailsRepository
				.getTaxComponentAdditionalDetails(taxComponent);
		Map<String, Object> taxComponentAdditionalDetailsChanges = taxComponentAdditionalDetails.update(command);
		if (!taxComponentAdditionalDetailsChanges.isEmpty()) {
			this.taxComponentAdditionalDetailsRepository.save(taxComponentAdditionalDetails);
		}

//		this.validator.validateTaxComponentForUpdate(taxComponent);
//		this.taxComponentRepository.save(taxComponent);
		return new CommandProcessingResultBuilder() //
				.withEntityId(id) //
				.with(taxComponentAdditionalDetailsChanges).build();
	}

}
