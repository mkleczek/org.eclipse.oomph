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
package org.eclipse.oomph.setup.internal.installer;

import org.eclipse.oomph.base.Annotation;
import org.eclipse.oomph.p2.core.BundlePool;
import org.eclipse.oomph.p2.core.P2Util;
import org.eclipse.oomph.p2.internal.ui.AgentManagerDialog;
import org.eclipse.oomph.setup.AnnotationConstants;
import org.eclipse.oomph.setup.Product;
import org.eclipse.oomph.setup.ProductVersion;
import org.eclipse.oomph.setup.Scope;
import org.eclipse.oomph.setup.internal.installer.SimpleInstallerDialog.ToolButton;
import org.eclipse.oomph.setup.ui.SetupUIPlugin;
import org.eclipse.oomph.setup.ui.wizards.ProductPage;
import org.eclipse.oomph.setup.ui.wizards.SetupWizardPage;
import org.eclipse.oomph.ui.StackComposite;
import org.eclipse.oomph.ui.UIUtil;
import org.eclipse.oomph.util.IOUtil;
import org.eclipse.oomph.util.PropertiesUtil;
import org.eclipse.oomph.util.StringUtil;

import org.eclipse.emf.common.util.URI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public class SimpleInstallerVariablePage extends SimpleInstallerPage
{
  private static final String POOL_ENABLED_KEY = "poolEnabled";

  private static final String TEXT_LOG = "Show installation log";

  private static final String TEXT_README = "Show readme file";

  private static final String TEXT_LAUNCH = "Launch product";

  private final Map<String, ProductVersion> productVersions = new HashMap<String, ProductVersion>();

  private Product product;

  private ProductVersion selectedProductVersion;

  private String readmePath;

  private BundlePool pool;

  private boolean poolEnabled;

  private Browser browser;

  private Composite versionComposite;

  private CCombo versionCombo;

  private ToolButton bitness32Button;

  private ToolButton bitness64Button;

  private int bitness = 64;

  private Label javaLabel;

  private Text javaText;

  private ToolButton javaButton;

  private Text folderText;

  private ToolButton folderButton;

  private ToolButton poolButton;

  private Thread installThread;

  private StackComposite installStack;

  private ToolButton installButton;

  private boolean installed;

  private ProgressBar progressBar;

  private Link progressLabel;

  private ToolButton cancelButton;

  private ToolButton backButton;

  public SimpleInstallerVariablePage(final Composite parent, int style, final SimpleInstallerDialog dialog)
  {
    super(parent, style, dialog);

    Preferences preferences = SetupInstallerPlugin.INSTANCE.getConfigurationPreferences();
    poolEnabled = preferences.getBoolean(POOL_ENABLED_KEY, true);
    enablePool(poolEnabled);

    GridLayout layout = ProductPage.createGridLayout(4);
    layout.marginWidth = SimpleInstallerDialog.MARGIN_WIDTH;
    layout.marginBottom = 20;
    layout.horizontalSpacing = 5;
    layout.verticalSpacing = 5;
    setLayout(layout);

    // Row 1
    GridData browserLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false, layout.numColumns, 1);
    browserLayoutData.heightHint = 142;

    Composite browserComposite = new Composite(this, SWT.BORDER);
    browserComposite.setLayoutData(browserLayoutData);
    browserComposite.setLayout(new FillLayout());

    browser = new Browser(browserComposite, SWT.NO_SCROLL);
    browser.addLocationListener(new LocationAdapter()
    {
      @Override
      public void changing(LocationEvent event)
      {
        String url = event.location;
        if (!"about:blank".equals(url))
        {
          SimpleInstallerDialog.openSytemBrowser(url);
          event.doit = false;
        }
      }
    });

    // Row 2
    new Label(this, SWT.NONE);
    new Label(this, SWT.NONE);
    new Label(this, SWT.NONE);
    new Label(this, SWT.NONE);

    // Row 3
    createLabel("Product Version ");

    versionComposite = new Composite(this, SWT.NONE);
    versionComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    versionComposite.setLayout(SetupWizardPage.createGridLayout(4));

    versionCombo = new CCombo(versionComposite, SWT.BORDER|SWT.READ_ONLY);
    versionCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    versionCombo.setFont(font);
    versionCombo.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        String label = versionCombo.getItem(versionCombo.getSelectionIndex());
        productVersionSelected(productVersions.get(label));
      }
    });

    new Label(versionComposite, SWT.NONE);

    bitness32Button = new ToolButton(versionComposite, SWT.RADIO, SetupInstallerPlugin.INSTANCE.getSWTImage("simple/32bit.png"), true);
    bitness32Button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    bitness32Button.setSelection(false);
    bitness32Button.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        bitness = 32;
        bitness32Button.setSelection(true);
        bitness64Button.setSelection(false);
      }
    });

    bitness64Button = new ToolButton(versionComposite, SWT.RADIO, SetupInstallerPlugin.INSTANCE.getSWTImage("simple/64bit.png"), true);
    bitness64Button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    bitness64Button.setSelection(true);
    bitness64Button.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        bitness = 64;
        bitness32Button.setSelection(false);
        bitness64Button.setSelection(true);
      }
    });

    new Label(this, SWT.NONE);
    new Label(this, SWT.NONE);

    // Row 4
    javaLabel = createLabel("Java VM ");

    javaText = new Text(this, SWT.BORDER);
    javaText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    javaText.setFont(font);

    javaButton = new ToolButton(this, SWT.PUSH, SetupInstallerPlugin.INSTANCE.getSWTImage("simple/folder.png"), false);
    javaButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
    javaButton.setToolTipText("Select Java VM...");

    new Label(this, SWT.NONE);

    // Row 5
    createLabel("Installation Folder ");

    folderText = new Text(this, SWT.BORDER);
    folderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    folderText.setFont(font);

    folderButton = new ToolButton(this, SWT.PUSH, SetupInstallerPlugin.INSTANCE.getSWTImage("simple/folder.png"), false);
    folderButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
    folderButton.setToolTipText("Select installation folder...");

    poolButton = new ToolButton(this, SWT.PUSH, getBundlePoolImage(), false);
    poolButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
    poolButton.setToolTipText("Configure bundle pool...");
    poolButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        manageBundlePools();
      }
    });

    // Row 6
    backButton = new ToolButton(this, SWT.PUSH, SetupInstallerPlugin.INSTANCE.getSWTImage("simple/back.png"), true);
    backButton.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, true, 1, 2));
    backButton.setToolTipText("Back");
    backButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        dialog.backSelected();
      }
    });

    installStack = new StackComposite(this, SWT.NONE);
    installStack.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    cancelButton = new ToolButton(this, SWT.PUSH, SetupInstallerPlugin.INSTANCE.getSWTImage("simple/cancel.png"), false);
    cancelButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
    cancelButton.setToolTipText("Cancel");
    cancelButton.setVisible(false);
    cancelButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        installCancel();
      }
    });

    new Label(this, SWT.NONE);

    installButton = new ToolButton(installStack, SWT.PUSH, SetupInstallerPlugin.INSTANCE.getSWTImage("simple/download_small.png"), false);

    final Composite progressComposite = new Composite(installStack, SWT.NONE);
    progressComposite.setLayout(SetupWizardPage.createGridLayout(1));

    GridData layoutData2 = new GridData(SWT.FILL, SWT.CENTER, true, true);
    layoutData2.heightHint = 28;

    progressBar = new ProgressBar(progressComposite, SWT.NONE);
    progressBar.setLayoutData(layoutData2);

    installButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        if (installed)
        {
          launch();
        }
        else
        {
          setEnabled(false);

          progressBar.setSelection(0);
          progressLabel.setForeground(null);
          cancelButton.setVisible(true);

          installStack.setTopControl(progressComposite);

          install();
        }
      }
    });

    installStack.setTopControl(installButton);

    // Row 7
    progressLabel = new Link(this, SWT.WRAP);
    progressLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
    progressLabel.setFont(SetupInstallerPlugin.getFont(font, URI.createURI("font:///9/bold")));
    progressLabel.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        if (TEXT_LOG.equals(e.text))
        {
          SimpleInstallerDialog.openSytemBrowser("file:/C:/develop/oomph/eclipse/configuration/org.eclipse.oomph.setup/setup.log");
        }
        else if (TEXT_README.equals(e.text))
        {
          if (readmePath != null)
          {
            SimpleInstallerDialog.openSytemBrowser("file:/C:/develop/oomph/eclipse/" + readmePath);
          }
        }
        else if (TEXT_LAUNCH.equals(e.text))
        {
          launch();
          return;
        }

        installButton.setFocus();
      }
    });
  }

  protected void productVersionSelected(ProductVersion productVersion)
  {
    if (selectedProductVersion != productVersion)
    {
      selectedProductVersion = productVersion;
      ProductPage.saveProductVersionSelection(installer.getCatalogManager(), selectedProductVersion);

      String label = "Java";
      String requiredJavaVersion = selectedProductVersion.getRequiredJavaVersion();
      if (!StringUtil.isEmpty(requiredJavaVersion))
      {
        label += " " + requiredJavaVersion;
      }

      label += " VM ";
      javaLabel.setText(label);
    }
  }

  @Override
  public boolean setFocus()
  {
    return folderText.setFocus();
  }

  public void setProduct(Product product)
  {
    this.product = product;

    StringBuilder builder = new StringBuilder();
    builder.append("<html><style TYPE=\"text/css\"><!-- ");
    builder.append("table{border:none; border-collapse:collapse}");
    builder.append(".label{font-size:1.1em; font-weight:700}");
    builder.append(".description{font-size:14px; color:#333}");
    builder.append(".col1{padding:10px; width:64px; text-align:center; vertical-align:top}");
    builder
        .append(" --></style><body style=\"background-color:#fafafa; overflow:auto; margin:10px; font-family:'Open Sans','Helvetica Neue',Helvetica,Arial,sans-serif\"><table>\n");

    SimpleInstallerProductPage.renderProduct(builder, product, true, null);
    browser.setText(SimpleInstallerProductPage.getHtml(builder), true);

    productVersions.clear();
    versionCombo.removeAll();

    ProductVersion defaultProductVersion = ProductPage.getDefaultProductVersion(installer.getCatalogManager(), product);
    int i = 0;
    int selection = 0;

    for (ProductVersion productVersion : product.getVersions())
    {
      if (defaultProductVersion == null)
      {
        defaultProductVersion = productVersion;
      }

      String label = productVersion.getLabel();
      if (label == null)
      {
        label = productVersion.getName();
      }

      productVersions.put(label, productVersion);
      versionCombo.add(label);

      if (productVersion == defaultProductVersion)
      {
        selection = i;
      }

      ++i;
    }

    versionCombo.pack();
    Point size = versionCombo.getSize();
    size.x += 10;
    versionCombo.setSize(size);
    versionComposite.layout();

    versionCombo.select(selection);
    versionCombo.setSelection(new Point(0, 0));
    productVersionSelected(defaultProductVersion);

    folderText.setText(getDefaultInstallationFolder());

    installStack.setTopControl(installButton);
    installButton.setImage(SetupInstallerPlugin.INSTANCE.getSWTImage("simple/download_small.png"));
    installButton.setToolTipText("Install");
    installed = false;

    progressLabel.setText("");
    setEnabled(true);
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    versionCombo.setEnabled(enabled);
    bitness32Button.setEnabled(enabled);
    bitness32Button.setVisible(enabled || bitness == 32);
    bitness64Button.setEnabled(enabled);
    bitness64Button.setVisible(enabled || bitness == 64);
    javaText.setEnabled(enabled);
    javaButton.setEnabled(enabled);
    folderText.setEnabled(enabled);
    folderButton.setEnabled(enabled);
    poolButton.setEnabled(enabled);
    backButton.setEnabled(enabled);
  }

  private String getDefaultInstallationFolder()
  {
    String name = product.getName();
    int lastDot = name.lastIndexOf('.');
    if (lastDot != -1)
    {
      name = name.substring(lastDot + 1);
    }

    for (int i = 0; i < Integer.MAX_VALUE; i++)
    {
      String filename = name;
      if (i != 0)
      {
        filename += i;
      }

      File folder = new File(PropertiesUtil.USER_HOME, filename);
      if (!folder.exists())
      {
        return folder.getAbsolutePath();
      }
    }

    throw new IllegalStateException("User home is full");
  }

  private Image getBundlePoolImage()
  {
    return SetupInstallerPlugin.INSTANCE.getSWTImage("simple/bundle_pool_" + (poolEnabled ? "enabled" : "disabled") + ".png");
  }

  private void enablePool(boolean poolEnabled)
  {
    if (this.poolEnabled != poolEnabled)
    {
      this.poolEnabled = poolEnabled;

      try
      {
        Preferences preferences = SetupInstallerPlugin.INSTANCE.getConfigurationPreferences();
        preferences.putBoolean(POOL_ENABLED_KEY, poolEnabled);
        preferences.flush();
      }
      catch (BackingStoreException ex)
      {
        SetupInstallerPlugin.INSTANCE.log(ex);
      }
    }

    if (poolEnabled)
    {
      pool = P2Util.getAgentManager().getDefaultBundlePool(SetupUIPlugin.INSTANCE.getSymbolicName());
    }
    else
    {
      pool = null;
    }

    if (poolButton != null)
    {
      poolButton.setImage(getBundlePoolImage());
    }
  }

  private Label createLabel(String text)
  {
    Label label = new Label(this, SWT.RIGHT);
    label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    label.setText(text);
    label.setFont(font);
    return label;
  }

  private void manageBundlePools()
  {
    final boolean[] enabled = { poolEnabled };

    AgentManagerDialog dialog = new AgentManagerDialog(getShell())
    {
      @Override
      protected void createUI(Composite parent)
      {
        final Button enabledButton = new Button(parent, SWT.CHECK);
        enabledButton.setText("Shared bundle pool enabled");
        enabledButton.setSelection(poolEnabled);
        enabledButton.addSelectionListener(new SelectionAdapter()
        {
          @Override
          public void widgetSelected(SelectionEvent e)
          {
            enabled[0] = enabledButton.getSelection();
            getComposite().setEnabled(enabled[0]);
          }
        });

        new Label(parent, SWT.NONE);
        super.createUI(parent);
        getComposite().setEnabled(poolEnabled);
      }

      @Override
      protected void createButtonsForButtonBar(Composite parent)
      {
        super.createButtonsForButtonBar(parent);
        Button button = getButton(IDialogConstants.OK_ID);
        if (button != null)
        {
          button.setEnabled(false);
        }
      }

      @Override
      protected void elementChanged(Object element)
      {
        Button button = getButton(IDialogConstants.OK_ID);
        if (button != null)
        {
          button.setEnabled(element instanceof BundlePool);
        }
      }
    };

    if (pool != null)
    {
      dialog.setSelectedElement(pool);
    }

    if (dialog.open() == AgentManagerDialog.OK)
    {
      enablePool(enabled[0]);
      pool = (BundlePool)dialog.getSelectedElement();
    }
  }

  private void install()
  {
    installButton.setImage(SetupInstallerPlugin.INSTANCE.getSWTImage("simple/download_small.png"));

    final Image[] productImage = { null };
    final Thread imageLoader = new Thread()
    {
      @Override
      public void run()
      {
        InputStream stream = null;

        try
        {
          String imageURI = ProductPage.getProductImageURI(product);
          stream = new URL(imageURI).openStream();

          productImage[0] = new Image(getDisplay(), stream);
        }
        catch (Exception ex)
        {
          //$FALL-THROUGH$
        }
        finally
        {
          IOUtil.closeSilent(stream);
        }
      }
    };

    imageLoader.setDaemon(true);
    imageLoader.start();

    installThread = new Thread()
    {
      @Override
      public void run()
      {
        final boolean[] canceled = { false };

        try
        {
          final int tasks = 100;
          for (int i = 0; i < tasks; i++)
          {
            final int task = i + 1;

            UIUtil.syncExec(new Runnable()
            {
              public void run()
              {
                try
                {
                  int selection = (progressBar.getMaximum() - progressBar.getMinimum() + 1) * task / tasks;
                  progressBar.setSelection(selection);
                  progressLabel.setText("Executing setup task " + task);
                }
                catch (SWTException ex)
                {
                  //$FALL-THROUGH$
                }
              }
            });

            try
            {
              sleep(50);
            }
            catch (InterruptedException ex)
            {
              canceled[0] = true;
              return;
            }
          }
        }
        finally
        {
          UIUtil.syncExec(new Runnable()
          {
            public void run()
            {
              try
              {
                cancelButton.setVisible(false);
                progressLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

                if (canceled[0])
                {
                  installButton.setToolTipText("Install");
                  progressLabel.setText("Installation canceled");
                  setEnabled(true);
                }
                else
                {
                  Image image = null;
                  try
                  {
                    imageLoader.join(5000);
                    if (productImage[0] != null)
                    {
                      image = productImage[0];
                    }
                  }
                  catch (Exception ex)
                  {
                    //$FALL-THROUGH$
                  }

                  installFinished(image);
                }
              }
              catch (SWTException ex)
              {
                //$FALL-THROUGH$
              }
            }
          });
        }
      }
    };

    installThread.setDaemon(true);
    installThread.start();
  }

  private void installCancel()
  {
    if (installThread != null)
    {
      installThread.interrupt();
    }

    installStack.setTopControl(installButton);
  }

  private void installFinished(Image productImage)
  {
    installed = true;
    installButton.setImage(productImage);
    installButton.setToolTipText("Launch");

    String message = "Installation finished successfully\n\n<a>" + TEXT_LOG + "</a>\n";
    readmePath = null;

    Scope scope = selectedProductVersion;
    while (scope != null)
    {
      Annotation annotation = scope.getAnnotation(AnnotationConstants.ANNOTATION_BRANDING_INFO);
      if (annotation != null)
      {
        readmePath = annotation.getDetails().get(AnnotationConstants.KEY_README_PATH);
        if (readmePath != null)
        {
          message += "<a>" + TEXT_README + "</a>\n";
          break;
        }
      }

      scope = scope.getParentScope();
    }

    message += "<a>" + TEXT_LAUNCH + "</a>";

    progressLabel.setText(message);
    backButton.setEnabled(true);

    installStack.setTopControl(installButton);
  }

  private void launch()
  {
    System.out.println("Launching...");
    dialog.exitSelected();
  }
}
