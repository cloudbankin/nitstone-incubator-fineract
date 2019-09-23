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
package org.finabile.fineract.portfolio.tax.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.tax.data.TaxComponentData;
import org.apache.fineract.portfolio.tax.service.TaxReadPlatformService;
import org.finabile.fineract.portfolio.tax.data.HabileTaxComponentData;
import org.finabile.fineract.portfolio.tax.domain.TaxComponentAdditionalDetails;
import org.finabile.fineract.portfolio.tax.service.HabileTaxReadPlatformService;
import org.finabile.fineract.portfolio.tax.service.TaxComponentTypeEnumerations;
import org.finabile.fineract.portfolio.tax.domain.TaxComponentAdditionalDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/habileTaxes/component")
@Component
@Scope("singleton")
public class HabileTaxComponentApiResource {

	private final String resourceNameForPermissions = "TAXCOMPONENT";

	private final PlatformSecurityContext context;
	private final TaxReadPlatformService taxReadPlatformService;
	private final HabileTaxReadPlatformService readPlatformService;
	private final DefaultToApiJsonSerializer<HabileTaxComponentData> toApiJsonSerializer;
	private final ApiRequestParameterHelper apiRequestParameterHelper;
	private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
	private final TaxComponentAdditionalDetailsRepository taxComponentAdditionalDetailsRepository;

	@Autowired
	public HabileTaxComponentApiResource(final PlatformSecurityContext context,
			final HabileTaxReadPlatformService readPlatformService,
			final DefaultToApiJsonSerializer<HabileTaxComponentData> toApiJsonSerializer,
			final ApiRequestParameterHelper apiRequestParameterHelper,
			final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
			final TaxReadPlatformService taxReadPlatformService,
			final TaxComponentAdditionalDetailsRepository taxComponentAdditionalDetailsRepository) {
		this.context = context;
		this.readPlatformService = readPlatformService;
		this.toApiJsonSerializer = toApiJsonSerializer;
		this.apiRequestParameterHelper = apiRequestParameterHelper;
		this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
		this.taxReadPlatformService = taxReadPlatformService;
		this.taxComponentAdditionalDetailsRepository = taxComponentAdditionalDetailsRepository;
	}

	@GET
	@Path("{taxComponentId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveTaxComponent(@PathParam("taxComponentId") final Long taxComponentId,
			@Context final UriInfo uriInfo, @QueryParam("template") Boolean template) {

		this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);
		final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper
				.process(uriInfo.getQueryParameters());
		TaxComponentData taxComponentData = this.taxReadPlatformService.retrieveTaxComponentData(taxComponentId);
		TaxComponentAdditionalDetails additionalDetails = this.taxComponentAdditionalDetailsRepository
				.getTaxComponentAdditionalDetailsById(taxComponentId);
		final EnumOptionData taxComponentType = TaxComponentTypeEnumerations
				.taxComponentType(additionalDetails.getTaxComponentType());
		HabileTaxComponentData habileTaxComponentData = new HabileTaxComponentData(taxComponentData, taxComponentType);

		if (template) {
			final List<EnumOptionData> taxComponentTypes = this.readPlatformService.retrieveTaxComponentTypeTemplate();
			habileTaxComponentData.setTaxComponentTypes(taxComponentTypes);
		}

		return this.toApiJsonSerializer.serialize(settings, habileTaxComponentData);
	}

	@GET
	@Path("/template")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveTemplate(@Context final UriInfo uriInfo) {

		this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

		final TaxComponentData taxComponentData = this.taxReadPlatformService.retrieveTaxComponentTemplate();
		final HabileTaxComponentData habileTaxComponentData = new HabileTaxComponentData();
		final List<EnumOptionData> taxComponentTypes = this.readPlatformService.retrieveTaxComponentTypeTemplate();
		habileTaxComponentData.setTaxComponentTypes(taxComponentTypes);
		habileTaxComponentData.setTaxComponentData(taxComponentData);

		final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper
				.process(uriInfo.getQueryParameters());
		return this.toApiJsonSerializer.serialize(settings, habileTaxComponentData);
	}

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String createTaxCompoent(final String apiRequestBodyAsJson) {

		final CommandWrapper commandRequest = new CommandWrapperBuilder().createHabileTaxComponent()
				.withJson(apiRequestBodyAsJson).build();

		final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

		return this.toApiJsonSerializer.serialize(result);
	}

	@PUT
	@Path("{taxComponentId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String updateTaxCompoent(@PathParam("taxComponentId") final Long taxComponentId,
			final String apiRequestBodyAsJson) {

		final CommandWrapper commandRequest = new CommandWrapperBuilder().updateHabileTaxComponent(taxComponentId)
				.withJson(apiRequestBodyAsJson).build();

		final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

		return this.toApiJsonSerializer.serialize(result);
	}

}