package com.netappsid.m2e.pde.target;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.netappsid.m2e.pde.target.internals.MavenBundleContainer;
import com.netappsid.m2e.pde.target.internals.MavenPDETarget;

public class GenerateS3ReleasePlatformAction extends SaveMavenTargetAction
{
	@Override
	protected void run(MavenPDETarget mavenPDETarget, MavenBundleContainer mavenBundleContainer)
	{
		super.run(mavenPDETarget, mavenBundleContainer);

		List<IProject> elements = Collections.checkedList(((IStructuredSelection) getSelection()).toList(), IProject.class);
		IProject targetProject = elements.get(0);

		IFile targetFile = targetProject.getFile(MavenPDETarget.PDE_TARGET_TARGET);

		// Ensure plugins folder is reset
		IFolder pluginsFolder = targetProject.getFolder(MavenPDETarget.PDE_TARGET_PLUGINS);
		try
		{
			pluginsFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		}
		catch (CoreException e)
		{
			new RuntimeException("Unable to refrech folder " + pluginsFolder.getFullPath().toOSString(), e);
		}

		// Ensure otherPlugins folder exists
		IFolder otherPluginsFolder = targetProject.getFolder(MavenPDETarget.PDE_TARGET_OTHER_PLUGINS);
		try
		{
			otherPluginsFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		}
		catch (CoreException e)
		{
			new RuntimeException("Unable to refrech folder " + pluginsFolder.getFullPath().toOSString(), e);
		}

		File pluginsFolderFile = pluginsFolder.getLocation().toFile();

		FileFilter jarFilter = new FileFilter()
			{
				@Override
				public boolean accept(File file)
				{
					return file.isFile() && file.getName().endsWith(".jar");
				}
			};

		List<OSGIBundleInfo> osgiBundleInfos = new ArrayList<OSGIBundleInfo>();

		// Files
		for (File pluginFile : pluginsFolderFile.listFiles())
		{
			JarFile jarFile;
			try
			{
				jarFile = new JarFile(pluginFile);
				JarEntry manifestFile = jarFile.getJarEntry(JarFile.MANIFEST_NAME);

				Manifest manifest = new Manifest(jarFile.getInputStream(manifestFile));

				String symbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");

				if (symbolicName == null)
				{
					// Not an OSGI plugin
					continue;
				}

				String version = manifest.getMainAttributes().getValue("Bundle-Version");

				if (symbolicName == null)
				{
					throw new RuntimeException("Version is null for plugin " + pluginFile.getPath());
				}

				osgiBundleInfos.add(new OSGIBundleInfo(symbolicName, version));
			}
			catch (IOException e)
			{
				new RuntimeException("Unable to create JarFile from file " + pluginFile.getPath(), e);
			}
		}

		// Folders
		// TODO

		// Order OSGIBundleInfor by name
		Collections.sort(osgiBundleInfos, new Comparator<OSGIBundleInfo>()
			{
				@Override
				public int compare(OSGIBundleInfo first, OSGIBundleInfo second)
				{
					if (first != null && second != null)
					{
						return first.getSymbolicName().compareTo(second.getSymbolicName());
					}
					else
					{
						if (first != null)
						{
							return 1;
						}
						else if (second != null)
						{
							return -1;
						}
						else
						{
							return 0;
						}
					}
				}
			});

		Document document = DocumentHelper.createDocument();

		// <osgi.release name="mavenclonerepository"/>
		Element root = document.addElement("osgi.release");
		root.addAttribute("name", "mavenclonerepository");

		for (OSGIBundleInfo osgiBundleInfo : osgiBundleInfos)
		{
			// <plugin symbolicname="info.clearthought.layout" version="1.0.0" />
			Element pluginElement = root.addElement("plugin");

			String symbolicName = osgiBundleInfo.getSymbolicName();

			int indexOfSemiColon = symbolicName.indexOf(';');
			if (indexOfSemiColon != -1)
			{
				symbolicName = symbolicName.substring(0, indexOfSemiColon);
			}

			pluginElement.addAttribute("symbolicname", symbolicName);
			pluginElement.addAttribute("version", osgiBundleInfo.getVersion());
		}

		XMLWriter writer;
		try
		{
			OutputFormat format = OutputFormat.createPrettyPrint();
			writer = new XMLWriter(new FileWriter(pluginsFolderFile.getParentFile().getAbsolutePath() + Path.SEPARATOR + "PDE_TARGET_S3Platform.xml"), format);
			writer.write(document);
			writer.close();

			// writer = new XMLWriter(System.out, format);
			// writer.write(document);
		}
		catch (IOException e)
		{
			new RuntimeException("Unable to write S3 platform to file ");
		}
	}

	private class OSGIBundleInfo
	{
		private final String symbolicName;
		private final String version;

		public OSGIBundleInfo(String symbolicName, String version)
		{
			this.symbolicName = symbolicName;
			this.version = version;
		}

		public String getSymbolicName()
		{
			return symbolicName;
		}

		public String getVersion()
		{
			return version;
		}
	}
}