package com.theFox6.kleinerNerd.data;

import com.theFox6.kleinerNerd.KleinerNerd;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.InputStream;

public class SendableImage {
    private String url;
    private String fileName;
    private String resourcePath;

    public SendableImage(String resource, String fileName) {
        this.resourcePath = resource;
        this.fileName = fileName;
    }

    public String link() {
        if (url != null)
            return url;
        else
            return "attachment://" + fileName;
    }

    public boolean needsAttach() {
        return url == null;
    }

    public InputStream data() {
        return KleinerNerd.class.getResourceAsStream(resourcePath);
    }

    public String name() {
        return fileName;
    }

    public FileUpload upload() {
        return FileUpload.fromData(data(), name());
    }
}
