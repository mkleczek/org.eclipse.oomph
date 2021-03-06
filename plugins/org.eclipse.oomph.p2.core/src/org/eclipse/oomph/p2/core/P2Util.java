/*
 * Copyright (c) 2014-2018 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.oomph.p2.core;

import org.eclipse.oomph.p2.internal.core.AgentImpl;
import org.eclipse.oomph.p2.internal.core.AgentManagerImpl;
import org.eclipse.oomph.p2.internal.core.CachingRepositoryManager;
import org.eclipse.oomph.p2.internal.core.CachingTransport;
import org.eclipse.oomph.util.StringUtil;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IRequirementChange;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IFilterExpression;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eike Stepper
 */
public final class P2Util
{
  private static final Pattern OSGI_PROPERTY_FILTER = Pattern.compile("(!?)\\((osgi.arch|osgi.os|osgi.ws)=([^)]+)\\)"); //$NON-NLS-1$

  private P2Util()
  {
  }

  public static synchronized AgentManager getAgentManager()
  {
    if (AgentManagerImpl.instance == null)
    {
      AgentManagerImpl.instance = new AgentManagerImpl();
    }

    return AgentManagerImpl.instance;
  }

  public static File getAgentLocation(IProvisioningAgent agent)
  {
    IAgentLocation location = (IAgentLocation)agent.getService(IAgentLocation.SERVICE_NAME);
    return URIUtil.toFile(location.getRootLocation());
  }

  public static IProvisioningAgent getCurrentProvisioningAgent()
  {
    return getAgentManager().getCurrentAgent().getProvisioningAgent();
  }

  public static Agent createAgent(File agentLocation)
  {
    return new AgentImpl((AgentManagerImpl)P2Util.getAgentManager(), agentLocation);
  }

  public static Set<String> getKnownRepositories(IRepositoryManager<?> manager)
  {
    Set<String> result = new HashSet<String>();
    for (URI uri : manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM))
    {
      result.add(uri.toString());
    }

    return result;
  }

  public static File getCacheFile(URI uri)
  {
    Agent agent = getAgentManager().getCurrentAgent();

    IMetadataRepositoryManager manager = agent.getMetadataRepositoryManager();
    if (manager instanceof CachingRepositoryManager)
    {
      CachingTransport transport = ((CachingRepositoryManager<?>)manager).getTransport();
      if (transport != null)
      {
        return transport.getCacheFile(uri);
      }
    }

    return null;
  }

  @SuppressWarnings("all")
  public static <T> Iterable<T> asIterable(final IQueryResult<T> queryResult)
  {
    if (queryResult instanceof Iterable<?>)
    {
      return Iterable.class.cast(queryResult);
    }

    return new Iterable<T>()
    {
      public Iterator<T> iterator()
      {
        return queryResult.iterator();
      }
    };
  }

  @SuppressWarnings("restriction")
  public static boolean isSimpleRequiredCapability(IRequirement requirement)
  {
    return requirement instanceof org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
  }

  public static Runnable preserveBundlePoolTimestamps(File bundlePoolLocation)
  {
    final File featuresFolder = new File(bundlePoolLocation, "features"); //$NON-NLS-1$
    final long featuresFolderLastModified = featuresFolder.lastModified();
    final File pluginsFolder = new File(bundlePoolLocation, "plugins"); //$NON-NLS-1$
    final long pluginsFolderLastModified = pluginsFolder.lastModified();

    return new Runnable()
    {
      public void run()
      {
        if (featuresFolderLastModified != 0L)
        {
          featuresFolder.setLastModified(featuresFolderLastModified);
        }

        if (pluginsFolderLastModified != 0L)
        {
          pluginsFolder.setLastModified(pluginsFolderLastModified);
        }
      }
    };
  }

  public static String getName(IInstallableUnit iu)
  {
    String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
    if (StringUtil.isEmpty(name))
    {
      return iu.getId();
    }

    return name;
  }

  public static Map<String, String> toProfilePropertiesMap(String profileProperties)
  {
    Map<String, String> result = new LinkedHashMap<String, String>();
    if (!StringUtil.isEmpty(profileProperties))
    {
      String[] properties = profileProperties.split(","); //$NON-NLS-1$
      for (String property : properties)
      {
        int index = property.indexOf('=');
        if (index == -1)
        {
          result.put(property, null);
        }
        else
        {
          result.put(property.substring(0, index), property.substring(index + 1));
        }
      }
    }

    return result;
  }

  public static String toProfilePropertiesString(Map<String, String> profileProperties)
  {
    StringBuilder result = new StringBuilder();
    for (Map.Entry<String, String> entry : profileProperties.entrySet())
    {
      if (result.length() != 0)
      {
        result.append(","); //$NON-NLS-1$
      }

      String key = entry.getKey();
      result.append(key);
      String value = entry.getValue();
      if (value != null)
      {
        result.append(' ').append(value);
      }
    }

    return result.toString();
  }

  public static IInstallableUnit createGeneralizedIU(IInstallableUnit iu, boolean isIncludeAllPlatforms, boolean isIncludeAllRequirements,
      boolean isIncludeNegativeRequirements)
  {
    // If we're not including all platform, no generalization is needed.
    if (!isIncludeAllPlatforms && isIncludeAllRequirements && isIncludeNegativeRequirements)
    {
      return iu;
    }

    // Determine the generalized IU filter.
    IMatchExpression<IInstallableUnit> filter = iu.getFilter();
    IMatchExpression<IInstallableUnit> generalizedFilter = isIncludeAllPlatforms ? generalize(filter) : filter;
    boolean needsGeneralization = filter != generalizedFilter;

    // Determine the generalized requirement filters.
    Collection<IRequirement> requirements = iu.getRequirements();
    IRequirement[] generalizedRequirements = requirements.toArray(new IRequirement[requirements.size()]);
    for (int i = 0; i < generalizedRequirements.length; ++i)
    {
      IRequirement requirement = generalizedRequirements[i];
      IMatchExpression<IInstallableUnit> requirementFilter = requirement.getFilter();
      IMatchExpression<IInstallableUnit> generalizedRequirementFilter = isIncludeAllPlatforms ? generalize(requirementFilter) : requirementFilter;

      // If the filter needs generalization, create a clone of the requirement, with the generalized filter replacement.
      int max = requirement.getMax();
      int min = requirement.getMin();
      if (requirementFilter != generalizedRequirementFilter || !isIncludeAllRequirements && min != 0 || !isIncludeNegativeRequirements && max == 0)
      {
        needsGeneralization = true;
        // IRequirement generalizedRequirement = MetadataFactory.createRequirement(requirement.getMatches(), generalizedRequirementFilter,
        // requirement.getMin(), requirement.getMax(), requirement.isGreedy(), requirement.getDescription());
        IRequirement generalizedRequirement = MetadataFactory.createRequirement(requirement.getMatches(), generalizedRequirementFilter,
            requirementFilter != generalizedRequirementFilter || !isIncludeAllRequirements ? 0 : min,
            !isIncludeNegativeRequirements && max == 0 ? Integer.MAX_VALUE : max, true, requirement.getDescription());
        generalizedRequirements[i] = generalizedRequirement;
      }
    }

    // If none of the filters or slicer-mode lower bounds need generalization, the original IU can be used.
    if (!needsGeneralization)
    {
      return iu;
    }

    // Create a description that clones the IU with the generalized filter and slicer-mode lower bound replacements.
    InstallableUnitDescription description;

    if (iu instanceof IInstallableUnitFragment)
    {
      IInstallableUnitFragment installableUnitFragment = (IInstallableUnitFragment)iu;
      MetadataFactory.InstallableUnitFragmentDescription fragmentDescription = new MetadataFactory.InstallableUnitFragmentDescription();
      Collection<IRequirement> host = installableUnitFragment.getHost();
      fragmentDescription.setHost(host.toArray(new IRequirement[host.size()]));
      description = fragmentDescription;
    }
    else if (iu instanceof IInstallableUnitPatch)
    {
      IInstallableUnitPatch installableUnitPatch = (IInstallableUnitPatch)iu;
      MetadataFactory.InstallableUnitPatchDescription patchDescription = new MetadataFactory.InstallableUnitPatchDescription();
      patchDescription.setApplicabilityScope(installableUnitPatch.getApplicabilityScope());
      patchDescription.setLifeCycle(installableUnitPatch.getLifeCycle());
      List<IRequirementChange> requirementsChange = installableUnitPatch.getRequirementsChange();
      patchDescription.setRequirementChanges(requirementsChange.toArray(new IRequirementChange[requirementsChange.size()]));
      description = patchDescription;
    }
    else
    {
      description = new MetadataFactory.InstallableUnitDescription();
    }

    description.setId(iu.getId());

    description.setVersion(iu.getVersion());

    Collection<IArtifactKey> artifacts = iu.getArtifacts();
    description.setArtifacts(artifacts.toArray(new IArtifactKey[artifacts.size()]));

    Collection<IProvidedCapability> providedCapabilities = iu.getProvidedCapabilities();
    description.setCapabilities(providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));

    description.setCopyright(iu.getCopyright());

    description.setFilter(generalizedFilter);

    Collection<ILicense> licenses = iu.getLicenses();
    description.setLicenses(licenses.toArray(new ILicense[licenses.size()]));

    Collection<IRequirement> metaRequirements = iu.getMetaRequirements();
    description.setMetaRequirements(metaRequirements.toArray(new IRequirement[metaRequirements.size()]));

    description.setRequirements(generalizedRequirements);

    description.setSingleton(iu.isSingleton());

    description.setTouchpointType(iu.getTouchpointType());
    description.setUpdateDescriptor(iu.getUpdateDescriptor());

    for (Iterator<Map.Entry<String, String>> iterator = iu.getProperties().entrySet().iterator(); iterator.hasNext();)
    {
      Map.Entry<String, String> entry = iterator.next();
      description.setProperty(entry.getKey(), entry.getValue());
    }

    for (ITouchpointData touchpointData : iu.getTouchpointData())
    {
      description.addTouchpointData(touchpointData);
    }

    return MetadataFactory.createInstallableUnit(description);
  }

  @SuppressWarnings("restriction")
  private static IMatchExpression<IInstallableUnit> generalize(IMatchExpression<IInstallableUnit> filter)
  {
    if (filter == null)
    {
      return null;
    }

    // Lazily determine if any parameter needs generalization.
    Object[] generalizedParameters = null;
    Object[] parameters = filter.getParameters();
    for (int i = 0; i < parameters.length; ++i)
    {
      Object parameter = parameters[i];
      if (parameter instanceof org.eclipse.equinox.internal.p2.metadata.expression.LDAPFilter)
      {
        String value = parameter.toString();
        Matcher matcher = OSGI_PROPERTY_FILTER.matcher(value);
        if (matcher.find())
        {
          // If the pattern matches, we need to generalize the parameters.
          if (generalizedParameters == null)
          {
            // Copy over all the parameters.
            // The ones that need generalization will be replaced.
            generalizedParameters = new Object[parameters.length];
            System.arraycopy(parameters, 0, generalizedParameters, 0, parameters.length);
          }

          // Build the replacement expression
          StringBuffer result = new StringBuffer();
          if (matcher.group(1).length() == 0)
          {
            matcher.appendReplacement(result, "($2=*)"); //$NON-NLS-1$
          }
          else
          {
            matcher.appendReplacement(result, "!($2=nothing)"); //$NON-NLS-1$
          }

          // Handle all the remaining matches the same way.
          while (matcher.find())
          {
            if (matcher.group(1).length() == 0)
            {
              matcher.appendReplacement(result, "($2=*)"); //$NON-NLS-1$
            }
            else
            {
              matcher.appendReplacement(result, "!($2=nothing)"); //$NON-NLS-1$
            }
          }

          // Complete the replacements, parse it back into an LDAP filter, and replace this parameter.
          matcher.appendTail(result);
          IFilterExpression ldap = ExpressionUtil.parseLDAP(result.toString());
          generalizedParameters[i] = ldap;
        }
      }
    }

    // If one of the parameters needed to be generalized...
    if (generalizedParameters != null)
    {
      // Parse the filter expression and create a new match expressions with the same filter but the generalized parameters.
      IExpression expression = ExpressionUtil.parse(filter.toString());
      return ExpressionUtil.getFactory().matchExpression(expression, generalizedParameters);
    }

    // Otherwise, return the original filter.
    return filter;
  }

  @SuppressWarnings("unused")
  private static InstallableUnitDescription createDescription(IInstallableUnit iu)
  {
    InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();

    description.setId(iu.getId());

    description.setVersion(iu.getVersion());

    Collection<IArtifactKey> artifacts = iu.getArtifacts();
    description.setArtifacts(artifacts.toArray(new IArtifactKey[artifacts.size()]));

    Collection<IProvidedCapability> providedCapabilities = iu.getProvidedCapabilities();
    description.setCapabilities(providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));

    description.setCopyright(iu.getCopyright());

    IMatchExpression<IInstallableUnit> filter = iu.getFilter();
    description.setFilter(filter);

    Collection<ILicense> licenses = iu.getLicenses();
    description.setLicenses(licenses.toArray(new ILicense[licenses.size()]));

    Collection<IRequirement> metaRequirements = iu.getMetaRequirements();
    description.setMetaRequirements(metaRequirements.toArray(new IRequirement[metaRequirements.size()]));

    Collection<IRequirement> requirements = iu.getRequirements();
    description.setRequirements(requirements.toArray(new IRequirement[requirements.size()]));

    description.setSingleton(iu.isSingleton());

    description.setTouchpointType(iu.getTouchpointType());
    description.setUpdateDescriptor(iu.getUpdateDescriptor());

    for (Iterator<Entry<String, String>> iterator = iu.getProperties().entrySet().iterator(); iterator.hasNext();)
    {
      Entry<String, String> entry = iterator.next();
      description.setProperty(entry.getKey(), entry.getValue());
    }

    for (ITouchpointData touchpointData : iu.getTouchpointData())
    {
      description.addTouchpointData(touchpointData);
    }

    return description;
  }

  /**
   * @author Eike Stepper
   */
  public interface VersionedIdFilter
  {
    public boolean matches(IVersionedId versionedId);
  }
}
