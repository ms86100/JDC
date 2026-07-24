package com.jira.plugin.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/test-management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ReportsResource {

    @GET
    @Path("/reports/summary")
    Response getSummary(@QueryParam("projectId") String projectId);

    @GET
    @Path("/reports/trend/{testId}")
    Response getTrend(@PathParam("testId") String testId);

    @GET
    @Path("/reports/coverage")
    Response getCoverage(@QueryParam("requirementId") String requirementId);
}