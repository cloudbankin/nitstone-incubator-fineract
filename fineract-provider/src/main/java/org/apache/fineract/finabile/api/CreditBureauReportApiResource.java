package org.apache.fineract.finabile.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import org.apache.fineract.finabile.service.CreditBureauReportReadPlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.apache.fineract.finabile.api.CreditBureauReportDetailsApiConstants;


@Path("/creditBureauReportDetail")
@Component
@Scope("singleton")
public class CreditBureauReportApiResource {
	private final PlatformSecurityContext context;
	 private final CreditBureauReportReadPlatformService creditBureauReportReadPlatformService;
	 
	 @Autowired
	 public CreditBureauReportApiResource(final PlatformSecurityContext context,
			 final CreditBureauReportReadPlatformService creditBureauReportReadPlatformService) {
		 this.context = context;
		 this.creditBureauReportReadPlatformService = creditBureauReportReadPlatformService;
	 }
	
	@GET
    @Path("/creditBureauDetails")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    public Response retrieveLoanSchedule(@QueryParam("asOnDate") final String asOnDate, @QueryParam("closedFrom") final String closedFrom, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(CreditBureauReportDetailsApiConstants.CREDIT_BUREAU_DETAIL_RESOURCE_NAME);
        Workbook result =this.creditBureauReportReadPlatformService.retrieveCreditBureauReportData(asOnDate, closedFrom);
        String pattern = "ddMMyyyy";
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        String date=format.format(new Date());
		try {
            File newFile = new File("Credit Bureau report-"+ date +".xls");
            result.write(new FileOutputStream(newFile));
			final ResponseBuilder response = Response.ok(new FileInputStream(newFile));
	        response.header("Content-Disposition", "attachment; filename=\"" + "Credit Bureau report-"+ date +".xls"+ "\"");
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


