package hbaseplugin.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;


/**
 * 
 * @author karthy
 *
 */
public class HBasePluginWizardPage extends WizardPage {
	private Text containerText;
	private ISelection selection;
	private Text classText;
	public Text packageText;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public HBasePluginWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("HBase Wizard");
		setDescription("HBase Class Definition");		
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		Label label = new Label(container, SWT.NULL);
		label.setText("&Container:");

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		label = new Label(container, SWT.NULL);
		label.setText("&Package Name:");

		packageText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		packageText.setLayoutData(gd);
		packageText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		new Label(container, 0);
		label = new Label(container, SWT.NULL);
		label.setText("&Class Name:");

		classText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		classText.setLayoutData(gd);
		classText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		new Label(container, 0);
		initialize1();
		dialogChanged();
		setControl(container);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */

	@SuppressWarnings("unused")
	private void initialize() {
		if (selection != null && selection.isEmpty() == false
				&& selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				IContainer container;
				if (obj instanceof IContainer)
					container = (IContainer) obj;
				else
					container = ((IResource) obj).getParent();
				containerText.setText(container.getFullPath().toString());
			}
		}
		
		//classText.setText("HBaseConnector.java");
	}
	
	private void initialize1()
	  {
	    if ((this.selection != null) && (!this.selection.isEmpty()) && 
	      ((this.selection instanceof IStructuredSelection))) {
	      IStructuredSelection ssel = (IStructuredSelection)this.selection;
	      if (ssel.size() > 1)
	        return;
	      Object obj = ssel.getFirstElement();
	      try
	      {
	        if ((obj instanceof IAdaptable)) {
	          IAdaptable adaptable = (IAdaptable)obj;

	          IJavaElement jelem = (IJavaElement)adaptable.getAdapter(IJavaElement.class);
	          if (jelem != null) {
	            IJavaProject jproject = jelem.getJavaProject();
	            if ((jproject != null) && (jproject.exists())) {
	            	containerText.setText(jproject.getElementName().replaceAll("[ \t]+", ""));
	              IPackageFragmentRoot[] roots = jproject.getPackageFragmentRoots();
	              for (IPackageFragmentRoot root : roots) {
	                if (root.getKind() == 1) {
	                  this.classText.setText(root.getPath().toString().substring(1));
	                  break;
	                }
	              }
	            }
	          }
	          if ((obj instanceof IPackageFragment)) {
	            IPackageFragment pf = (IPackageFragment)obj;
	            this.packageText.setText(pf.getElementName());
	          }
	        }
	      } catch (JavaModelException jme) {
	        jme.printStackTrace();
	      }
	    }
	    
	    classText.setText("HBaseConnector");
		// packageText.setText("");
	  }

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */

	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				"Select new file container");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
			}
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(getContainerName()));
		String fileName = getClassName();
		String packageName = getPackageName();

		if (getContainerName().length() == 0) {
			updateStatus("Project must be specified");
			return;
		}
		if (container == null
				|| (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("Project must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}

		if (packageName.length() == 0) {
			updateStatus("Pacakge name must be specified");
			return;
		}
		if (packageName.endsWith(".")) {
		      updateStatus("Package name must be valid");
		      return;
		    }
		
		if (packageName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("Package name must be valid");
			return;
		}
		if (fileName.length() == 0) {
			updateStatus("Class name must be specified");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("Class name must be valid");
			return;
		}
		int dotLoc = fileName.lastIndexOf('.');
		if (dotLoc != -1) {
			String ext = fileName.substring(dotLoc + 1);
			if (ext.equalsIgnoreCase("java") == false) {
				updateStatus("Class extension must be \"java\"");
				return;
			}
		}
		
		 try
		    {
		      IJavaProject project = (IJavaProject)container.getProject().getNature("org.eclipse.jdt.core.javanature");
		      IPackageFragmentRoot[] packages = project.getAllPackageFragmentRoots();
		      for (IPackageFragmentRoot ipfr : packages)
		      {
		        IPackageFragment ipackage;
		        if ((ipackage = ipfr.getPackageFragment(packageName)) != null) {
		          if (!ipackage.getCompilationUnit(fileName + ".java").exists()) break;
		          updateStatus("Class already exists");
		          return;
		        }
		      }

		    }
		    catch (CoreException ce)
		    {
		      ce.printStackTrace();
		    }
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getContainerName() {
		return containerText.getText();
	}

	public String getFileName() {
		return classText.getText();
	}
	
	public String getClassName() {
		return classText.getText();
	}
	public String getPackageName() {
		return packageText.getText();
	}
}