package com.andrii.taskmanagement.config;

import org.testcontainers.containers.MySQLContainer;

public class CustomMySqlContainer extends MySQLContainer<CustomMySqlContainer> {
    private static final String IMAGE = "mysql:8";
    private static CustomMySqlContainer container;

    private CustomMySqlContainer() {
        super(IMAGE);
    }

    public static synchronized CustomMySqlContainer getInstance() {
        if (container == null) {
            container = new CustomMySqlContainer();
        }
        return container;
    }

    @Override
    public void stop() {}
}
