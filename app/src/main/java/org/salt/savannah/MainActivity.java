package org.salt.savannah;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import org.salt.savannah.ui.SaltApp;

/** SALT shell. Screens follow the iOS layout; messages stay ciphertext without the chat key. */
public final class MainActivity extends Activity {
    public static final String CONTENT_SECURITY_POLICY =
        "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' blob: data: https://images.unsplash.com; font-src 'self'; connect-src 'self'; "
            + "form-action 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'self'";

    private SaltApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = new SaltApp(this);
        setContentView(app.root);
    }

    @Override
    public void onBackPressed() {
        if (app == null || !app.onBack()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SaltApp.PHOTO_REQUEST && resultCode == RESULT_OK && app != null) {
            app.onPhoto(data);
        }
    }
}
