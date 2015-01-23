package nl.tudelft.watchdog.ui.wizards.userregistration;

import nl.tudelft.watchdog.logic.network.NetworkUtils;
import nl.tudelft.watchdog.ui.preferences.Preferences;
import nl.tudelft.watchdog.ui.wizards.IdEnteredEndingPage;

/**
 * Possible finishing page in the wizard. If the user exists on the server, or
 * the server is not reachable, the user can exit here.
 */
class UserIdEnteredEndingPage extends IdEnteredEndingPage {

	/** Constructor. */
	public UserIdEnteredEndingPage() {
		super("user");
		pageNumber = 2;
	}

	protected String buildTransferURLforId() {
		return NetworkUtils.buildExistingUserURL(id);
	}

	protected void setId() {
		((UserRegistrationWizard) getWizard()).userid = id;
		Preferences.getInstance().setUserid(id);
	}

	protected String getId() {
		return ((UserWelcomePage) getWizard().getStartingPage()).getId();
	}

	@Override
	public boolean canFinish() {
		return false;
	}

}
