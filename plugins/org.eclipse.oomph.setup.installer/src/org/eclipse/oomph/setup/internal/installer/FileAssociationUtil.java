/*
 * Copyright (c) 2016 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stepper - initial API and implementation
 */
package org.eclipse.oomph.setup.internal.installer;

import org.eclipse.oomph.util.IOUtil;
import org.eclipse.oomph.util.OS;
import org.eclipse.oomph.util.OomphPlugin.Preference;
import org.eclipse.oomph.util.PropertiesUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eike Stepper
 */
public class FileAssociationUtil
{
  public static final FileAssociationUtil INSTANCE = create();

  private static final String LAUNCHER = OS.getCurrentLauncher(false);

  private static final Preference PREF_CHECK_REGISTATION = SetupInstallerPlugin.INSTANCE.getConfigurationPreference("checkRegistration");

  private FileAssociationUtil()
  {
  }

  public final boolean canBeRegistered()
  {
    return LAUNCHER != null && !LAUNCHER.startsWith(PropertiesUtil.getTmpDir()) && !isRegistered();
  }

  public boolean isRegistered()
  {
    // Subclasses may override.
    return false;
  }

  public final void register()
  {
    register(LAUNCHER);
  }

  public void register(String launcher)
  {
    // Subclasses may override.
  }

  public static boolean isCheckRegistration()
  {
    return PREF_CHECK_REGISTATION.get(true);
  }

  public static void setCheckRegistration(boolean checkRegistration)
  {
    PREF_CHECK_REGISTATION.set(checkRegistration);
  }

  private static FileAssociationUtil create()
  {
    if (OS.INSTANCE.isWin())
    {
      return new Win();
    }

    if (OS.INSTANCE.isMac())
    {
      return new Mac();
    }

    if (OS.INSTANCE.isLinux())
    {
      return new Linux();
    }

    return new FileAssociationUtil();
  }

  /**
   * @author Eike Stepper
   */
  private static final class Win extends FileAssociationUtil
  {
    private static final String EXTENSION = ".setup";

    private static final String TYPE = "Oomph.setup.1";

    // private static final String HKEY_CLASSES = "HKEY_CLASSES_ROOT";

    private static final String HKEY_CLASSES = "HKEY_CURRENT_USER\\Software\\Classes";

    private static final String REG_SZ = "REG_SZ";

    private static final int SUCCESS = 0;

    public Win()
    {
    }

    @Override
    public boolean isRegistered()
    {
      String type = queryRegistryDefaultValue(HKEY_CLASSES + "\\" + EXTENSION);
      if (!TYPE.equals(type))
      {
        return false;
      }

      int xxx;
      // TODO Do we want to check for the "Content Type" value?

      String openCommand = queryRegistryDefaultValue(HKEY_CLASSES + "\\" + TYPE + "\\shell\\open\\command");
      if (!("\"" + LAUNCHER + "\" \"%1\"").equals(openCommand))
      {
        return false;
      }

      return true;
    }

    @Override
    public void register(String launcher)
    {
      File regFile = null;

      try
      {
        launcher = launcher.replace("\\", "\\\\");

        List<String> lines = new ArrayList<String>();
        lines.add("Windows Registry Editor Version 5.00");
        lines.add("");
        lines.add("[" + HKEY_CLASSES + "\\" + EXTENSION + "]");
        lines.add("@=\"" + TYPE + "\"");
        lines.add("\"Content Type\"=\"application/x-oomph-setup+xml\"");
        lines.add("");
        lines.add("[" + HKEY_CLASSES + "\\" + TYPE + "]");
        lines.add("@=\"Oomph Setup\"");
        lines.add("");
        lines.add("[" + HKEY_CLASSES + "\\" + TYPE + "\\DefaultIcon]");
        lines.add("@=\"\\\"" + launcher + "\\\"\"");
        lines.add("");
        lines.add("[" + HKEY_CLASSES + "\\" + TYPE + "\\shell\\edit\\command]");
        // Does not work: lines.add("@=REG_EXPAND_SZ:\"\\\"%SystemRoot%\\\\System32\\\\notepad.exe\\\" \\\"%1\\\"\"");
        lines.add("@=hex(2):22,00,25,00,53,00,79,00,73,00,74,00,65,00,6d,00,52,00,6f,00,6f,00,\\");
        lines.add(" 74,00,25,00,5c,00,73,00,79,00,73,00,74,00,65,00,6d,00,33,00,32,00,5c,00,6e,\\");
        lines.add(" 00,6f,00,74,00,65,00,70,00,61,00,64,00,2e,00,65,00,78,00,65,00,22,00,20,00,\\");
        lines.add(" 22,00,25,00,31,00,22,00,00,00");
        lines.add("");
        lines.add("[" + HKEY_CLASSES + "\\" + TYPE + "\\shell\\open]");
        lines.add("\"MUIVerb\"=\"Install...\"");
        lines.add("");
        lines.add("[" + HKEY_CLASSES + "\\" + TYPE + "\\shell\\open\\command]");
        lines.add("@=\"\\\"" + launcher + "\\\" \\\"%1\\\"\"");
        lines.add("");

        regFile = File.createTempFile("oomph-", ".reg");
        IOUtil.writeLines(regFile, "ASCII", lines);

        exec("reg.exe", "import", regFile.getAbsolutePath());
        exec("ie4uinit.exe", "-ClearIconCache");
      }
      catch (Throwable ex)
      {
        SetupInstallerPlugin.INSTANCE.log(ex);
      }
      finally
      {
        if (regFile != null)
        {
          IOUtil.deleteBestEffort(regFile, true);
        }
      }
    }

    private String queryRegistryDefaultValue(String key)
    {
      try
      {
        List<String> lines = exec("reg.exe", "query", key, "/ve");
        if (lines != null && lines.size() >= 3)
        {
          String line = lines.get(2);
          int pos = line.indexOf(REG_SZ);
          if (pos != -1)
          {
            return line.substring(pos + REG_SZ.length()).trim();
          }
        }
      }
      catch (Throwable ex)
      {
        //$FALL-THROUGH$
      }

      return null;
    }

    private List<String> exec(String... command)
    {
      Process process = null;

      try
      {
        process = new ProcessBuilder(command).start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        List<String> lines = new ArrayList<String>();
        String line;

        while ((line = bufferedReader.readLine()) != null)
        {
          lines.add(line);
        }

        if (process.waitFor() != SUCCESS)
        {
          return null;
        }

        return lines;
      }
      catch (Throwable ex)
      {
        return null;
      }
      finally
      {
        if (process != null)
        {
          IOUtil.closeSilent(process.getInputStream());
          IOUtil.closeSilent(process.getOutputStream());
          IOUtil.closeSilent(process.getErrorStream());
        }
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  private static final class Mac extends FileAssociationUtil
  {
    public Mac()
    {
    }
  }

  /**
   * @author Eike Stepper
   */
  private static final class Linux extends FileAssociationUtil
  {
    public Linux()
    {
    }

    @Override
    public void register(String launcher)
    {
      // desktop-file-validate ~/.local/share/applications/eclipse-inst.desktop

      // ~/.local/share/mime/packages/application-x-foobar.xml

      // <?xml version="1.0" encoding="UTF-8"?>
      // <mime-info xmlns="http://www.freedesktop.org/standards/shared-mime-info">
      // <mime-type type="application/x-foobar">
      // <comment>foo file</comment>
      // <icon name="application-x-foobar"/>
      // <glob-deleteall/>
      // <glob pattern="*.foo"/>
      // </mime-type>
      // </mime-info>

      // update-mime-database ~/.local/share/mime

      // gsettings get com.canonical.Unity.Launcher favorites

      // gsettings set com.canonical.Unity.Launcher favorites "['application://org.gnome.Nautilus.desktop', 'application://firefox.desktop',
      // 'application://ubuntu-software-center.desktop', 'application://unity-control-center.desktop', 'application://gnome-terminal.desktop',
      // 'unity://running-apps', 'application://eclipse-inst.desktop', 'application://gedit.desktop', 'unity://expo-icon', 'unity://devices']"

      // gtk-launch eclipse-inst
    }
  }
}
