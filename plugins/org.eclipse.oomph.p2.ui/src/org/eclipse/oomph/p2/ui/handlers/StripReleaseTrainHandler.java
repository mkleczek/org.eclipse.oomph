package org.eclipse.oomph.p2.ui.handlers;

import org.eclipse.oomph.p2.core.Agent;
import org.eclipse.oomph.p2.core.P2Util;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class StripReleaseTrainHandler extends AbstractHandler
{
  /**
   * The constructor.
   */
  public StripReleaseTrainHandler()
  {
  }

  /**
   * the command has been executed, so extract extract the needed information
   * from the application context.
   */
  public Object execute(ExecutionEvent event) throws ExecutionException
  {
    stripTrain();
    // stripOrbit();

    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
    MessageDialog.openInformation(window.getShell(), "Oomph P2 Management UI", "Done!");
    return null;
  }

  private void stripTrain()
  {
    String[] trainPrefixes = { "com.fasterxml.jackson", "com.mycorp.examples.timeservice", "com.naef.jnlua", "com.spotify.docker.client",
        "com.thoughtworks.selenium", "jnr", "org.eclipse.actf", "org.eclipse.birt", "org.eclipse.bpel", "org.eclipse.bpmn2", "org.eclipse.cdt",
        "org.eclipse.egf", "org.eclipse.emf.cdo", "org.eclipse.emf.diffmerge", "org.eclipse.emf.ecp", "org.eclipse.emf.edapt", "org.eclipse.emf.emfstore",
        "org.eclipse.emf.facet", "org.eclipse.emf.query", "org.eclipse.emf.rap", "org.eclipse.emfforms", "org.eclipse.fx", "org.eclipse.gmt.modisco",
        "org.eclipse.graphiti", "org.eclipse.gyrex", "org.eclipse.jpt", "org.eclipse.jsf", "org.eclipse.jst", "org.eclipse.jubula", "org.eclipse.jwt",
        "org.eclipse.launchbar", "org.eclipse.ldt", "org.eclipse.libra", "org.eclipse.linuxtools", "org.eclipse.m2m", "org.eclipse.mat", "org.eclipse.modisco",
        "org.eclipse.net4j", "org.eclipse.objectteams", "org.eclipse.papyrus", "org.eclipse.persistence", "org.eclipse.photran", "org.eclipse.php",
        "org.eclipse.ptp", "org.eclipse.rap", "org.eclipse.rcptt", "org.eclipse.remote", "org.eclipse.riena", "org.eclipse.rse", "org.eclipse.scout",
        "org.eclipse.sphinx", "org.eclipse.stardust", "org.eclipse.tcf", "org.eclipse.thym", "org.eclipse.tm", "org.eclipse.tracecompass", "org.eclipse.wb",
        "org.eclipse.wtp", "org.eclipse.xwt", "org.eclipse.datatools", "org.eclipse.gef4", "org.eclipse.sapphire", "org.eclipse.uml2", "org.eclipse.xpand" };

    try
    {
      URI uri = new File("C:/develop/develop/p2/releases/mars/201510021000").toURI();
      Agent agent = P2Util.getAgentManager().getCurrentAgent();

      List<IInstallableUnit> iusToRemove = new ArrayList<IInstallableUnit>();

      IMetadataRepositoryManager metadataRepositoryManager = agent.getMetadataRepositoryManager();
      IMetadataRepository metadataRepository = metadataRepositoryManager.loadRepository(uri, null);
      for (IInstallableUnit iu : metadataRepository.query(QueryUtil.createIUAnyQuery(), null))
      {
        String id = iu.getId();
        if (matches(id, trainPrefixes))
        {
          iusToRemove.add(iu);
        }
      }

      metadataRepository.removeInstallableUnits(iusToRemove);
      metadataRepositoryManager.removeRepository(uri);

      List<IArtifactKey> keysToRemove = new ArrayList<IArtifactKey>();
      IArtifactRepositoryManager artifactRepositoryManager = agent.getArtifactRepositoryManager();
      IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository(uri, null);
      for (IArtifactKey key : artifactRepository.query(ArtifactKeyQuery.ALL_KEYS, null))
      {
        String id = key.getId();
        if (matches(id, trainPrefixes))
        {
          keysToRemove.add(key);
        }
      }

      artifactRepository.removeDescriptors(keysToRemove.toArray(new IArtifactKey[keysToRemove.size()]), null);
      artifactRepositoryManager.removeRepository(uri);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  private void stripOrbit()
  {
    String[] prefixes = { "javax.xml" };

    try
    {
      URI uri = new File("C:/develop/develop/p2/tools/orbit/downloads/drops/R20150519210750/repository").toURI();
      Agent agent = P2Util.getAgentManager().getCurrentAgent();

      List<IInstallableUnit> iusToRemove = new ArrayList<IInstallableUnit>();

      IMetadataRepositoryManager metadataRepositoryManager = agent.getMetadataRepositoryManager();
      IMetadataRepository metadataRepository = metadataRepositoryManager.loadRepository(uri, null);
      for (IInstallableUnit iu : metadataRepository.query(QueryUtil.createIUAnyQuery(), null))
      {
        String id = iu.getId();
        if (!matches(id, prefixes))
        {
          iusToRemove.add(iu);
        }
      }

      metadataRepository.removeInstallableUnits(iusToRemove);
      metadataRepositoryManager.removeRepository(uri);

      List<IArtifactKey> keysToRemove = new ArrayList<IArtifactKey>();
      IArtifactRepositoryManager artifactRepositoryManager = agent.getArtifactRepositoryManager();
      IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository(uri, null);
      for (IArtifactKey key : artifactRepository.query(ArtifactKeyQuery.ALL_KEYS, null))
      {
        String id = key.getId();
        if (!matches(id, prefixes))
        {
          keysToRemove.add(key);
        }
      }

      artifactRepository.removeDescriptors(keysToRemove.toArray(new IArtifactKey[keysToRemove.size()]), null);
      artifactRepositoryManager.removeRepository(uri);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  private static boolean matches(String id, String[] prefixes)
  {
    for (int i = 0; i < prefixes.length; i++)
    {
      String prefix = prefixes[i] + ".";
      if (id.startsWith(prefix))
      {
        return true;
      }
    }

    return false;
  }
}
