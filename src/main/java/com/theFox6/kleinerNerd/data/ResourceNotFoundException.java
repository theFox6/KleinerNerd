package com.theFox6.kleinerNerd.data;

import java.io.FileNotFoundException;

public class ResourceNotFoundException extends FileNotFoundException {
    private static final long serialVersionUID = -3547413623636304260L;

    /**
     * constructs a resource not found exception
     *
     * @param resource the resource that was not found
     */
    public ResourceNotFoundException(String resource) {
        super("resource \"" + resource + "\" couldn't be found");
    }

    public ResourceNotFoundException(String resource, String location) {
        super("resource \"" + resource + "\" at: \"" + location + "\" couldn't be found");
    }
}
