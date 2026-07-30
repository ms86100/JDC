package com.avionics_systems.plugin.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test-management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface TestSetResource {

    @POST
    @Path("/test-sets")
    Response createTestSet(String request);

    @GET
    @Path("/test-sets/{id}")
    Response getTestSet(@PathParam("id") String id);

    @POST
    @Path("/test-sets/{id}/tests")
    Response addTestsToSet(@PathParam("id") String id, String request);
}