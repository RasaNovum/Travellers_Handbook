package net.rasanovum.travellershandbook.data;

import net.rasanovum.travellershandbook.TravellersHandbook;

import java.util.Objects;

public record JournalRequirement(Type type, String id, long amount, Mode mode, String displayName) {
    public JournalRequirement(Type type, String id, long amount, Mode mode) {
        this(type, id, amount, mode, "");
    }

    public JournalRequirement {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(displayName, "displayName");
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Requirement id cannot be empty");
        }
        if (id.startsWith("#")) {
            if (type != Type.STRUCTURE || id.length() == 1) {
                throw new IllegalArgumentException("Only structure requirements support tags");
            }
            id = "#" + TravellersHandbook.parseId(id.substring(1)).toString();
        } else {
            id = TravellersHandbook.parseId(id).toString();
        }
        if (amount <= 0 || amount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Requirement amount must be between 1 and " + Integer.MAX_VALUE);
        }
        if (type != Type.ITEM && amount != 1) {
            throw new IllegalArgumentException("Amount is only supported for item requirements");
        }
    }

    public boolean isGoal() {
        return mode == Mode.GOAL;
    }

    public boolean isLock() {
        return mode == Mode.LOCK;
    }

    public enum Type {
        ADVANCEMENT,
        ITEM,
        STRUCTURE,
        ENTRY
    }

    public enum Mode {
        LOCK,
        GOAL
    }
}
