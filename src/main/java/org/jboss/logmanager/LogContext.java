/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.logmanager;

import java.security.Permission;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.logging.LoggingMXBean;
import java.util.logging.LoggingPermission;

/**
 * A logging context, for producing isolated logging environments.
 */
public final class LogContext {
    private static final LogContext SYSTEM_CONTEXT = new LogContext();

    static final Permission CREATE_CONTEXT_PERMISSION = new RuntimePermission("createLogContext", null);
    static final Permission SET_CONTEXT_SELECTOR_PERMISSION = new RuntimePermission("setLogContextSelector", null);
    static final Permission CONTROL_PERMISSION = new LoggingPermission("control", null);

    @SuppressWarnings({ "ThisEscapedInObjectConstruction" })
    private final LoggerNode rootLogger = new LoggerNode(this);
    @SuppressWarnings({ "ThisEscapedInObjectConstruction" })
    private final LoggingMXBean mxBean = new LoggingMXBeanImpl(this);

    /**
     * This lock is taken any time a change is made which affects multiple nodes in the hierarchy.
     */
    final Lock treeLock = new ReentrantLock(false);

    LogContext() {
    }

    /**
     * Create a new log context.  If a security manager is installed, the caller must have the {@code "createLogContext"}
     * {@link RuntimePermission RuntimePermission} to invoke this method.
     *
     * @return a new log context
     */
    public static LogContext create() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(CREATE_CONTEXT_PERMISSION);
        }
        return new LogContext();
    }

    /**
     * Get a logger with the given name from this logging context.
     *
     * @param name the logger name
     * @return the logger instance
     * @see java.util.logging.LogManager#getLogger(String)
     */
    public Logger getLogger(String name) {
        return rootLogger.getOrCreate(name).getOrCreateLogger();
    }

    /**
     * Get a logger with the given name from this logging context if it already exists.  If no logger of the given
     * name currently exists, {@code null} is returned.
     *
     * @param name the logger name
     * @return the logger instance, or {@code null} if none exists with the given name
     */
    public Logger getLoggerIfExists(String name) {
        final LoggerNode node = rootLogger.getIfExists(name);
        return node == null ? null : node.getLogger();
    }

    /**
     * Get the {@code LoggingMXBean} associated with this log context.
     *
     * @return the {@code LoggingMXBean} instance
     */
    public LoggingMXBean getMxBean() {
        return mxBean;
    }

    /**
     * Get the system log context.
     *
     * @return the system log context
     */
    public static LogContext getSystemLogContext() {
        return SYSTEM_CONTEXT;
    }

    /**
     * The default log context selector, which always returns the system log context.
     */
    public static final LogContextSelector DEFAULT_LOG_CONTEXT_SELECTOR = new LogContextSelector() {
        public LogContext getLogContext() {
            return SYSTEM_CONTEXT;
        }
    };

    private static volatile LogContextSelector logContextSelector = DEFAULT_LOG_CONTEXT_SELECTOR;

    /**
     * Get the currently active log context.
     *
     * @return the currently active log context
     */
    public static LogContext getLogContext() {
        return logContextSelector.getLogContext();
    }

    /**
     * Set a new log context selector.  If a security manager is installed, the caller must have the {@code "setLogContextSelector"}
     * {@link RuntimePermission RuntimePermission} to invoke this method.
     *
     * @param newSelector the new selector.
     */
    public static void setLogContextSelector(LogContextSelector newSelector) {
        if (newSelector == null) {
            throw new NullPointerException("newSelector is null");
        }
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SET_CONTEXT_SELECTOR_PERMISSION);
        }
        logContextSelector = newSelector;
    }

    static void checkAccess() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(CONTROL_PERMISSION);
        }
    }

    LoggerNode getRootLoggerNode() {
        return rootLogger;
    }
}
