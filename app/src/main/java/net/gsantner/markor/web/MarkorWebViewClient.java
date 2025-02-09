/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.web;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;

import java.io.File;
import java.net.URLDecoder;

public class MarkorWebViewClient extends WebViewClient {

    private final Activity _activity;
    private int _restoreScrollY = 0;
    private boolean _restoreScrollYEnabled = false;

    public MarkorWebViewClient(Activity activity) {
        _activity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        try {
            Context context = view.getContext();

            if (url.equals("about:blank")) {
                view.reload();
                return true;
            }
            if (url.startsWith("file:///android_asset/")) {
                return false;
            } else if (url.startsWith("file://")) {
                MarkorContextUtils su = new MarkorContextUtils(view.getContext());
                File file = new File(URLDecoder.decode(url.replace("file://", "")));
                String mimetype;
                for (String str : new String[]{file.getAbsolutePath(), file.getAbsolutePath().replaceFirst("[#].*$", ""), file.getAbsolutePath() + ".md", file.getAbsolutePath() + ".txt"}) {
                    File f = new File(str);
                    if (f.exists() && FormatRegistry.isTextFile(f)) {
                        file = f;
                        break;
                    }
                }
                if (file.isDirectory()) {
                    _activity.startActivity(new Intent(_activity, MainActivity.class)
                            .putExtra(Document.EXTRA_PATH, file));
                } else if (FormatRegistry.isTextFile(file)) {
                    _activity.startActivity(new Intent(_activity, DocumentActivity.class)
                            .putExtra(Document.EXTRA_PATH, file));
                } else if (file.getName().toLowerCase().endsWith(".apk")) {
                    su.requestApkInstallation(context, file);
                } else if ((mimetype = su.getMimeType(_activity, url)) != null) {
                    su.viewFileInOtherApp(context, file, mimetype);
                } else {
                    su.viewFileInOtherApp(context, file, null);
                }
            } else {
                MarkorContextUtils su = new MarkorContextUtils(_activity);
                AppSettings settings = ApplicationObject.settings();
                if (!settings.isOpenLinksWithChromeCustomTabs() || (settings.isOpenLinksWithChromeCustomTabs() && !su.openWebpageInChromeCustomTab(context, url))) {
                    su.openWebpageInExternalBrowser(context, url);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (_restoreScrollYEnabled) {
            for (int dt : new int[]{50, 100, 150, 200, 250, 300}) {
                view.postDelayed(() -> view.setScrollY(_restoreScrollY), dt);
            }
            _restoreScrollYEnabled = false;
        }
        super.onPageFinished(view, url);
    }

    public void setRestoreScrollY(int scrollY) {
        _restoreScrollY = scrollY;
        _restoreScrollYEnabled = true;
    }


}
