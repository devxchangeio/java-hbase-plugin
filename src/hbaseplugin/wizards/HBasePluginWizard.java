package hbaseplugin.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "java". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public class HBasePluginWizard extends Wizard implements INewWizard {
	private HBasePluginWizardPage page;
	private ISelection selection;
	public static String containerName = null;
	public static String packageName = null;
	public static String fileName = null;

	/**
	 * Constructor for HBasePluginWizard.
	 */
	public HBasePluginWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new HBasePluginWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		containerName = page.getContainerName();
		packageName = page.getPackageName();
		fileName = page.getClassName();
		
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 * @throws Exception 
	 */

	@SuppressWarnings("unused")
	private void doFinish(
		String containerName,
		String fileName,
		IProgressMonitor monitor)
		throws Exception {
		
		//this.fileName = fileName;
		// create a sample file
		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}
		IContainer container = (IContainer) resource;
		IJavaProject project = (IJavaProject) container.getProject().getNature(
				"org.eclipse.jdt.core.javanature");
		IPackageFragment ipackage = project.getAllPackageFragmentRoots()[0]
				.createPackageFragment(packageName, false, monitor);
		String contents = getHBaseClass();
		final ICompilationUnit cu = ipackage.createCompilationUnit(fileName
				+ ".java", contents, false, monitor);
		monitor.worked(1);
		
		contents = getTestClass();
		final ICompilationUnit cu1 = ipackage.createCompilationUnit("HBaseTest.java", contents, false, monitor);
		monitor.worked(1);
		
		final IFile file1 = container.getFile(new Path("/src/pom.txt"));
		try {
			InputStream stream = openPOMContentStream();
			if (file1.exists()) {
				file1.setContents(stream, true, true, monitor);
			} else {
				file1.create(stream, true, monitor);
			}
			stream.close();
			monitor.worked(1);
			
		} catch (Exception e) {
			System.err.println(e.getMessage().toString());
		}
		monitor.worked(1);

	}
	
	/**
	 * We will initialize file contents with a sample text.
	 * @throws Exception 
	 */

	@SuppressWarnings("unused")
	private InputStream openContentStream() throws Exception {
		String contents = getHBaseClass();
		return new ByteArrayInputStream(contents.getBytes());
	}
	
	private InputStream openPOMContentStream() throws Exception {
		String contents = getPomFile();
		return new ByteArrayInputStream(contents.getBytes());
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "HBasePlugin", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
	
	private String getHBaseClass() throws Exception {

		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream(
				"/templates/hbaseconnector.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			line = line.replaceAll("#filename", fileName);
			line = line.replaceAll("#package", packageName);
			System.out.println(line);

			boolean skip = false;
			if (!skip) {
				sb.append(line);
				sb.append("\n");
			}

		}
		reader.close();
		reader.close();
		return sb.toString();

	}
	
	private String getPomFile() throws Exception {

		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream(
				"/templates/pom.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			line = line.replaceAll("#filename", fileName);
			System.out.println(line);
			boolean skip = false;
			if (!skip) {
				sb.append(line);
				sb.append("\n");
			}

		}
		reader.close();
		reader.close();
		return sb.toString();

	}
	
	private String getTestClass() throws Exception {

		StringBuffer sb = new StringBuffer();
		BufferedReader reader;
		InputStream input = this.getClass().getResourceAsStream(
				"/templates/hbasetester.template");
		reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			line = line.replaceAll("#fileName", fileName);
			line = line.replaceAll("#package", packageName);

			System.out.println(line);
			boolean skip = false;
			if (!skip) {
				sb.append(line);
				sb.append("\n");
			}

		}
		reader.close();
		reader.close();
		return sb.toString();

	}
}