package com.theFox6.kleinerNerd.system;

public enum InstanceState {
    /**
     * An InstanceState that can be used by the owner to override running states
     * after making sure the bot is not running anymore.
     */
    OVERRIDE(false, "Der Zustand vom KleinenNerd wurde überschrieben."),

    STARTING(true, "Eine Instanz vom KleinenNerd scheint zu starten."),
    RUNNING(true, "Der KleineNerd scheint noch zu laufen."),
    SHUTTING_DOWN(true, "Der KleineNerd scheint sich gerade noch herunterzufahren."),
    SHUTDOWN(false, "Der KleineNerd ist zuletzt ordentlich runtergefahren."),
    DYING(true, "Der KleineNerd scheint sich an einem Fehler verschluckt zu haben."),
    UNKNOWN(true, "Der Zustand des KleinenNerds ist unbekannt.");

    public final boolean running;
    public final String message;

    InstanceState(boolean running, String msg) {
        this.message = msg;
        this.running = running;
    }
}