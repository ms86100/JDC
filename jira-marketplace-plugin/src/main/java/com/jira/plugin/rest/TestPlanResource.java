package com.jira.plugin.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test-management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestPlanResource {

    @POST
    @Path("/test-plans")
    Response createTestPlan(String request);

    @GET
    @Path("/test-plans/{id}")
    Response getTestPlan(@PathParam("id") String id);

    @POST
    @Path("/test-plans/{id}/execute")
    Response executeTestPlan(@PathParam("id") String id, String request);
}