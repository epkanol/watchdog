package eclipseUIReader;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import eclipseUIReader.UIComponentListeners.WindowListener;

/**
 * class that sets up the listeners for eclipse UI events
 */
public class UIListener implements IUIListener {
	@Override
	public void attachListeners(){		
		
		//for new windows
		PlatformUI.getWorkbench().addWindowListener(new WindowListener());
		
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows())
	    {
			//for existing windows
			WindowListener.addPageListener(window);       
	    }
		
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows())
	    {
			IWorkbenchPage activePage = window.getActivePage();
			if(activePage != null){
				IWorkbenchPart activePart = activePage.getActivePart();
				DocChangeListenerAttacher.listenToDocChanges(activePart);			
			}
		}
				
	}
}