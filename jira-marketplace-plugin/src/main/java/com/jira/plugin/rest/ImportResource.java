package com.jira.plugin.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test-management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportResource {

    @POST
    @Path("/import/cucumber")
    Response importCucumber(String request);

    @POST
    @Path("/import/junit")
    Response importJUnit(String request);

    @GET
    @Path("/import/status/{jobId}")
    Response getImportStatus(@PathParam("jobId") String jobId);
}