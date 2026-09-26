package net.nekozouneko.playerguard.paid;

public enum CreationConfirmationMode {
    GUI("GUI確認"),
    CHAT("チャット確認");

    public final String label;

    CreationConfirmationMode(String label) {
        this.label = label;
    }

    public CreationConfirmationMode next() {
        return this == GUI ? CHAT : GUI;
    }

    public static CreationConfirmationMode parse(String value) {
        if (value == null) return GUI;
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return GUI;
        }
    }
}
