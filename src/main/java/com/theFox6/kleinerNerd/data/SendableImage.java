package com.theFox6.kleinerNerd.data;

import com.theFox6.kleinerNerd.KleinerNerd;

import java.io.InputStream;

public class SendableImage {
    private String url;
    private String fileName;
    private String resourcePath;

    public String link() {
        if (url != null)
            return url;
        else
            return "attachment://" + fileName;
    }

    public boolean needsAttach() {
        return url == null;
    }

    public InputStream data() throws ResourceNotFoundException {
        return KleinerNerd.class.getResourceAsStream(resourcePath);
    }

    public String name() {
        return fileName;
    }
}
