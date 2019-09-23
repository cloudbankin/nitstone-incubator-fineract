package org.finabile.fineract.portfolio.code.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.tax.service.TaxReadPlatformService;
import org.finabile.fineract.portfolio.code.data.FinabileCodeValueData;
import org.finabile.fineract.portfolio.code.service.FinabileCodeValueReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/finabileCodeValue")
@Component
@Scope("singleton")
public class FinabileCodeValueApiResources {

	private final String resourceNameForPermissions = "TAXCOMPONENT";

	private final PlatformSecurityContext context;
	private final TaxReadPlatformService taxReadPlatformService;
	private final FinabileCodeValueReadPlatformService finabileCodeValueReadPlatformService;
	private final DefaultToApiJsonSerializer<FinabileCodeValueData> toApiJsonSerializer;
	private final ApiRequestParameterHelper apiRequestParameterHelper;
	private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

	@Autowired
	public FinabileCodeValueApiResources(final PlatformSecurityContext context,
			final FinabileCodeValueReadPlatformService finabileCodeValueReadPlatformService,
			final DefaultToApiJsonSerializer<FinabileCodeValueData> toApiJsonSerializer,
			final ApiRequestParameterHelper apiRequestParameterHelper,
			final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
			final TaxReadPlatformService taxReadPlatformService) {
		this.context = context;
		this.finabileCodeValueReadPlatformService = finabileCodeValueReadPlatformService;
		this.toApiJsonSerializer = toApiJsonSerializer;
		this.apiRequestParameterHelper = apiRequestParameterHelper;
		this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
		this.taxReadPlatformService = taxReadPlatformService;

	}

	@GET
	@Path("/template")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveTemplate(@Context final UriInfo uriInfo) {

		this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

		final List<FinabileCodeValueData> addressTypes = this.finabileCodeValueReadPlatformService.getAddressTypes();

		StringBuilder codeValueAndId = new StringBuilder();
		for (FinabileCodeValueData addressType : addressTypes) {
			if (codeValueAndId.length() <= 0) {
				codeValueAndId.append(addressType.getCodeValue() + " : " + addressType.getCodeValueAndId());
			} else {
				codeValueAndId.append(", " + addressType.getCodeValue() + " : " + addressType.getCodeValueAndId());
			}
		}

		FinabileCodeValueData addressType = new FinabileCodeValueData(null, null, codeValueAndId.toString());

		final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper
				.process(uriInfo.getQueryParameters());
		return this.toApiJsonSerializer.serialize(settings, addressType);
	}
}
