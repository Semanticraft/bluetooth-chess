import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.runner.RunWith;

import ui.connectionacceptance.ConnectionAcceptanceActivity;

@RunWith(AndroidJUnit4.class)
public class ConnectionAcceptanceActivityTest {
    @Rule
    public ActivityScenarioRule<ConnectionAcceptanceActivity> activityScenarioRule =
            new ActivityScenarioRule<>(ConnectionAcceptanceActivity.class);
}
