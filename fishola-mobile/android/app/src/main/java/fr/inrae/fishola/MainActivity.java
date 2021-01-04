package fr.inrae.fishola;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;

import java.util.ArrayList;

import android.webkit.CookieManager;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Initializes the Bridge
    this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
      // Additional plugins you've installed go here
      // Ex: add(TotallyAwesomePlugin.class);
    }});

  }

  /*
   * Les cookies ne sont pas immédiatement persistés ce qui fait que si l'application est tuée avant
   * que les cookies soient parsistés, ils sont perdus : https://github.com/ionic-team/capacitor/issues/3012
   * La méthode ci-dessous est le workaround décrit dans le ticket.
   */
  @Override
  public void onPause() {
    super.onPause();
    CookieManager.getInstance().flush();
  }

}
