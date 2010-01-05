/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.impl;

import java.util.*;
import org.eclipse.equinox.internal.ds.*;
import org.eclipse.equinox.internal.ds.model.ServiceComponentProp;
import org.eclipse.equinox.internal.util.ref.Log;
import org.osgi.service.component.*;
import org.osgi.service.log.LogService;

/**
 * ComponentFactoryImpl.java
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 */

public class ComponentFactoryImpl implements ComponentFactory {

	static boolean security = false;

	private ServiceComponentProp sci;

	/**
	 * ComponentFactoryImpl
	 * 
	 * @param component
	 *            the ComponentDescription Object with Properties
	 */
	public ComponentFactoryImpl(ServiceComponentProp component) {
		security = Log.security();
		this.sci = component;
	}

	/**
	 * Create a new instance of the component. Additional properties may be
	 * provided for the component instance.
	 * 
	 * @param properties
	 *            Additional properties for the component instance.
	 * @return A ComponentInstance object encapsulating the component instance.
	 *         The returned component instance has been activated.
	 */
	public ComponentInstance newInstance(Dictionary additionalProps) {
		ComponentInstanceImpl instance = null;
		try {
			if (Activator.DEBUG) {
				Activator.log.debug("ComponentFactoryImpl.newInstance(): " + sci.name, null); //$NON-NLS-1$
			}

			// merge properties
			Hashtable props = (Hashtable) sci.getProperties().clone();
			SCRUtil.copyTo(props, additionalProps);

			// create a new SCP (adds to resolver scpEnabled list)
			ServiceComponentProp newSCP = InstanceProcess.resolver.mapNewFactoryComponent(sci.serviceComponent, props);

			// register the component and make instance if immediate
			Vector toBuild = new Vector(1);
			toBuild.addElement(newSCP);
			InstanceProcess.staticRef.buildComponents(toBuild, security);
			if (!newSCP.instances.isEmpty()) {
				// an instance was built because the component is either
				// immediate
				// or someone has got it as service (if provides one)
				instance = (ComponentInstanceImpl) newSCP.instances.firstElement();
			}
			if (instance == null) {
				// finally build an instance if not done yet
				instance = InstanceProcess.staticRef.buildComponent(null, newSCP, null, security);
			}
			instance.factory = this;
		} catch (Throwable e) {
			if (e instanceof ComponentException) {
				throw (ComponentException) e;
			}
			Activator.log(null, LogService.LOG_ERROR, "ComponentFactoryImpl.newInstance(): failed for " + sci.name + " with properties " + additionalProps, e); //$NON-NLS-1$ //$NON-NLS-2$
			throw new ComponentException(Messages.COULD_NOT_CREATE_NEW_INSTANCE, e);
		}
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "ComponentFactory for " + sci.name; //$NON-NLS-1$
	}

}
