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

import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.Trigger;
import org.eclipse.oomph.setup.User;
import org.eclipse.oomph.setup.Workspace;
import org.eclipse.oomph.setup.internal.core.SetupTaskPerformer;
import org.eclipse.oomph.setup.internal.core.util.CatalogManager;
import org.eclipse.oomph.setup.ui.SetupUIPlugin;
import org.eclipse.oomph.ui.HelpSupport.HelpProvider;
import org.eclipse.oomph.ui.OomphWizardDialog;
import org.eclipse.oomph.ui.PersistentButton;
import org.eclipse.oomph.ui.UIUtil;
import org.eclipse.oomph.util.ReflectUtil;
import org.eclipse.oomph.util.StringUtil;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eike Stepper
 */
public abstract class SetupWizardPage extends WizardPage implements HelpProvider
{
  private static final String TOGGLE_COMMAND_PREFIX = "toggleCommand:";

  private Composite checkComposite;

  public SetupWizardPage(String pageName)
  {
    super(pageName);
    setPageComplete(false);
  }

  @Override
  public SetupWizard getWizard()
  {
    return (SetupWizard)super.getWizard();
  }

  @Override
  protected IDialogSettings getDialogSettings()
  {
    IDialogSettings settings = super.getDialogSettings();
    String sectionName = getName();
    return UIUtil.getOrCreateSection(settings, sectionName);
  }

  public ComposedAdapterFactory getAdapterFactory()
  {
    return getWizard().getAdapterFactory();
  }

  public String getHelpPath()
  {
    String id = "Doc" + getClass().getSimpleName();
    return SetupWizard.HELP_FOLDER + id + ".html#" + id + "_1_table";
  }

  public ResourceSet getResourceSet()
  {
    return getWizard().getResourceSet();
  }

  public CatalogManager getCatalogManager()
  {
    return getWizard().getCatalogManager();
  }

  public Trigger getTrigger()
  {
    return getWizard().getTrigger();
  }

  public Installation getInstallation()
  {
    return getWizard().getInstallation();
  }

  public Workspace getWorkspace()
  {
    return getWizard().getWorkspace();
  }

  public User getUser()
  {
    return getWizard().getUser();
  }

  public SetupTaskPerformer getPerformer()
  {
    return getWizard().getPerformer();
  }

  public void setPerformer(SetupTaskPerformer performer)
  {
    getWizard().setPerformer(performer);
  }

  public boolean performCancel()
  {
    return true;
  }

  protected void handleInactivity(Display display, boolean inactive)
  {
  }

  public void enterPage(boolean forward)
  {
  }

  public void leavePage(boolean forward)
  {
  }

  public final void advanceToNextPage()
  {
    IWizardPage nextPage = getNextPage();
    getContainer().showPage(nextPage);
  }

  public final void createControl(Composite parent)
  {
    GridLayout gridLayout = createGridLayout(1);
    gridLayout.marginWidth = 5;

    Composite pageControl = new Composite(parent, SWT.NONE);
    pageControl.setLayout(gridLayout);
    super.setControl(pageControl);
    setPageComplete(false);

    if (false)
    {
      StepIndicatorCanvas canvas = new StepIndicatorCanvas(pageControl, SWT.NONE);
      canvas.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

      List<String> steps = new ArrayList<String>();
      IWizardPage[] pages = getWizard().getPages();
      for (int i = 0; i < pages.length; i++)
      {
        IWizardPage page = pages[i];
        steps.add(page.getTitle());

        if (page == this)
        {
          canvas.setCurrentStep(i);
        }
      }

      canvas.setSteps(steps.toArray(new String[steps.size()]));
    }

    Composite uiContainer = new Composite(pageControl, SWT.NONE);
    uiContainer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
    uiContainer.setLayout(createGridLayout(1));

    Point sizeHint = getSizeHint();

    GridData layoutData = new GridData(GridData.FILL_BOTH);
    layoutData.widthHint = sizeHint.x;
    layoutData.heightHint = sizeHint.y;

    Control ui = createUI(uiContainer);
    ui.setLayoutData(layoutData);

    createCheckButtons();
    createFooter(pageControl);
    parent.layout(true, true);
  }

  protected void createFooter(Composite parent)
  {
  }

  protected void createCheckButtons()
  {
  }

  protected final Button addCheckButton(String text, String toolTip, boolean defaultSelection, String persistenceKey)
  {
    if (checkComposite == null)
    {
      checkComposite = new Composite((Composite)getControl(), SWT.NONE);
      checkComposite.setLayout(createGridLayout(1));
      checkComposite.setLayoutData(new GridData());
    }
    else
    {
      GridLayout checkLayout = (GridLayout)checkComposite.getLayout();
      ++checkLayout.numColumns;
    }

    Button button;
    if (persistenceKey != null)
    {
      boolean toggleCommand = false;
      if (persistenceKey.startsWith(TOGGLE_COMMAND_PREFIX))
      {
        persistenceKey = persistenceKey.substring(TOGGLE_COMMAND_PREFIX.length());
        toggleCommand = UIUtil.WORKBENCH != null;
      }

      PersistentButton.Persistence persistence;
      if (toggleCommand)
      {
        persistence = new PersistentButton.ToggleCommandPersistence(persistenceKey);
      }
      else
      {
        persistence = new PersistentButton.DialogSettingsPersistence(getDialogSettings(), persistenceKey);
      }

      button = new PersistentButton(checkComposite, SWT.CHECK, defaultSelection, persistence);
    }
    else
    {
      button = new Button(checkComposite, SWT.CHECK);
      button.setSelection(defaultSelection);
    }

    button.setLayoutData(new GridData());
    button.setText(text);
    if (!StringUtil.isEmpty(toolTip))
    {
      button.setToolTipText(toolTip);
    }

    return button;
  }

  protected final Composite getCheckComposite()
  {
    return checkComposite;
  }

  @Override
  protected final void setControl(Control newControl)
  {
    throw new UnsupportedOperationException();
  }

  protected Point getSizeHint()
  {
    return new Point(800, 500);
  }

  protected abstract Control createUI(Composite parent);

  protected final void addHelpCallout(Control control, int number)
  {
    IWizardContainer container = getContainer();
    if (container instanceof OomphWizardDialog)
    {
      OomphWizardDialog dialog = (OomphWizardDialog)container;
      dialog.getHelpSupport().addHelpCallout(control, number);
    }
  }

  protected void setButtonState(int buttonID, boolean enabled)
  {
    try
    {
      IWizardContainer container = getContainer();
      Method method = ReflectUtil.getMethod(container.getClass(), "getButton", int.class);
      method.setAccessible(true);
      Button button = (Button)method.invoke(container, buttonID);
      button.setEnabled(enabled);
    }
    catch (Throwable ex)
    {
      // Ignore
    }
  }

  private TitleBarUpdater titleBarUpdater = new StepIndicatorTitleBarUpdater();

  @Override
  public void setImageDescriptor(ImageDescriptor imageDescriptor)
  {
    titleBarUpdater.setImageDescriptor(imageDescriptor);
  }

  @Override
  public String getTitle()
  {
    return titleBarUpdater.getTitle();
  }

  @Override
  public void setTitle(String title)
  {
    titleBarUpdater.setTitle(title);
  }

  @Override
  public void setDescription(String description)
  {
    titleBarUpdater.setDescription(description);
  }

  @Override
  public void setMessage(String message, int type)
  {
    titleBarUpdater.setMessage(message, type);
  }

  @Override
  public void setErrorMessage(String message)
  {
    titleBarUpdater.setErrorMessage(message);
  }

  private void superSetImageDescriptor(ImageDescriptor imageDescriptor)
  {
    super.setImageDescriptor(imageDescriptor);
  }

  public String superGetTitle()
  {
    return super.getTitle();
  }

  private void superSetTitle(String title)
  {
    super.setTitle(title);
  }

  private void superSetDescription(String description)
  {
    super.setDescription(description);
  }

  private void superSetMessage(String message, int type)
  {
    super.setMessage(message, type);
  }

  private void superSetErrorMessage(String message)
  {
    super.setErrorMessage(message);
  }

  protected static GridLayout createGridLayout(int numColumns)
  {
    GridLayout layout = new GridLayout(numColumns, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    return layout;
  }

  /**
   * @author Eike Stepper
   */
  public interface TitleBarUpdater
  {
    public void setImageDescriptor(ImageDescriptor imageDescriptor);

    public String getTitle();

    public void setTitle(String title);

    public void setDescription(String description);

    public void setMessage(String message, int type);

    public void setErrorMessage(String message);
  }

  /**
   * @author Eike Stepper
   */
  private final class DefaultTitleBarUpdater implements TitleBarUpdater
  {
    public void setImageDescriptor(ImageDescriptor imageDescriptor)
    {
      superSetImageDescriptor(imageDescriptor);
    }

    public String getTitle()
    {
      return superGetTitle();
    }

    public void setTitle(String title)
    {
      superSetTitle(title);
    }

    public void setDescription(String description)
    {
      superSetDescription(description);
    }

    public void setMessage(String message, int type)
    {
      superSetMessage(message, type);
    }

    public void setErrorMessage(String message)
    {
      superSetErrorMessage(message);
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class StepIndicatorTitleBarUpdater implements TitleBarUpdater
  {
    private static final int BORDER_WIDTH = 10;

    private final Image titleImage = SetupUIPlugin.INSTANCE.getSWTImage("install_wiz.png");

    private final ImageData titleImageData = titleImage.getImageData();

    private ImageDescriptor imageDescriptor;

    private String title;

    private String description;

    private String message;

    private int type;

    private String errorMessage;

    private boolean layoutChanged;

    public StepIndicatorTitleBarUpdater()
    {

    }

    public void setImageDescriptor(ImageDescriptor imageDescriptor)
    {
      this.imageDescriptor = imageDescriptor;
      updateTitleBar();
    }

    public String getTitle()
    {
      return title;
    }

    public void setTitle(String title)
    {
      this.title = title;
      updateTitleBar();
    }

    public void setDescription(String description)
    {
      this.description = description;
      updateTitleBar();
    }

    public void setMessage(String message, int type)
    {
      this.message = message;
      this.type = type;
      updateTitleBar();
    }

    public void setErrorMessage(String message)
    {
      errorMessage = message;
      updateTitleBar();
    }

    private void updateTitleBar()
    {
      WizardDialog dialog = (WizardDialog)getContainer();
      if (dialog == null)
      {
        return;
      }

      final Shell shell = dialog.getShell();

      StepIndicator stepIndicator = new StepIndicator(shell.getDisplay(), getControl().getFont())
      {
        @Override
        protected Rectangle getClientArea()
        {
          Rectangle clientArea = shell.getClientArea();
          clientArea.width -= titleImageData.width + 2 * BORDER_WIDTH;
          return clientArea;
        }
      };

      List<String> steps = new ArrayList<String>();
      IWizardPage[] pages = getWizard().getPages();
      boolean currentPage = isCurrentPage();

      for (int i = 0; i < pages.length; i++)
      {
        IWizardPage page = pages[i];
        steps.add(page.getTitle());

        if (currentPage)
        {
          stepIndicator.setCurrentStep(i);
        }
      }

      stepIndicator.setSteps(steps.toArray(new String[steps.size()]));

      if (currentPage)
      {

        int width = shell.getClientArea().width;
        int height = titleImageData.height;

        final Image buffer = new Image(shell.getDisplay(), width, height);

        GC gc = new GC(buffer);
        gc.drawImage(titleImage, width - titleImageData.width, 0);
        stepIndicator.paint(gc, BORDER_WIDTH, 6);
        gc.dispose();

        ImageDescriptor descriptor = new ImageDescriptor()
        {
          @Override
          public ImageData getImageData()
          {
            return buffer.getImageData();
          }
        };

        superSetImageDescriptor(descriptor);

        if (!layoutChanged)
        {
          shell.layout(true, true);
          layoutChanged = true;
        }

        // dialog.setTitleImage(buffer);
        // dialog.updateTitleBar();
      }
    }
  }
}
