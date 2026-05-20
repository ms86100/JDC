package com.jira.plugin.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test-management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestExecutionResource {

    @POST
    @Path("/test-executions")
    Response createExecution(String request);

    @GET
    @Path("/test-executions/{id}")
    Response getExecution(@PathParam("id") String id);

    @PUT
    @Path("/test-executions/{id}/steps/{stepId}")
    Response recordStepResult(@PathParam("id") String executionId,
                              @PathParam("stepId") String stepId,
                              String request);

    @PUT
    @Path("/test-executions/{id}/complete")
    Response completeExecution(@PathParam("id") String executionId);
}