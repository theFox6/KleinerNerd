package com.theFox6.kleinerNerd.achievements;

import com.theFox6.kleinerNerd.data.SendableImage;

public class Achievement {
    public SendableImage image;
    public String title;
    public String desc;
    public String roleName;

    public Achievement(String title, String description) {
        this(title,description,null);
    }

    public Achievement(String title, String description, SendableImage image) {
        this(title, description, image, null);
    }

    public Achievement(String title, String description, SendableImage image, String roleName) {
        this.title = title;
        this.desc = description;
        this.image = image;
        this.roleName = roleName;
    }
}
