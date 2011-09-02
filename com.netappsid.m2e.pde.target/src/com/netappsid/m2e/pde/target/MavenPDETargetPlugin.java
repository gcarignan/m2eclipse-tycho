package com.netappsid.m2e.pde.target;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


public class MavenPDETargetPlugin extends AbstractUIPlugin {

	
	private static BundleContext context;

	public MavenPDETargetPlugin() {
		
	}

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		super.start(context);
	}
	
	public static <T> T getService(Class<T> service)
	{
		ServiceReference<T> serviceReference = context.getServiceReference(service);
		return context.getService(serviceReference);
	}
	
}
