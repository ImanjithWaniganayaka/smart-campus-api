package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Cross-cutting API observability filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter so
 * that every HTTP interaction is logged — without polluting individual
 * resource methods with Logger.info() calls. This is the recommended
 * JAX-RS approach for cross-cutting concerns (logging, auth, CORS, etc.).
 */
@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(ApiLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("[REQUEST]  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("[RESPONSE] %s %s -> HTTP %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()));
    }
}
