package cf.vojtechh.apkmirror;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

public class SearchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.search_dialog, null);
        final EditText editText = (EditText) alertLayout.findViewById(R.id.textViewSearch);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.search);
        // this is set the view from XML inside AlertDialog
        alert.setView(alertLayout);
        // disallow cancel of AlertDialog on click of back button and outside touch
        alert.setCancelable(false);
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = String.valueOf(editText.getText());
                Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://www.apkmirror.com/?s=" + text));

                startActivity(i);
                finish();
            }
        });
        alert.show();

    }


}
