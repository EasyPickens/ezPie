/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.utilities;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
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