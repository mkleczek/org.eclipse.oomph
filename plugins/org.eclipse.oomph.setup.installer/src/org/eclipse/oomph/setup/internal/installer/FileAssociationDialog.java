/*
 * Copyright (c) 2016 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.oomph.setup.internal.installer;

import org.eclipse.oomph.setup.ui.AbstractConfirmDialog;
import org.eclipse.oomph.util.PropertiesUtil;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Eike Stepper
 */
public class FileAssociationDialog extends AbstractConfirmDialog
{
  private boolean register = true;

  public FileAssociationDialog()
  {
    super("Register File Association", 560, 270, "Check on each startup");
    setShellStyle(getShellStyle() & ~SWT.MIN & ~SWT.MAX);
  }

  public boolean isRegister()
  {
    return register;
  }

  @Override
  protected String getShellText()
  {
    return PropertiesUtil.getProductName();
  }

  @Override
  protected String getDefaultMessage()
  {
    return "Do you want to associate this installer with '.setup' files?";
  }

  @Override
  protected boolean getRememberButtonDefaultValue()
  {
    return FileAssociationUtil.isCheckRegistration();
  }

  @Override
  protected void createUI(Composite parent)
  {
    initializeDialogUnits(parent);

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
    composite.setLayout(new GridLayout(1, false));

    Button yesButton = new Button(composite, SWT.RADIO);
    yesButton.setText("Yes, associate this installer with '.setup' files.");
    yesButton.setLayoutData(new GridData(GridData.BEGINNING));
    yesButton.setSelection(register);
    Dialog.applyDialogFont(yesButton);
    yesButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        register = true;
      }
    });

    Button noButton = new Button(composite, SWT.RADIO);
    noButton.setText("No, skip association.");
    noButton.setLayoutData(new GridData(GridData.BEGINNING));
    yesButton.setSelection(!register);
    Dialog.applyDialogFont(noButton);
    noButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        register = false;
      }
    });
  }

  @Override
  protected void doCreateButtons(Composite parent)
  {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
  }
}
