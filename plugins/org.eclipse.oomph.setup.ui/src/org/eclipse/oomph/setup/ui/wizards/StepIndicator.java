/*
 * Copyright (c) 2014 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.oomph.setup.ui.wizards;

import org.eclipse.oomph.setup.ui.SetupUIPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * @author Eike Stepper
 */
public abstract class StepIndicator
{
  private static final int BORDER = 3;

  private final Display display;

  private final Font normalFont;

  private final Font boldFont;

  private final Color whiteColor;

  private final Color grayColor;

  private final Color activeColor;

  private String[] steps;

  private int[] stepWidths;

  private int[] stepWidthsBold;

  private int extraWidth;

  private int currentStep;

  private int height = -1;

  private int clientAreaWidth;

  public StepIndicator(Display display, Font normalFont)
  {
    this.display = display;
    this.normalFont = normalFont;
    boldFont = SetupUIPlugin.getBoldFont(normalFont);

    whiteColor = display.getSystemColor(SWT.COLOR_WHITE);
    grayColor = display.getSystemColor(SWT.COLOR_DARK_GRAY);
    activeColor = new Color(display, 222, 230, 248);
  }

  public void dispose()
  {
    activeColor.dispose();
  }

  public String[] getSteps()
  {
    return steps;
  }

  public void setSteps(String... steps)
  {
    this.steps = steps;
  }

  public int getCurrentStep()
  {
    return currentStep;
  }

  public void setCurrentStep(int currentStep)
  {
    this.currentStep = currentStep;
  }

  public void reset()
  {
    stepWidths = null;
  }

  public int getHeight(GC gc)
  {
    boolean newGC = false;
    if (gc == null)
    {
      gc = new GC(display);
      newGC = true;
    }

    try
    {
      gc.setFont(boldFont);
      height = gc.stringExtent("Ag").y + 2 * BORDER;
      if (height % 2 == 0)
      {
        ++height;
      }

      return height + BORDER;
    }
    finally
    {
      if (newGC)
      {
        gc.dispose();
      }
    }
  }

  public void paint(GC gc, int x, int y)
  {
    gc.setAdvanced(true);
    gc.setAntialias(SWT.ON);

    if (height == -1)
    {
      getHeight(gc);
    }

    int stepCount = steps.length;
    if (stepWidths == null)
    {
      stepWidths = new int[stepCount];
      stepWidthsBold = new int[stepCount];
      int totalWidth = 0;

      for (int i = 0; i < stepCount; i++)
      {
        String step = steps[i];

        gc.setFont(boldFont);
        Point extent = gc.stringExtent(step);
        stepWidthsBold[i] = extent.x;

        gc.setFont(normalFont);
        extent = gc.stringExtent(step);
        stepWidths[i] = extent.x;

        totalWidth += stepWidthsBold[i];
        if (i != 0)
        {
          totalWidth += BORDER;
        }
      }

      Rectangle clientArea = getClientArea();
      clientAreaWidth = clientArea.width;
      extraWidth = Math.max(0, (clientAreaWidth - totalWidth) / stepCount);
    }

    Color oldBackground = gc.getBackground();
    Color oldForeground = gc.getForeground();

    for (int i = 0; i < stepCount; ++i)
    {
      String step = steps[i];
      int width = stepWidthsBold[i] + extraWidth;

      int x2 = i < stepCount - 1 ? x + width : clientAreaWidth - 1;
      int[] polygon = { x, y, //
          x2 - (i < stepCount + 1 ? 2 * BORDER : 0), y, //
          x2, y + height / 2,//
          x2 - (i < stepCount + 1 ? 2 * BORDER : 0), y + height - 1, //
          x, y + height - 1,//
          x + (i > 0 ? 2 * BORDER : 0), y + height / 2 };

      int stringWidth;
      if (i == currentStep)
      {
        gc.setBackground(activeColor);
        gc.setFont(boldFont);
        stringWidth = stepWidthsBold[i];
      }
      else
      {
        gc.setBackground(whiteColor);
        gc.setFont(normalFont);
        stringWidth = stepWidths[i];
      }

      gc.fillPolygon(polygon);
      gc.setBackground(oldBackground);

      gc.setForeground(grayColor);
      gc.drawPolygon(polygon);
      gc.setForeground(oldForeground);

      gc.drawString(step, x + width / 2 - stringWidth / 2, y + BORDER, true);
      x += width + BORDER;
    }
  }

  protected abstract Rectangle getClientArea();
}
