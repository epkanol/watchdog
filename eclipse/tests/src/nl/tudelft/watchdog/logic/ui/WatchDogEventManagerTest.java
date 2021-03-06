package nl.tudelft.watchdog.logic.ui;

import java.util.Date;

import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import nl.tudelft.watchdog.core.logic.interval.IDEIntervalManagerBase;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.DebugInterval;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.EditorIntervalBase;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.IntervalBase;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.IntervalType;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.ReadingInterval;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.TypingInterval;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.UserActiveInterval;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.WatchDogViewInterval;
import nl.tudelft.watchdog.core.logic.storage.PersisterBase;
import nl.tudelft.watchdog.core.logic.ui.InactivityNotifier;
import nl.tudelft.watchdog.core.logic.ui.InactivityNotifiers;
import nl.tudelft.watchdog.core.logic.ui.UserInactivityNotifier;
import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEventType;
import nl.tudelft.watchdog.eclipse.logic.InitializationManager;
import nl.tudelft.watchdog.eclipse.logic.interval.IntervalManager;
import nl.tudelft.watchdog.eclipse.util.WatchDogUtils;

/**
 * Tests the {@link WatchDogEventManager}. Because this creates the intervals that are
 * eventually transfered to the server, this is one of the most crucial parts of
 * WatchDog. Tests could flicker because they deal with timers (and Java gives
 * no guarantee as to when these timers will be executed).
 *
 * TODO (TimvdLippe): This test is disabled until we can properly deflake it.
 * It is currently relying on Thread synchronization and sleeps, which are flaky.
 */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class WatchDogEventManagerTest {

	private static final int USER_ACTIVITY_TIMEOUT = 300;
	private static final int TIMEOUT_GRACE_PERIOD = (int) (USER_ACTIVITY_TIMEOUT * 1.1);
	private IDEIntervalManagerBase intervalManager;
	private ITextEditor mockedTextEditor;
	private EditorIntervalBase editorInterval;
	private IntervalBase interval;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		IDEIntervalManagerBase intervalManagerReal = new IntervalManager(
				Mockito.mock(PersisterBase.class),
				Mockito.mock(PersisterBase.class));
		intervalManager = Mockito.spy(intervalManagerReal);
		mockedTextEditor = Mockito.mock(ITextEditor.class);

		WatchDogEventType.intervalManager = intervalManager;
		WatchDogEventType.editorSpecificImplementation = new InitializationManager.EclipseWatchDogEventSpecificImplementation(intervalManager);
		InactivityNotifiers.READING.updateNotifier(new InactivityNotifier(USER_ACTIVITY_TIMEOUT, WatchDogEventType.READING_INACTIVITY));
		InactivityNotifiers.USER_INACTIVITY.updateNotifier(new UserInactivityNotifier(USER_ACTIVITY_TIMEOUT, WatchDogEventType.USER_INACTIVITY));
		InactivityNotifiers.TYPING.updateNotifier(new InactivityNotifier(USER_ACTIVITY_TIMEOUT, WatchDogEventType.TYPING_INACTIVITY));
	}

	@Test
	public void create_read_interval() {
		createMockEvent(WatchDogEventType.ACTIVE_FOCUS);
		Mockito.verify(intervalManager).addInterval(Mockito.isA(ReadingInterval.class));
	}

	@Test
	public void create_read_interval_only_once() {
		createMockEvent(WatchDogEventType.ACTIVE_FOCUS);
		Mockito.verify(intervalManager).addInterval(
				Mockito.isA(ReadingInterval.class));
		createMockEvent(WatchDogEventType.CARET_MOVED);
		createMockEvent(WatchDogEventType.CARET_MOVED);
		createMockEvent(WatchDogEventType.PAINT);
		Mockito.verify(intervalManager).addInterval(
				Mockito.isA(ReadingInterval.class));
	}

	@Test
	public void create_user_interval_only_once() {
		createMockEvent(WatchDogEventType.USER_ACTIVITY);
		WatchDogUtils.sleep(50);
		createMockEvent(WatchDogEventType.USER_ACTIVITY);
		WatchDogUtils.sleep(50);
		createMockEvent(WatchDogEventType.USER_ACTIVITY);
		Mockito.verify(intervalManager).addInterval(
				Mockito.isA(IntervalBase.class));
	}

	@Test
	public void read_interval_is_closed() {
		createMockEvent(WatchDogEventType.ACTIVE_FOCUS);
		Mockito.verify(intervalManager).addInterval(
				Mockito.isA(ReadingInterval.class));
		createMockEvent(WatchDogEventType.INACTIVE_FOCUS);
		Mockito.verify(intervalManager, Mockito.atLeastOnce()).closeInterval(
				Mockito.isA(ReadingInterval.class), Mockito.isA(Date.class));
		Assert.assertEquals(null, intervalManager.getEditorInterval());
	}

	@Test
	public void create_write_interval() {
		createMockEvent(WatchDogEventType.SUBSEQUENT_EDIT);
		Mockito.verify(intervalManager).addInterval(
				Mockito.isA(TypingInterval.class));
	}

	@Test
	public void create_write_interval_and_not_read_interval() {
		createMockEvent(WatchDogEventType.START_EDIT);
		createMockEvent(WatchDogEventType.SUBSEQUENT_EDIT);
		Mockito.verify(intervalManager, Mockito.atLeast(1)).addInterval(
				Mockito.isA(TypingInterval.class));
		Mockito.verify(intervalManager, Mockito.never()).addInterval(
				Mockito.isA(ReadingInterval.class));
		createMockEvent(WatchDogEventType.CARET_MOVED);
		createMockEvent(WatchDogEventType.SUBSEQUENT_EDIT);
		createMockEvent(WatchDogEventType.PAINT);
		Mockito.verify(intervalManager, Mockito.atLeast(1)).addInterval(
				Mockito.isA(TypingInterval.class));
		Mockito.verify(intervalManager, Mockito.never()).addInterval(
				Mockito.isA(ReadingInterval.class));
	}

	@Test
	public void closing_ide_closes_write_interval() {
		createMockEvent(WatchDogEventType.SUBSEQUENT_EDIT);
		createMockEvent(WatchDogEventType.END_IDE);
		Mockito.verify(intervalManager, Mockito.atLeastOnce()).closeInterval(
				Mockito.isA(TypingInterval.class), Mockito.isA(Date.class));
	}

	@Test
	public void after_timeout_no_more_interval() {
		createMockEvent(WatchDogEventType.ACTIVE_WINDOW);
		createMockEvent(WatchDogEventType.USER_ACTIVITY);
		Mockito.verify(intervalManager, Mockito.timeout(TIMEOUT_GRACE_PERIOD))
				.closeInterval(Mockito.isA(IntervalBase.class),
						Mockito.any(Date.class));
		Assert.assertEquals(null, intervalManager.getEditorInterval());
	}

	@Test
	public void after_reading_timeout_no_more_interval() {
		createMockEvent(WatchDogEventType.ACTIVE_FOCUS);
		Mockito.verify(intervalManager,
				Mockito.timeout(TIMEOUT_GRACE_PERIOD).atLeast(1))
				.closeInterval(Mockito.isA(ReadingInterval.class),
						Mockito.any(Date.class));
		Assert.assertEquals(null, intervalManager.getEditorInterval());
	}

	@Test
	public void after_writing_timeout_no_more_interval() {
		createMockEvent(WatchDogEventType.SUBSEQUENT_EDIT);
		// first close null interval
		Mockito.verify(intervalManager,
				Mockito.timeout(TIMEOUT_GRACE_PERIOD).atLeast(1))
				.closeInterval((IntervalBase) Mockito.isNull(),
						Mockito.any(Date.class));

		Mockito.verify(intervalManager,
				Mockito.timeout(TIMEOUT_GRACE_PERIOD).atLeast(1))
				.closeInterval(Mockito.isA(TypingInterval.class),
						Mockito.any(Date.class));
	}

	@Test
	public void after_prolonged_reading_timeout_no_more_interval() {
		createMockEvent(WatchDogEventType.ACTIVE_FOCUS);
		createMockEvent(WatchDogEventType.CARET_MOVED);
		Mockito.verify(intervalManager,
				Mockito.timeout(TIMEOUT_GRACE_PERIOD * 2).atLeast(1))
				.closeInterval(Mockito.isA(ReadingInterval.class),
						Mockito.any(Date.class));
		Assert.assertEquals(null, intervalManager.getEditorInterval());
	}

	@Test
	public void no_more_user_activities_should_close_interval() {
		createMockEvent(WatchDogEventType.USER_ACTIVITY);
		WatchDogUtils.sleep(USER_ACTIVITY_TIMEOUT / 5);
		createMockEvent(WatchDogEventType.ACTIVE_FOCUS);
		WatchDogUtils.sleep(USER_ACTIVITY_TIMEOUT / 2);
		editorInterval = intervalManager.getEditorInterval();
		interval = intervalManager.getInterval(UserActiveInterval.class);

		createMockEvent(WatchDogEventType.CARET_MOVED);
		WatchDogUtils.sleep(USER_ACTIVITY_TIMEOUT / 2);
		createMockEvent(WatchDogEventType.CARET_MOVED);

		Assert.assertFalse(editorInterval.isClosed());
		Assert.assertFalse(interval.isClosed());
		createMockEvent(WatchDogEventType.USER_ACTIVITY);
		WatchDogUtils.sleep(USER_ACTIVITY_TIMEOUT / 2);

		WatchDogUtils.sleep(USER_ACTIVITY_TIMEOUT * 3);
		Assert.assertEquals(null, intervalManager.getEditorInterval());
		Assert.assertEquals(null,
				intervalManager.getInterval(UserActiveInterval.class));
		Assert.assertTrue(editorInterval.isClosed());
		Assert.assertTrue(interval.isClosed());
	}

	/**
	 * Advanced synchronization test, which tests whether sub-sequent intervals
	 * (stopped because another interval was stopped) have the same end time
	 * stamp. These intervals used to have a slightly delayed end timestamp.
	 * This test should document that this is fixed.
	 */
	@Test
	public void has_correct_timestamp_for_write_interval() {
		edit_event_creates_user_activity();
		WatchDogUtils.sleep(USER_ACTIVITY_TIMEOUT);
		WatchDogUtils.sleep(USER_ACTIVITY_TIMEOUT);

		Assert.assertTrue(editorInterval.getEnd().getTime() <= interval
				.getEnd().getTime());
	}

	/**
	 * Advanced synchronization test, which certifies that intervals that should
	 * be created independent of each other have indeed different timestamps.
	 */
	@Test
	public void start_timestamp_should_be_different_for_different_intervals() {
		edit_event_creates_user_activity();

		Assert.assertTrue(interval.getStart().before(editorInterval.getStart()));
	}

	/**
	 * This test verifies that one additional {@link IntervalBase} interval is
	 * created when a writing interval is created. This should be of type
	 * {@link IntervalType#USER_ACTIVE}.
	 */
	@Test
	public void edit_event_creates_user_activity() {
		createMockEvent(WatchDogEventType.SUBSEQUENT_EDIT);
		WatchDogUtils.sleep(TIMEOUT_GRACE_PERIOD / 5);
		editorInterval = intervalManager.getEditorInterval();
		interval = intervalManager.getInterval(UserActiveInterval.class);
		Assert.assertNotNull(interval);
	}

	/**
	 * Advanced synchronization test, which tests whether sub-sequent intervals
	 * (started because of another interval was created) have the same starting
	 * time stamp. These intervals used to have a slightly delayed timestamp.
	 * This test should document that this is fixed.
	 */
	@Test
	public void has_correct_start_timestamp_for_writing_intervals() {
		edit_event_creates_user_activity();

		Assert.assertEquals(editorInterval.getStart(), interval.getStart());
	}

	@Test
	public void create_watchdog_interval() {
		createMockEvent(WatchDogEventType.START_WATCHDOGVIEW);
		Mockito.verify(intervalManager).addInterval(
				Mockito.isA(WatchDogViewInterval.class));
		createMockEvent(WatchDogEventType.END_WATCHDOGVIEW);
		Mockito.verify(intervalManager, Mockito.atLeastOnce()).closeInterval(
				Mockito.isA(WatchDogViewInterval.class), Mockito.isA(Date.class));

	}

	@Test
	public void create_debug_interval() {
		createMockEvent(WatchDogEventType.START_DEBUG);
		Mockito.verify(intervalManager).addInterval(Mockito.isA(DebugInterval.class));
		createMockEvent(WatchDogEventType.END_DEBUG);
		Mockito.verify(intervalManager, Mockito.atLeastOnce()).closeInterval(
				Mockito.isA(DebugInterval.class), Mockito.isA(Date.class));
	}

	private void createMockEvent(WatchDogEventType watchDogEventType) {
		if (watchDogEventType == WatchDogEventType.SUBSEQUENT_EDIT) {
			watchDogEventType.process(new WatchDogEventType.EditorWithModCount(mockedTextEditor, 0));
		} else {
			watchDogEventType.process(mockedTextEditor);
		}
	}

}
