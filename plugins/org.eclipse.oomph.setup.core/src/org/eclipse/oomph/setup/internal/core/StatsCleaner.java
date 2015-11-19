/*
 * Copyright (c) 2015 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.oomph.setup.internal.core;

import org.eclipse.oomph.setup.Index;
import org.eclipse.oomph.setup.Scope;
import org.eclipse.oomph.setup.internal.core.util.SetupCoreUtil;
import org.eclipse.oomph.util.IOUtil;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Usage:
 *
 * Copy the <b>source code</b> of the following two pages into the file /org.eclipse.oomph.setup.core/stats.txt :
 *
 *   https://dev.eclipse.org/committers/committertools/stats.php?filename=/stats/oomph/product/
 *   https://dev.eclipse.org/committers/committertools/stats.php?filename=/stats/oomph/project/
 *
 * Then run this application.
 *
 * @author Eike Stepper
 */
public class StatsCleaner implements IApplication
{
  private static final int PATH_OFFSET = "http://download/eclipse.org".length();

  private static final Pattern PATTERN = Pattern.compile("javascript:fnViewDaily\\('([^']*)', 'daily'\\);");

  private final List<String> allowedURIs = new ArrayList<String>();

  public Object start(IApplicationContext context) throws Exception
  {
    String queryLocation = null;

    String[] arguments = (String[])context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
    if (arguments != null)
    {
      for (int i = 0; i < arguments.length; ++i)
      {
        String option = arguments[i];
        if ("-queryLocation".equals(option))
        {
          queryLocation = arguments[++i];
        }
      }
    }

    if (queryLocation == null)
    {
      return null;
    }

    System.out.println("Stats Cleaner");
    System.out.println("=============");
    System.out.println();

    ResourceSet resourceSet = SetupCoreUtil.createResourceSet();
    Resource resource = resourceSet.getResource(SetupContext.INDEX_SETUP_URI, true);
    Index index = (Index)resource.getContents().get(0);

    for (TreeIterator<EObject> it = index.eAllContents(); it.hasNext();)
    {
      EObject object = it.next();
      if (object instanceof Scope)
      {
        Scope scope = (Scope)object;

        String name = scope.getQualifiedName();
        if (name.startsWith("self") || name.startsWith("user"))
        {
          continue;
        }

        allow(true, scope);
        allow(false, scope);
      }
    }

    for (String line : IOUtil.readLines(new File(queryLocation), "UTF-8"))
    {
      Matcher matcher = PATTERN.matcher(line);
      if (matcher.find())
      {
        String uri = matcher.group(1);
        if (!isAllowed(uri))
        {
          System.out.println(uri);
        }
      }
    }

    return null;
  }

  public void stop()
  {
  }

  private void allow(boolean success, Scope scope)
  {
    URI uri = SetupCoreUtil.getStatsURI(success, scope);
    if (uri != null)
    {
      String str = uri.toString().substring(PATH_OFFSET);
      allowedURIs.add(str);
    }
  }

  private boolean isAllowed(String line)
  {
    for (String allowedURI : allowedURIs)
    {
      if (line.startsWith(allowedURI))
      {
        return true;
      }
    }

    return false;
  }
}
