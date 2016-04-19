package com.fanniemae.automation.data.utilities;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-22
 * 
 */
public class DriverShim implements Driver {
    private final Driver _driver;

    public DriverShim(Driver d) {
        this._driver = d;
    }

    @Override
    public boolean acceptsURL(String u) throws SQLException {
        return this._driver.acceptsURL(u);
    }

    @Override
    public Connection connect(String u, Properties p) throws SQLException {
        return this._driver.connect(u, p);
    }

    @Override
    public int getMajorVersion() {
        return this._driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return this._driver.getMinorVersion();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
        return this._driver.getPropertyInfo(u, p);
    }

    @Override
    public boolean jdbcCompliant() {
        return this._driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return this._driver.getParentLogger();
    }
}