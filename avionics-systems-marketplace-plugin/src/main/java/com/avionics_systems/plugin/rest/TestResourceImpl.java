package com.avionics_systems.plugin.rest;

import com.avionics_systems.plugin.config.PluginConfig;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test-management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestResourceImpl implements TestResource {

    @Override
    public Response createTest(String request) {
        PluginConfig config = PluginConfig.getInstance();
        String status = config != null ? config.getResponseStatusCreated() : "created";
        return Response.ok()
                .entity("{\"status\":\"" + status + "\"}")
                .build();
    }

    @Override
    public Response getTest(String id) {
        PluginConfig config = PluginConfig.getInstance();
        String testName = config != null ? config.getPlaceholderTestName() : "Sample Test";
        return Response.ok()
                .entity("{\"id\":\"" + id + "\",\"name\":\"" + testName + "\"}")
                .build();
    }

    @Override
    public Response updateTest(String id, String request) {
        PluginConfig config = PluginConfig.getInstance();
        String status = config != null ? config.getResponseStatusUpdated() : "updated";
        return Response.ok()
                .entity("{\"status\":\"" + status + "\",\"id\":\"" + id + "\"}")
                .build();
    }

    @Override
    public Response deleteTest(String id) {
        PluginConfig config = PluginConfig.getInstance();
        String status = config != null ? config.getResponseStatusDeleted() : "deleted";
        return Response.ok()
                .entity("{\"status\":\"" + status + "\",\"id\":\"" + id + "\"}")
                .build();
    }

    @Override
    public Response searchTests(String projectId, String requirementId, String label) {
        return Response.ok()
                .entity("{\"tests\":[],\"total\":0}")
                .build();
    }
}
