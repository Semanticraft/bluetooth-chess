import static androidx.test.espresso.Espresso.onView;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.bluetoothchess.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ui.main.MainActivity;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testButtonText() {
        onView(ViewMatchers.withId(R.id.btn_save_state_history_nav_m))
                .check(ViewAssertions.matches(ViewMatchers.withText("Spielstandsübersicht")));
        onView(ViewMatchers.withId(R.id.btn_accept_connection_nav_m))
                .check(ViewAssertions.matches(ViewMatchers.withText("Verbindung annehmen")));
        onView(ViewMatchers.withId(R.id.btn_search_connection_nav_m))
                .check(ViewAssertions.matches(ViewMatchers.withText("Verbindung suchen")));
    }

    @Test
    public void testButtonNavigationSSH() {
        onView(ViewMatchers.withId(R.id.btn_save_state_history_nav_m))
                .perform(ViewActions.click());
        onView(ViewMatchers.withId(R.id.btn_back_ssh))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testButtonNavigationCA() {
        onView(ViewMatchers.withId(R.id.btn_accept_connection_nav_m))
                .perform(ViewActions.click());
        onView(ViewMatchers.withId(R.id.btn_back_ca))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testButtonNavigationC() {
        onView(ViewMatchers.withId(R.id.btn_search_connection_nav_m))
                .perform(ViewActions.click());
        onView(ViewMatchers.withId(R.id.btn_back_c))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }
}
