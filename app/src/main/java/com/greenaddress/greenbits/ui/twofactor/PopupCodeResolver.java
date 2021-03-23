package com.greenaddress.greenbits.ui.twofactor;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.greenapi.data.HWDeviceRequiredData;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class PopupCodeResolver implements CodeResolver {
    private Activity activity;
    private MaterialDialog dialog;

    public PopupCodeResolver(final Activity activity) {
        this.activity=activity;
    }

    @Override
    public SettableFuture<String> hardwareRequest(final HWDeviceRequiredData requiredData) {
        return null;
    }

    @Override
    public SettableFuture<String> code(final String method, final int attemptsRemaining) {
        final SettableFuture<String> future = SettableFuture.create();

        final MaterialDialog.Builder builder =
            UI.popup(activity, activity.getString(R.string.id_please_provide_your_1s_code,
                                                  method))
            .inputType(InputType.TYPE_CLASS_NUMBER)
            .icon(getIconFor(method))
            .cancelable(false)
            .alwaysCallInputCallback()
            .input(activity.getString(R.string.id_attempts_remaining_d, attemptsRemaining), "", (dialog, input) -> {
                if(input != null && input.length() == 6){
                    Log.d("RSV", "PopupCodeResolver OK callback");
                    future.set(input.toString());
                    dismiss();
                }
            })
            .onPositive((dialog, which) -> {
                Log.d("RSV", "PopupCodeResolver CANCEL callback");
                future.set(null);
            })
            .onNegative((dialog, which) -> {
            Log.d("RSV", "PopupCodeResolver CANCEL callback");
            future.set(null);
        });

        activity.runOnUiThread(() -> {
            Log.d("RSV", "PopupCodeResolver dialog show");
            dialog = builder.show();
        });

        return future;
    }

    @Override
    public void dismiss() {
        if (dialog != null) {
            activity.runOnUiThread(() -> {
                dialog.dismiss();
            });
        }
    }

    private Drawable getIconFor(final String method) {
        switch (method) {
        case "email": return ContextCompat.getDrawable(activity, R.drawable.ic_2fa_email);
        case "sms": return ContextCompat.getDrawable(activity, R.drawable.ic_2fa_sms);
        case "gauth": return ContextCompat.getDrawable(activity, R.drawable.ic_2fa_google);
        case "phone": return ContextCompat.getDrawable(activity, R.drawable.ic_2fa_call);
        default: return null;
        }
    }
}
