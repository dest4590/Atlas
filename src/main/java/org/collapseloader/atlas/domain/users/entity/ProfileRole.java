package org.collapseloader.atlas.domain.users.entity;

public enum ProfileRole {
    USER,
    TESTER,
    ADMIN,
    DEVELOPER,
    OWNER;

    public boolean isTester() {
        return this == TESTER;
    }
}
