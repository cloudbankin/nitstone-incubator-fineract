package org.apache.fineract.finabile.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.fineract.finabile.service.MonthlyAccrualReadPlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/monthlyAccrualDetail")
@Component
@Scope("singleton")
public class MonthlyAccrualDetailApiResource {
	 private final PlatformSecurityContext context;
	 private final MonthlyAccrualReadPlatformService monthlyAccrualReadPlatformService;
	 
	 @Autowired
	 public MonthlyAccrualDetailApiResource(final PlatformSecurityContext context,
			 final MonthlyAccrualReadPlatformService monthlyAccrualReadPlatformService) {
		 this.context = context;
		 this.monthlyAccrualReadPlatformService = monthlyAccrualReadPlatformService;
	 }
	 

//  High Mark
  @GET
  @Path("/accrualDetails")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Produces({ MediaType.APPLICATION_OCTET_STREAM })
  public Response retrieveLoanSchedule(@QueryParam("startDate") final String startDate, @QueryParam("endDate") final String endDate, @Context final UriInfo uriInfo) {

      this.context.authenticatedUser().validateHasReadPermission(MonthlyAccrualDetailApiConstants.MONTHLY_ACCRUAL_DETAIL_RESOURCE_NAME);
      Workbook result =this.monthlyAccrualReadPlatformService.retrieveMonthlyAccrualDetails(startDate, endDate);
		try {
          File newFile = new File("AccrualDetails.xls");
          result.write(new FileOutputStream(newFile));
			final ResponseBuilder response = Response.ok(new FileInputStream(newFile));
	        response.header("Content-Disposition", "attachment; filename=\"" + "AccrualDetails.xls"+ "\"");
	        response.header("Content-Type","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	        return response.build();

		} catch (Exception e) {
			e.printStackTrace();
			final ResponseBuilder response1 = Response.serverError();
			return response1.build();
			// TODO Auto-generated catch block
			
		}
		
  }
	
	
}
