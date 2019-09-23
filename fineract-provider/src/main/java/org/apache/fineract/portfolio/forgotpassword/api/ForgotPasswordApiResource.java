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
package org.apache.fineract.portfolio.forgotpassword.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.PlatformEmailService;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.portfolio.forgotpassword.domain.PasswordResetToken;
import org.apache.fineract.portfolio.forgotpassword.repository.PasswordResetTokenRepository;
import org.apache.fineract.useradministration.api.AppUserApiConstant;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserPreviousPassword;
import org.apache.fineract.useradministration.domain.AppUserPreviousPasswordRepository;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.exception.PasswordPreviouslyUsedException;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Path("/forgotPassword")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Component
@Scope("singleton")
public class ForgotPasswordApiResource {

    /**
     * The set of parameters that are supported in response for
     * {@link AppUserData}.
     */
    private final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<>(Arrays.asList("id", "officeId", "officeName", "username",
            "firstname", "lastname", "email", "allowedOffices", "availableRoles", "selectedRoles", "staff"));

    private final Set<String> RESPONSE_TOKEN_DATA_PARAMETERS = new HashSet<>(Arrays.asList("id", "token", "userId", "expiryDate", "isExpired"));

    
    private final String resourceNameForPermissions = "USER";

    private final PlatformSecurityContext context;
    private final AppUserReadPlatformService readPlatformService;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final DefaultToApiJsonSerializer<AppUser> toApiJsonSerializer;
    private final DefaultToApiJsonSerializer<PasswordResetToken> toApiJsonTokenSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final AppUserRepository appUserRepository;
    private final PlatformEmailService emailService;
    private final PasswordResetTokenRepository tokenRepository;
    private final AppUserPreviousPasswordRepository appUserPreviewPasswordRepository;
    private final PlatformPasswordEncoder applicationPasswordEncoder;
    @Autowired
    public ForgotPasswordApiResource(final PlatformSecurityContext context, final AppUserReadPlatformService readPlatformService,
            final OfficeReadPlatformService officeReadPlatformService, final DefaultToApiJsonSerializer<AppUser> toApiJsonSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, final AppUserRepository appUserRepository,
            final PlatformEmailService emailService, final PasswordResetTokenRepository tokenRepository,
            final DefaultToApiJsonSerializer<PasswordResetToken> toApiJsonTokenSerializer,
            final PlatformPasswordEncoder applicationPasswordEncoder,final AppUserPreviousPasswordRepository appUserPreviewPasswordRepository) {
        this.context = context;
        this.readPlatformService = readPlatformService;
        this.officeReadPlatformService = officeReadPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.appUserRepository = appUserRepository;
        this.emailService = emailService;
        this.tokenRepository = tokenRepository;
        this.toApiJsonTokenSerializer = toApiJsonTokenSerializer;
        this.applicationPasswordEncoder = applicationPasswordEncoder;
        this.appUserPreviewPasswordRepository = appUserPreviewPasswordRepository;
    }

    @GET
    @Path("/resetLink/{username}")
    public String retrieveOne(@PathParam("username") final String username, @Context final UriInfo uriInfo) {

       // this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions, userId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        //AppUserData user = this.readPlatformService.retrieveUserByEmail(userName, email);
        
        AppUser appUser = this.appUserRepository.findAppUserByName(username);
       
        if(appUser != null && !StringUtils.isBlank(appUser.getEmail()))
        {
	        PasswordResetToken token = new PasswordResetToken();
	        token.setToken(UUID.randomUUID().toString());
	        token.setUser(appUser);
	        token.setExpiryDate(30);
	        tokenRepository.save(token);
	
	        String url = "https://nitstone.habiletechnologies.com/#";
	       
	        emailService.sendToResetPassword(appUser.getEmail(), appUser.getFirstname(), url, token);
        }
        if(appUser != null && StringUtils.isBlank(appUser.getEmail()))
        	appUser.setEmail("not found");
        
        return this.toApiJsonSerializer.serialize(settings, appUser, this.RESPONSE_DATA_PARAMETERS);
    }

    
    @GET
    @Path("/reset/{token}")
    public String retrieveRsetTokenDetails(@PathParam("token") final String token, @Context final UriInfo uriInfo) {

       // this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions, userId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        
        PasswordResetToken resetToken = this.tokenRepository.findByToken(token);
        
        if(resetToken.isExpired())
        {
        	resetToken = null;
        }

        return this.toApiJsonTokenSerializer.serialize(settings, resetToken, this.RESPONSE_TOKEN_DATA_PARAMETERS);
    }
    
    @POST
    @Path("/{userName}/{password}")
    public String updatePassword(@PathParam("userName") final String userName, @PathParam("password") final String password, @Context final UriInfo uriInfo) {

       // this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions, userId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        
        AppUser appUser = this.appUserRepository.findAppUserByName(userName);
        appUser.setPassword(password);
        
        final String encodePassword = this.applicationPasswordEncoder.encode(appUser);
        appUser.updatePassword(encodePassword);
        final AppUserPreviousPassword currentPasswordToSaveAsPreview = getCurrentPasswordToSaveAsPreview(appUser, encodePassword);
        
        appUserRepository.saveAndFlush(appUser);
        
        if (currentPasswordToSaveAsPreview != null) {
            appUserPreviewPasswordRepository.save(currentPasswordToSaveAsPreview);
        }
        
        return this.toApiJsonSerializer.serialize(settings, appUser, this.RESPONSE_DATA_PARAMETERS);
    }

    private AppUserPreviousPassword getCurrentPasswordToSaveAsPreview(final AppUser user, final String encodePassword) {

        final String passWordEncodedValue = encodePassword;

        AppUserPreviousPassword currentPasswordToSaveAsPreview = null;

        if (passWordEncodedValue != null) {

            PageRequest pageRequest = new PageRequest(0, AppUserApiConstant.numberOfPreviousPasswords, Sort.Direction.DESC, "removalDate");

            final List<AppUserPreviousPassword> nLastUsedPasswords = appUserPreviewPasswordRepository.findByUserId(user.getId(),
                    pageRequest);

            for (AppUserPreviousPassword aPreviewPassword : nLastUsedPasswords) {

                if (aPreviewPassword.getPassword().equals(passWordEncodedValue)) {

                throw new PasswordPreviouslyUsedException();

                }
            }

            currentPasswordToSaveAsPreview = new AppUserPreviousPassword(user);

        }

        return currentPasswordToSaveAsPreview;

    }

}