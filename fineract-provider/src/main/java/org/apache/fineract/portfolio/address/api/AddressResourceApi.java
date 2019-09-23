package org.apache.fineract.portfolio.address.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.portfolio.address.domain.AddressOptionData;
import org.apache.fineract.portfolio.address.service.AddressOptionsService;
import org.apache.fineract.portfolio.client.data.ClientIdentifierData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/addressOptions")
@Component
@Scope("singleton")
public class AddressResourceApi {

	private final AddressOptionsService addressOptionsService;
	private final DefaultToApiJsonSerializer<ClientIdentifierData> toApiJsonSerializer;
	private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

	@Autowired
	AddressResourceApi(AddressOptionsService addressOptionsService,
			DefaultToApiJsonSerializer<ClientIdentifierData> toApiJsonSerializer,
			PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
		this.addressOptionsService = addressOptionsService;
		this.toApiJsonSerializer = toApiJsonSerializer;
		this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
	}

	/**
	 * Get address default options
	 * 
	 * @return String
	 */
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAddressOptions() {
		AddressOptionData addressOptionData = this.addressOptionsService.getOptions();
		return this.toApiJsonSerializer.serialize(addressOptionData);
	}

	@DELETE
	@Path("/{addressId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String updateStatus(@PathParam("addressId") final Long addressId) {

		final CommandWrapper commandRequest = new CommandWrapperBuilder().updateAddressStatus(addressId).build();

		final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

		return this.toApiJsonSerializer.serialize(result);
	}

}
