package org.apache.fineract.portfolio.address.service;

import java.util.Collection;

import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.portfolio.address.domain.Address;
import org.apache.fineract.portfolio.address.domain.AddressOptionData;
import org.apache.fineract.portfolio.address.domain.AddressRepository;
import org.apache.fineract.portfolio.address.serialization.AddressCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientAddress;
import org.apache.fineract.portfolio.client.domain.ClientAddressRepository;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Service
public class AddressOptionsService {

	private final CodeValueReadPlatformService codeValueReadPlatformService;
	private final AddressCommandFromApiJsonDeserializer fromApiJsonDeserializer;
	private final CodeValueRepository codeValueRepository;
	private final AddressRepository addressRepository;
	private final ClientAddressRepository clientAddressRepository;
	private final ClientRepositoryWrapper clientRepositoryWrapper;

	@Autowired
	AddressOptionsService(CodeValueReadPlatformService codeValueReadPlatformService,
			AddressCommandFromApiJsonDeserializer fromApiJsonDeserializer, CodeValueRepository codeValueRepository,
			AddressRepository addressRepository, final ClientAddressRepository clientAddressRepository,
			ClientRepositoryWrapper clientRepositoryWrapper) {
		this.codeValueReadPlatformService = codeValueReadPlatformService;
		this.fromApiJsonDeserializer = fromApiJsonDeserializer;
		this.codeValueRepository = codeValueRepository;
		this.addressRepository = addressRepository;
		this.clientAddressRepository = clientAddressRepository;
		this.clientRepositoryWrapper = clientRepositoryWrapper;
	}

	public AddressOptionData getOptions() {

		final Collection<CodeValueData> addressTypes = this.codeValueReadPlatformService
				.retrieveCodeValuesByCode("ADDRESS_TYPE");

		final Collection<CodeValueData> countryOptions = this.codeValueReadPlatformService
				.retrieveCodeValuesByCode("COUNTRY");

		final Collection<CodeValueData> stateOptions = this.codeValueReadPlatformService
				.retrieveCodeValuesByCode("STATE");

		return AddressOptionData.fromSingleData(addressTypes, countryOptions, stateOptions);
	}

	public CommandProcessingResult addNewClientAddress(long entityId, String entityType, final JsonCommand command,
			long clientId) {
		CodeValue stateIdobj = null;
		CodeValue countryIdObj = null;
		long stateId;
		long countryId;
		ClientAddress clientAddressobj = new ClientAddress();
		final JsonArray addressArray = command.arrayOfParameterNamed("address");

		Address address = new Address();
		for (int i = 0; i < addressArray.size(); i++) {
			final JsonObject jsonObject = addressArray.get(i).getAsJsonObject();

			// validate every address
			this.fromApiJsonDeserializer.validateForCreate(jsonObject.toString(), true);

			if (jsonObject.get("stateProvinceId") != null) {
				stateId = jsonObject.get("stateProvinceId").getAsLong();
				stateIdobj = this.codeValueRepository.getOne(stateId);
			}

			if (jsonObject.get("countryId") != null) {
				countryId = jsonObject.get("countryId").getAsLong();
				countryIdObj = this.codeValueRepository.getOne(countryId);
			}

			final long addressTypeId = jsonObject.get("addressTypeId").getAsLong();
			final CodeValue addressTypeIdObj = this.codeValueRepository.getOne(addressTypeId);

			address = Address.fromJsonObject(entityId, entityType, jsonObject, stateIdobj, countryIdObj);
			this.addressRepository.save(address);

			/*
			 * final Long addressid = address.getId(); final Address addobj =
			 * this.addressRepository.getOne(addressid);
			 * 
			 * boolean isActive=false; if(jsonObject.get("isActive")!= null) { isActive=
			 * jsonObject.get("isActive").getAsBoolean(); }
			 * 
			 * clientAddressobj = ClientAddress.fromJson(isActive, client, addobj,
			 * addressTypeIdObj); this.clientAddressRepository.save(clientAddressobj);
			 */

		}

		return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(address.getId())
				.build();
	}
}
