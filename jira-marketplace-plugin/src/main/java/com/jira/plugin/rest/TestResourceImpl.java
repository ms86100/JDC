package com.jira.plugin.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test-management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestResourceImpl implements TestResource {

    @Override
    public Response createTest(String request) {
        return Response.ok()
                .entity("{\"status\":\"created\"}")
                .build();
    }

    @Override
    public Response getTest(String id) {
        return Response.ok()
                .entity("{\"id\":\"" + id + "\",\"name\":\"Sample Test\"}")
                .build();
    }

    @Override
    public Response updateTest(String id, String request) {
        return Response.ok()
                .entity("{\"status\":\"updated\",\"id\":\"" + id + "\"}")
                .build();
    }

    @Override
    public Response deleteTest(String id) {
        return Response.ok()
                .entity("{\"status\":\"deleted\",\"id\":\"" + id + "\"}")
                .build();
    }

    @Override
    public Response searchTests(String projectId, String requirementId, String label) {
        return Response.ok()
                .entity("{\"tests\":[],\"total\":0}")
                .build();
    }
}