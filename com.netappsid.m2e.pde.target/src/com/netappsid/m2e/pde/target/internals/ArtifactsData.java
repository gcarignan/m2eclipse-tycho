package com.netappsid.m2e.pde.target.internals;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;

public class ArtifactsData
{
	private final Map<String, Artifact> mostRecentArtifactIdToArtifacts = new HashMap<String, Artifact>();
	private final Map<String, Artifact> mostRecentArtifactIdToTestArtifacts = new HashMap<String, Artifact>();

	public Collection<Artifact> getArtifacts()
	{
		return mostRecentArtifactIdToArtifacts.values();
	}

	Map<String, Artifact> getMostRecentArtifactIdToArtifacts()
	{
		return mostRecentArtifactIdToArtifacts;
	}

	public Collection<Artifact> getTestArtifacts()
	{
		return mostRecentArtifactIdToTestArtifacts.values();
	}

	Map<String, Artifact> getMostRecentArtifactIdToTestArtifacts()
	{
		return mostRecentArtifactIdToTestArtifacts;
	}
}
