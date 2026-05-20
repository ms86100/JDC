package com.jira.plugin.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test-management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface TestResource {

    @POST
    @Path("/tests")
    Response createTest(String request);

    @GET
    @Path("/tests/{id}")
    Response getTest(@PathParam("id") String id);

    @PUT
    @Path("/tests/{id}")
    Response updateTest(@PathParam("id") String id, String request);

    @DELETE
    @Path("/tests/{id}")
    Response deleteTest(@PathParam("id") String id);

    @GET
    @Path("/tests/search")
    Response searchTests(@QueryParam("projectId") String projectId,
                         @QueryParam("requirementId") String requirementId,
                         @QueryParam("label") String label);
}