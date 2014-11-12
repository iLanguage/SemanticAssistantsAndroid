/*
Semantic Assistants -- http://www.semanticsoftware.info/semantic-assistants

This file is part of the Semantic Assistants architecture.

Copyright (C) 2014 Semantic Software Lab, http://www.semanticsoftware.info
Rene Witte
Bahar Sateli

The Semantic Assistants architecture is free software: you can
redistribute and/or modify it under the terms of the GNU Affero General
Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package info.semanticsoftware.semassist.server.rest;

import info.semanticsoftware.semassist.server.rest.resource.FileResource;
import info.semanticsoftware.semassist.server.rest.resource.ServiceResource;
import info.semanticsoftware.semassist.server.rest.resource.ServicesResource;
import info.semanticsoftware.semassist.server.rest.resource.UserResource;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.Context;

/**
 * Restlet main class.
 * @author Bahar Sateli
 * */
public class SemAssist extends Application {

	/** Restlet constructor.
	 * @param parentContext application context
	 */
	public SemAssist(final Context parentContext) {
		super(parentContext);
	}
	
	public SemAssist(){
		super();
	}

	/** Defines the routers for incoming request URLs. This is 
	 * how the restlet matches URLs to hnalder classes.
	 * @return the defined router object */
	@Override
	public synchronized Restlet createInboundRoot() {
		Router router = new Router(getContext());
		// Define routers for NLP services
		router.attach("/services", ServicesResource.class);
		router.attach("/service", ServiceResource.class);
		// Define routers for user actions
		router.attach("/user", UserResource.class);
		router.attach("/users/{userName}", UserResource.class);
		// Define routers for file
		router.attach("/file/{fileName}", FileResource.class);
		return router;
	}
}
