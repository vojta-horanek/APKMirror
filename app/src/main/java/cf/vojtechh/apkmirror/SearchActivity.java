package cf.vojtechh.apkmirror;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Window;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class SearchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        new MaterialDialog.Builder(this)
                .title(R.string.search)
                .inputRangeRes(1, 100, R.color.Warning)
                .input(R.string.search, R.string.nothing, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent i = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://www.apkmirror.com/?s=" + dialog.getInputEditText().getText()));

                        startActivity(i);
                        finish();
                    }
                })
                .negativeText(R.string.cancel)
                .show();
    }


}
