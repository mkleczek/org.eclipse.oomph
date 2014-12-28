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

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Eike Stepper
 */
public class StepIndicatorCanvas extends Canvas implements ControlListener, PaintListener
{
  private final StepIndicator stepIndicator;

  public StepIndicatorCanvas(Composite parent, int style)
  {
    super(parent, style);
    addControlListener(this);
    addPaintListener(this);

    stepIndicator = new StepIndicator(parent.getDisplay(), parent.getFont())
    {
      @Override
      protected Rectangle getClientArea()
      {
        return StepIndicatorCanvas.this.getClientArea();
      }
    };
  }

  @Override
  public void dispose()
  {
    stepIndicator.dispose();
    super.dispose();
  }

  public String[] getSteps()
  {
    return stepIndicator.getSteps();
  }

  public void setSteps(String... steps)
  {
    stepIndicator.setSteps(steps);
  }

  public int getCurrentStep()
  {
    return stepIndicator.getCurrentStep();
  }

  public void setCurrentStep(int currentStep)
  {
    stepIndicator.setCurrentStep(currentStep);
  }

  public void controlMoved(ControlEvent e)
  {
    // Do nothing.
  }

  public void controlResized(ControlEvent e)
  {
    stepIndicator.reset();
  }

  @Override
  public Point computeSize(int wHint, int hHint, boolean changed)
  {
    Point initialSize = super.computeSize(wHint, hHint, changed);
    initialSize.y = stepIndicator.getHeight(null);
    return initialSize;
  }

  public void paintControl(PaintEvent e)
  {
    stepIndicator.paint(e.gc, 0, 0);
  }
}
