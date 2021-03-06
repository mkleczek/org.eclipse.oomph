/*
 * Copyright (c) 2016 Ed Merks (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *    Ed Merks - initial API and implementation
 */
package org.eclipse.oomph.util;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Various static helper methods for dealing progress monitors.
 *
 * @author Ed Merks
 */
public final class MonitorUtil
{
  private MonitorUtil()
  {
  }

  @SuppressWarnings("all")
  public static org.eclipse.core.runtime.IProgressMonitorWithBlocking create(IProgressMonitor monitor, int ticks)
  {
    return new org.eclipse.core.runtime.SubProgressMonitor(monitor, ticks);
  }
}
