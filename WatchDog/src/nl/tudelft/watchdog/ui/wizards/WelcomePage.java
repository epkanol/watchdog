package nl.tudelft.watchdog.ui.wizards;

import java.net.MalformedURLException;
import java.net.URL;

import nl.tudelft.watchdog.ui.UIUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * The first page of a Wizard. It asks an initial yes-or no question. Depending
 * on the answer, it dynamically display an input field or an introduction page.
 */
abstract public class WelcomePage extends FinishableWizardPage {

	/** The welcome title. To be changed by subclasses. */
	protected String welcomeTitle;

	/** The text to welcome the user. To be changed by subclasses. */
	protected String welcomeText;

	/** The text on the label for the user input. To be changed by subclasses. */
	protected String labelText;

	/** The tooltip on the label and inputText. */
	protected String inputToolTip;

	/** The label question. To be changed by subclasses. */
	protected String labelQuestion;

	/** The length (in characters) of the WatchDog id. */
	private static final int idLength = 40;

	/**
	 * The Composite which holds the text field for the new user welcome or
	 * holds the text field for the existing user login.
	 */
	private Composite dynamicContent;

	/**
	 * The id as entered by the user (note: as delivered from this wizard page,
	 * still unchecked).
	 */
	private Text userInput;

	/** The no button from the question. */
	private Button radioButtonNo;

	/** Constructor. */
	public WelcomePage(String title) {
		super(title);
		setTitle(title);
	}

	@Override
	public void createControl(Composite parent) {
		Composite topContainer = UIUtils.createGridedComposite(parent, 1);

		// Sets up the basis layout
		createQuestionComposite(topContainer);

		setControl(topContainer);
		setPageComplete(false);
	}

	/**
	 * Creates and returns the question whether WatchDog Id is already known.
	 */
	private Composite createQuestionComposite(final Composite parent) {
		Composite composite = UIUtils.createGridedComposite(parent, 2);
		UIUtils.createLabel(labelQuestion, composite);

		final Composite radioButtons = UIUtils.createGridedComposite(composite,
				1);
		radioButtons.setLayout(new FillLayout());
		final Button radioButtonYes = UIUtils.createRadioButton(radioButtons,
				"Yes");
		radioButtonYes.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				removeDynamicContent(parent);
				dynamicContent = createLoginComposite(parent);
				parent.layout();
				setPageComplete(false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		radioButtonNo = UIUtils.createRadioButton(radioButtons, "No");
		radioButtonNo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				setErrorMessageAndPageComplete(null);
				removeDynamicContent(parent);
				dynamicContent = createWelcomeComposite(parent);
				parent.layout();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		return composite;
	}

	/** Removes the dynamic content from the page, if it exists. */
	private void removeDynamicContent(final Composite parent) {
		if (dynamicContent != null) {
			dynamicContent.dispose();
			parent.layout();
			parent.update();
		}
	}

	/**
	 * Creates and returns an input field, in which user can enter their
	 * existing WatchDog ID.
	 */
	private Composite createLoginComposite(Composite parent) {
		Composite composite = UIUtils.createGridedComposite(parent, 2);
		composite.setLayoutData(UIUtils.createFullGridUsageData());

		userInput = UIUtils.createLinkedFieldInput(labelText, inputToolTip,
				composite);
		userInput.setTextLimit(idLength);
		userInput.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (userInput.getText().length() == idLength) {
					setErrorMessageAndPageComplete(null);
				} else {
					setErrorMessageAndPageComplete("Not a valid id.");
				}
				getWizard().getContainer().updateButtons();
			}
		});

		return composite;
	}

	/** Creates and returns a welcoming composite for new ids. */
	private Composite createWelcomeComposite(Composite parent) {
		Composite composite = UIUtils.createGridedComposite(parent, 1);
		createSeparator(composite);
		UIUtils.createBoldLabel(welcomeTitle, composite);
		UIUtils.createLabel("", composite);

		Link linkedText = new Link(composite, SWT.WRAP);
		linkedText.setText(welcomeText);
		GridData labelData = new GridData();
		labelData.widthHint = parent.getClientArea().width - 30;
		linkedText.setLayoutData(labelData);
		linkedText.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					// Open default external browser
					PlatformUI.getWorkbench().getBrowserSupport()
							.getExternalBrowser().openURL(new URL(event.text));
				} catch (PartInitException | MalformedURLException exception) {
					// Browser could not be opened. We do nothing about it.
				}
			}
		});

		return composite;
	}

	/** Creates a horizontal separator. */
	private void createSeparator(Composite parent) {
		Label separator = UIUtils.createLabel("", SWT.SEPARATOR
				| SWT.HORIZONTAL | SWT.FILL, parent);
		GridData layoutData = UIUtils.createFullGridUsageData();
		layoutData.horizontalSpan = 2;
		separator.setLayoutData(layoutData);
	}

	/** @return Whether a possibly valid id has been entered. */
	public boolean hasValidUserId() {
		return userInput != null && !userInput.isDisposed()
				&& getErrorMessage() == null && isPageComplete();
	}

	/** @return The id entered by the user. */
	public/* package */String getId() {
		return userInput.getText();
	}

	/**
	 * @return Whether the user wants to create a new id (<code>true</code> in
	 *         that case, <code>false</code> otherwise).
	 */
	public boolean getRegisterNewId() {
		return radioButtonNo.getSelection();
	}

	@Override
	public boolean canFinish() {
		return false;
	}

}