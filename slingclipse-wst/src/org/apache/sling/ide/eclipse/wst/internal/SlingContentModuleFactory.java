package org.apache.sling.ide.eclipse.wst.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ModuleFile;
import org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate;

public class SlingContentModuleFactory extends ProjectModuleFactoryDelegate {

    private static final String NATURE_ID = "sling.content";

    @Override
    public ModuleDelegate getModuleDelegate(IModule module) {

        System.out.println("SlingContentModuleFactory.getModuleDelegate()");

        return new SlingContentModuleDelegate(module);
    }

    @Override
    protected IModule createModule(IProject project) {

        try {
            IFacetedProject facetedProject = ProjectFacetsManager.create(project);
            for (IProjectFacetVersion facet : facetedProject.getProjectFacets()) {
                System.out.println("Project " + project + " has facet " + facet);
                if (facet.getProjectFacet().getId().equals(NATURE_ID)) {
                    return createModule(project.getName(), project.getName(), NATURE_ID, "1.0", project);
                }
            }
        } catch (CoreException ce) {
            // TODO logging
            }


        return null;
    }

    static class SlingContentModuleDelegate extends ModuleDelegate {

        private final IModule module;

        public SlingContentModuleDelegate(IModule module) {
            this.module = module;
        }

        @Override
        public IStatus validate() {
            return Status.OK_STATUS; // TODO actually validate
        }

        @Override
        public IModuleResource[] members() throws CoreException {
            IProject project = module.getProject();
            final List<IModuleResource> resources = new ArrayList<IModuleResource>();
            // TODO just a hack to get some files
            project.accept(new IResourceVisitor() {
                @Override
                public boolean visit(IResource resource) throws CoreException {
                    System.out.println(resource.getName() + " -> " + resource.getFileExtension());
                    if (resource.getType() == IResource.FILE && resource.getFileExtension().equals("txt")) {
                        resources.add(new ModuleFile((IFile) resource, resource.getName(), resource
                                .getProjectRelativePath()));
                    }
                    return true;
                }
            });

            System.out.println("SlingContentModuleFactory.SlingContentModuleDelegate.members() returned " + resources);

            return resources.toArray(new IModuleResource[resources.size()]);
        }

        @Override
        public IModule[] getChildModules() {
            return new IModule[0]; // TODO revisit, do we need child modules?
        }
    }
}
