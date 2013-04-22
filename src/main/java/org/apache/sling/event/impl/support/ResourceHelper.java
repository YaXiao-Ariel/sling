/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.event.impl.support;

import java.io.InputStream;
import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.jobs.deprecated.JobStatusNotifier;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.event.EventConstants;

public abstract class ResourceHelper {

    public static final String RESOURCE_TYPE_FOLDER = "sling:Folder";

    public static final String RESOURCE_TYPE_JOB = "slingevent:Job";

    public static final String RESOURCE_TYPE_EVENT = "slingevent:Event";

    public static final String BUNDLE_EVENT_UPDATED = "org/osgi/framework/BundleEvent/UPDATED";

    public static final String BUNDLE_EVENT_STARTED = "org/osgi/framework/BundleEvent/STARTED";

    /** List of ignored properties to write to the repository. */
    @SuppressWarnings("deprecation")
    private static final String[] IGNORE_PROPERTIES = new String[] {
        EventUtil.PROPERTY_DISTRIBUTE,
        EventUtil.PROPERTY_APPLICATION,
        EventConstants.EVENT_TOPIC,
        JobUtil.JOB_ID,
        JobUtil.PROPERTY_JOB_PARALLEL,
        JobUtil.PROPERTY_JOB_RUN_LOCAL,
        JobUtil.PROPERTY_JOB_QUEUE_ORDERED,
        JobUtil.PROPERTY_NOTIFICATION_JOB,
        JobStatusNotifier.CONTEXT_PROPERTY_NAME
    };

    /**
     * Check if this property should be ignored
     */
    public static boolean ignoreProperty(final String name) {
        for(final String prop : IGNORE_PROPERTIES) {
            if ( prop.equals(name) ) {
                return true;
            }
        }
        return false;
    }

    /** Allowed characters for a node name */
    private static final BitSet ALLOWED_CHARS;

    /** Replacement characters for unallowed characters in a node name */
    private static final char REPLACEMENT_CHAR = '_';

    // Prepare the ALLOWED_CHARS bitset with bits indicating the unicode
    // character index of allowed characters. We deliberately only support
    // a subset of the actually allowed set of characters for nodes ...
    static {
        final String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz0123456789_,.-+#!?$%&()=";
        final BitSet allowedSet = new BitSet();
        for (int i = 0; i < allowed.length(); i++) {
            allowedSet.set(allowed.charAt(i));
        }
        ALLOWED_CHARS = allowedSet;
    }

    /**
     * Filter the node name for not allowed characters and replace them.
     * @param nodeName The suggested node name.
     * @return The filtered node name.
     */
    public static String filterName(final String resourceName) {
        final StringBuilder sb  = new StringBuilder(resourceName.length());
        char lastAdded = 0;

        for(int i=0; i < resourceName.length(); i++) {
            final char c = resourceName.charAt(i);
            char toAdd = c;

            if (!ALLOWED_CHARS.get(c)) {
                if (lastAdded == REPLACEMENT_CHAR) {
                    // do not add several _ in a row
                    continue;
                }
                toAdd = REPLACEMENT_CHAR;

            } else if(i == 0 && Character.isDigit(c)) {
                sb.append(REPLACEMENT_CHAR);
            }

            sb.append(toAdd);
            lastAdded = toAdd;
        }

        if (sb.length()==0) {
            sb.append(REPLACEMENT_CHAR);
        }

        return sb.toString();
    }

    public static final String PROPERTY_MARKER_READ_ERROR = ResourceHelper.class.getName() + "/ReadError";

    public static Map<String, Object> cloneValueMap(final ValueMap vm) {
        boolean hasReadError = false;
        final Map<String, Object> result = new HashMap<String, Object>(vm);
        for(final Map.Entry<String, Object> entry : result.entrySet()) {
            if ( entry.getValue() instanceof InputStream ) {
                final Object value = vm.get(entry.getKey(), Serializable.class);
                if ( value != null ) {
                    entry.setValue(value);
                } else {
                    hasReadError = true;
                }
            }
        }
        if ( hasReadError ) {
            result.put(PROPERTY_MARKER_READ_ERROR, Boolean.TRUE);
        }
        return result;
    }

    public static void getOrCreateBasePath(final ResourceResolver resolver,
            final String path)
    throws PersistenceException {
        // TODO - we should rather fix ResourceUtil.getOrCreateResource:
        //        on concurrent writes, create might fail!
        for(int i=0;i<5;i++) {
            try {
                ResourceUtil.getOrCreateResource(resolver,
                        path,
                        ResourceHelper.RESOURCE_TYPE_FOLDER,
                        ResourceHelper.RESOURCE_TYPE_FOLDER,
                        true);
                return;
            } catch ( final PersistenceException pe ) {
                // ignore
            }
        }
        throw new PersistenceException("Unable to create resource with path " + path);
    }

    public static Resource getOrCreateResource(final ResourceResolver resolver,
            final String path, final Map<String, Object> props)
    throws PersistenceException {
        // TODO - we should rather fix ResourceUtil.getOrCreateResource:
        //        on concurrent writes, create might fail!
        for(int i=0;i<5;i++) {
            try {
                return ResourceUtil.getOrCreateResource(resolver,
                        path,
                        props,
                        ResourceHelper.RESOURCE_TYPE_FOLDER,
                        true);
            } catch ( final PersistenceException pe ) {
                // ignore
            }
        }
        throw new PersistenceException("Unable to create resource with path " + path);
    }
}