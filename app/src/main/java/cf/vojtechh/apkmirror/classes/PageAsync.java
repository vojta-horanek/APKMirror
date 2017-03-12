package cf.vojtechh.apkmirror.classes;

import android.graphics.Color;
import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

import cf.vojtechh.apkmirror.interfaces.AsyncResponse;

public class PageAsync extends AsyncTask<String, Integer, Integer> {

    private String themeColor;

    public AsyncResponse response = null;


    @Override
    protected Integer doInBackground(String... url) {

        try {

            Document doc = Jsoup.connect(url[0]).get();

            Elements metaElements = doc.select("meta[name=theme-color]");

            if (metaElements.size() != 0) {
                themeColor = metaElements.get(0).attr("content");
            } else {
                themeColor = "#FF8B14";
            }

//            Not going to use this since is pretty much useless
//            (gets app title for recents)

//            Elements h1Elements = doc.select("h1[class=app-title]");
//
//            if (h1Elements.size() != 0) {
//                appTitle = h1Elements.get(0).attr("title");
//            } else {
//                appTitle = "APKMirror";
//            }

            return Color.parseColor(themeColor);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    protected void onPostExecute(Integer result) {

        if (result != null) {
            response.onProcessFinish(result);
        } else {
            response.onProcessFinish(Color.parseColor("#FF8B14"));
        }
    }

}
