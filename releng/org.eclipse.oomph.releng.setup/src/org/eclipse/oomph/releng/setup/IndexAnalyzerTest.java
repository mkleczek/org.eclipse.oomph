/*
 * Copyright (c) 2018 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.oomph.releng.setup;

import org.eclipse.oomph.setup.Configuration;
import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.ProductVersion;
import org.eclipse.oomph.setup.Project;
import org.eclipse.oomph.setup.SetupFactory;
import org.eclipse.oomph.setup.SetupTask;
import org.eclipse.oomph.setup.SetupTaskContainer;
import org.eclipse.oomph.setup.Stream;
import org.eclipse.oomph.setup.Workspace;
import org.eclipse.oomph.setup.internal.core.SetupContext;
import org.eclipse.oomph.setup.internal.core.util.ECFURIHandlerImpl;
import org.eclipse.oomph.setup.internal.core.util.ECFURIHandlerImpl.CacheHandling;
import org.eclipse.oomph.setup.internal.core.util.ResourceMirror;
import org.eclipse.oomph.setup.internal.core.util.SetupCoreUtil;
import org.eclipse.oomph.util.CollectionUtil;
import org.eclipse.oomph.util.IOUtil;
import org.eclipse.oomph.util.StringUtil;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 */
@SuppressWarnings("restriction")
public class IndexAnalyzerTest
{
  private static final File OUTPUT_DIR = new File(System.getProperty("output.dir", "target/configurations"));

  private static final File PMI_DIR = new File(OUTPUT_DIR, "pmi");

  private static final String PMI_URL = System.getProperty("pmi.url", "https://projects.eclipse.org/json/projects/all");

  private static final URI BASE_URI = URI.createURI(System.getProperty("base.uri", "http://download.eclipse.org/oomph/configurations/"));

  private static final URIConverter URI_CONVERTER = new ExtensibleURIConverterImpl()
  {
    {
      getURIMap().put(URI.createFileURI(OUTPUT_DIR.getAbsolutePath() + "/"), BASE_URI);
    }
  };

  private static Map<String, String> projectTitles = new HashMap<String, String>();

  private static Map<String, PMIRepo> reposByURL = new HashMap<String, PMIRepo>();

  private static Map<String, Set<PMIRepo>> reposByID = new HashMap<String, Set<PMIRepo>>();

  private static Map<Project, Set<PMIRepo>> reposByProject = new HashMap<Project, Set<PMIRepo>>();

  @Test
  public void analyze() throws Exception
  {
    loadPMI(PMI_URL);

    ResourceSet resourceSet = loadResources();
    handleResources(resourceSet, new ProjectAnalyzer());
    handleResources(resourceSet, new ConfigurationAnalyzer());

    synthesizeConfigurations(resourceSet);

    PMI_DIR.mkdirs();
    savePMI();
    saveHTML();
  }

  private void synthesizeConfigurations(ResourceSet resourceSet) throws IOException
  {
    ProductVersion committersLatestReleased = (ProductVersion)resourceSet.getEObject(URI.createURI(
        "index:/org.eclipse.setup#//@productCatalogs[name='org.eclipse.products']/@products[name='epp.package.committers']/@versions[name='latest.released']"),
        true);

    for (PMIRepo pmiRepo : reposByURL.values())
    {
      Set<Project> projects = pmiRepo.getProjects();
      Set<Configuration> configurations = pmiRepo.getConfigurations();

      if (!projects.isEmpty())
      {
        String projectID = pmiRepo.getProjectID();
        System.out.println(pmiRepo.getURL() + " --> " + projectID);

        List<Stream> streams = new ArrayList<Stream>();
        for (Project project : projects)
        {
          System.out.println("   Project " + project.getQualifiedName());

          for (Stream stream : project.getStreams())
          {
            streams.add(stream);
            break;
          }
        }

        if (configurations.isEmpty())
        {
          if (!streams.isEmpty())
          {
            String projectLabel = StringUtil.capAll(projectID.replace('.', ' '));

            Installation installation = SetupFactory.eINSTANCE.createInstallation();
            installation.setName(projectID + ".installation");
            installation.setLabel(projectLabel + " Installation");
            installation.setDescription("Bla bla");
            installation.setProductVersion(committersLatestReleased);

            Workspace workspace = SetupFactory.eINSTANCE.createWorkspace();
            workspace.setName(projectID + ".workspace");
            workspace.setLabel(projectLabel + " Workspace");
            workspace.setDescription("Bla bla");
            workspace.getStreams().addAll(streams);

            Configuration configuration = SetupFactory.eINSTANCE.createConfiguration();
            configuration.setLabel(projectLabel + " Configuration");
            configuration.setDescription("Bla bla");
            configuration.setInstallation(installation);
            configuration.setWorkspace(workspace);

            File file = new File(OUTPUT_DIR, projectID + "." + pmiRepo.getNumber() + ".setup");
            Resource resource = resourceSet.createResource(URI.createFileURI(file.getAbsolutePath()));
            resource.getContents().add(configuration);
            resource.save(null);

            configurations.add(configuration);
          }
        }

        for (Configuration configuration : configurations)
        {
          System.out.println("   Configuration " + configuration.getLabel());
        }

        System.out.println();
      }
    }
  }

  private void saveHTML() throws JSONException, IOException
  {
    StringBuilder builder = new StringBuilder();
    saveHTML(builder, "technology.egit", reposByID.get("technology.egit"));

    for (Map.Entry<String, Set<PMIRepo>> entry : reposByID.entrySet())
    {
      String projectID = entry.getKey();
      if (!"technology.egit".equals(projectID))
      {
        saveHTML(builder, projectID, entry.getValue());
      }
    }

    File file = new File(PMI_DIR, "configurations.html");
    Writer writer = new BufferedWriter(new FileWriter(file));
    writer.write(builder.toString());
    writer.close();

    for (File icon : new File("icons").listFiles())
    {
      IOUtil.copyFile(icon, new File(PMI_DIR, icon.getName()));
    }
  }

  private void saveHTML(StringBuilder builder, String projectID, Set<PMIRepo> pmiRepos) throws JSONException
  {
    int initialLength = builder.length();
    boolean projectHasConfigs = false;

    String projectTitle = projectTitles.get(projectID);
    if (projectTitle == null)
    {
      projectTitle = StringUtil.capAll(projectID.replace('.', ' '));
    }

    builder.append("<h2><a href=\"https://projects.eclipse.org/projects/" + projectID + "/developer\">" + projectTitle + "</a></h2>\n");

    for (PMIRepo pmiRepo : pmiRepos)
    {
      builder.append("<h4><img src=\"git.png\"/>&nbsp;" + pmiRepo.getURL() + "</h4>\n");
      builder.append("<p>\n");
      builder.append("&nbsp;&nbsp;&nbsp;<img src=\"gerrit.png\"/>&nbsp;<a href=\"\">Review with Gerrit</a>\n");
      builder.append("&nbsp;&nbsp;&nbsp;<img src=\"browse.png\"/>&nbsp;<a href=\"\">Browse Repository</a>\n");

      Set<Configuration> configurations = pmiRepo.getConfigurations();
      if (!configurations.isEmpty())
      {
        projectHasConfigs = true;

        for (Configuration configuration : configurations)
        {
          URI uri = EcoreUtil.getURI(configuration).trimFragment();
          uri = URI_CONVERTER.normalize(uri);

          builder.append("&nbsp;&nbsp;&nbsp;<img src=\"oomph.png\"/>&nbsp;<a href=\"" + uri
              + "\" title=\"Drag this link and drop it onto the Eclipse Installer's title area in order to install a complete development environment for this repository\">Install Workspace</a>\n");
        }
      }

      builder.append("</p>\n\n");
    }

    if (!projectHasConfigs)
    {
      builder.setLength(initialLength);
    }
  }

  private void savePMI() throws JSONException, IOException
  {
    JSONObject projects = new JSONObject();
    for (Map.Entry<String, Set<PMIRepo>> entry : reposByID.entrySet())
    {
      JSONObject project = new JSONObject();
      boolean projectHasConfigs = false;

      JSONArray sourceRepo = new JSONArray();
      project.put("source_repo", sourceRepo);

      for (PMIRepo pmiRepo : entry.getValue())
      {
        Set<Configuration> configurations = pmiRepo.getConfigurations();
        if (!configurations.isEmpty())
        {
          JSONObject repoElement = new JSONObject();
          repoElement.put("url", pmiRepo.getURL());

          JSONArray configs = new JSONArray();
          repoElement.put("configs", configs);

          for (Configuration configuration : configurations)
          {
            URI uri = EcoreUtil.getURI(configuration).trimFragment();
            uri = URI_CONVERTER.normalize(uri);

            JSONObject configElement = new JSONObject();
            configElement.put("url", uri);
            configElement.put("label", configuration.getLabel());
            configElement.put("description", configuration.getDescription());

            configs.put(configElement);
            projectHasConfigs = true;
          }

          sourceRepo.put(repoElement);
        }
      }

      if (projectHasConfigs)
      {
        projects.put(entry.getKey(), project);
      }
    }

    JSONObject root = new JSONObject();
    root.put("projects", projects);

    File file = new File(PMI_DIR, "configurations.json");
    Writer writer = new BufferedWriter(new FileWriter(file));
    root.write(writer);
    writer.close();
  }

  private void loadPMI(String pmiURL) throws Exception
  {
    InputStream stream = new URL(pmiURL).openStream();
    JSONObject object = new JSONObject(new JSONTokener(new InputStreamReader(stream)));

    JSONObject projects = object.getJSONObject("projects");
    String[] names = JSONObject.getNames(projects);

    for (String name : names)
    {
      JSONObject project = projects.getJSONObject(name);
      projectTitles.put(name, project.getString("title"));

      JSONArray repos = project.getJSONArray("source_repo");
      for (int i = 0; i < repos.length(); i++)
      {
        JSONObject repo = repos.getJSONObject(i);
        String url = repo.getString("url");

        PMIRepo pmiRepo = new PMIRepo(url, name, i);
        reposByURL.put(url, pmiRepo);
        CollectionUtil.add(reposByID, name, pmiRepo);
      }
    }

    stream.close();
  }

  private ResourceSet loadResources()
  {
    ResourceSet resourceSet = SetupCoreUtil.createResourceSet();
    resourceSet.getLoadOptions().put(ECFURIHandlerImpl.OPTION_CACHE_HANDLING, CacheHandling.CACHE_IGNORE);

    ResourceMirror resourceMirror = new ResourceMirror(resourceSet)
    {
    };

    resourceMirror.perform(SetupContext.INDEX_SETUP_URI);
    resourceMirror.dispose();

    EcoreUtil.resolveAll(resourceSet);
    return resourceSet;
  }

  private static void handleResources(ResourceSet resourceSet, ResourceHandler handler)
  {
    for (URI uri : resourceSet.getURIConverter().getURIMap().keySet())
    {
      String lastSegment = uri.lastSegment();
      if (lastSegment != null && lastSegment.endsWith(".setup"))
      {
        Resource resource = null;

        try
        {
          resource = resourceSet.getResource(uri, true);

          EObject root = resource.getContents().get(0);
          if (!handler.handleResource(root))
          {
            return;
          }
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
          System.err.println("### " + uri);

          if (resource != null)
          {
            for (Diagnostic diagnostic : resource.getErrors())
            {
              System.err.println("### " + diagnostic.getMessage());
            }
          }
        }
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  public interface ResourceHandler
  {
    public boolean handleResource(EObject root) throws Exception;
  }

  /**
   * @author Eike Stepper
   */
  private static final class ProjectAnalyzer implements ResourceHandler
  {
    public boolean handleResource(EObject root) throws Exception
    {
      if (root instanceof Project)
      {
        Project project = (Project)root;
        analyzeProject(project);
      }

      return true;
    }

    private void analyzeProject(Project project)
    {
      if (!"org.eclipse".equals(project.getProjectCatalog().getName()))
      {
        return;
      }

      analyzeProject(project, project.getSetupTasks());

      for (Stream stream : project.getStreams())
      {
        analyzeProject(project, stream.getSetupTasks());
      }

      for (Project subProject : project.getProjects())
      {
        analyzeProject(subProject);
      }
    }

    private void analyzeProject(Project project, EList<SetupTask> setupTasks)
    {
      for (SetupTask setupTask : setupTasks)
      {
        EClass eClass = setupTask.eClass();

        if ("GitCloneTask".equals(eClass.getName()))
        {
          String remoteURI = (String)setupTask.eGet(eClass.getEStructuralFeature("remoteURI"));

          PMIRepo pmiRepo = determinePMIRepo(remoteURI);
          if (pmiRepo != null)
          {
            registerProjects(pmiRepo, project);
          }
        }

        if (setupTask instanceof SetupTaskContainer)
        {
          SetupTaskContainer container = (SetupTaskContainer)setupTask;
          analyzeProject(project, container.getSetupTasks());
        }
      }
    }

    private PMIRepo determinePMIRepo(String remoteURI)
    {
      PMIRepo pmiRepo = reposByURL.get(remoteURI);
      if (pmiRepo != null)
      {
        return pmiRepo;
      }

      for (Map.Entry<String, PMIRepo> entry : reposByURL.entrySet())
      {
        String sourceRepo = entry.getKey();
        if (sourceRepo.endsWith(remoteURI))
        {
          return entry.getValue();
        }

        if (sourceRepo.endsWith(remoteURI + ".git"))
        {
          return entry.getValue();
        }

        if ((sourceRepo + ".git").endsWith(remoteURI))
        {
          return entry.getValue();
        }
      }

      return null;
    }

    private void registerProjects(PMIRepo pmiRepo, Project project)
    {
      pmiRepo.getProjects().add(project);
      CollectionUtil.add(reposByProject, project, pmiRepo);

      for (Project subProject : project.getProjects())
      {
        registerProjects(pmiRepo, subProject);
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  private static final class ConfigurationAnalyzer implements ResourceHandler
  {
    public boolean handleResource(EObject root) throws Exception
    {
      if (root instanceof Configuration)
      {
        Configuration configuration = (Configuration)root;
        analyzeConfiguration(configuration);
      }

      return true;
    }

    private void analyzeConfiguration(Configuration configuration)
    {
      Workspace workspace = configuration.getWorkspace();
      if (workspace != null)
      {
        for (Stream stream : workspace.getStreams())
        {
          Project project = stream.getProject();

          Set<PMIRepo> pmiRepos = reposByProject.get(project);
          if (pmiRepos != null)
          {
            for (PMIRepo pmiRepo : pmiRepos)
            {
              pmiRepo.getConfigurations().add(configuration);
            }
          }
        }
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  private static final class PMIRepo
  {
    private final String url;

    private final String projectID;

    private final int number;

    private final Set<Project> projects = new HashSet<Project>();

    private final Set<Configuration> configurations = new HashSet<Configuration>();

    public PMIRepo(String theURL, String theProject, int theNumber)
    {
      url = theURL;
      projectID = theProject;
      number = theNumber;
    }

    public String getURL()
    {
      return url;
    }

    public String getProjectID()
    {
      return projectID;
    }

    public int getNumber()
    {
      return number;
    }

    public Set<Project> getProjects()
    {
      return projects;
    }

    public Set<Configuration> getConfigurations()
    {
      return configurations;
    }
  }
}
