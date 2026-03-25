package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application configuration.
 * Sets the versioned base path for all API resources.
 *
 * Lifecycle note: By default, JAX-RS resource classes are request-scoped
 * (a new instance per request). We use a shared DataStore singleton to
 * safely manage in-memory data across requests.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Jersey discovers resources via package scanning configured in Main.java
}
