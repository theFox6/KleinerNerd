package com.theFox6.kleinerNerd.acheivements;

import com.theFox6.kleinerNerd.data.SendableImage;

public class Achievement {
    public SendableImage image;
    public String title;
    public String desc;

    public Achievement(String title, String description) {
        this(title,description,null);
    }

    public Achievement(String title, String description, SendableImage image) {
        this.title = title;
        this.desc = description;
        this.image = image;
    }
}
