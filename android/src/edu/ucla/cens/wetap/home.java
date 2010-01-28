package edu.ucla.cens.wetap;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.content.Context;
import android.widget.Button;

public class home extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate (b);
        setContentView (R.layout.home);

        ((Button) findViewById (R.id.start_survey)).setOnClickListener (survey_button_listener);
        ((Button) findViewById (R.id.start_map)).setOnClickListener (map_button_listener);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu m) {
        super.onCreateOptionsMenu (m);

        m.add (Menu.NONE, 0, Menu.NONE, "About").setIcon (android.R.drawable.ic_menu_info_details);
        m.add (Menu.NONE, 1, Menu.NONE, "Instructions").setIcon (android.R.drawable.ic_menu_help);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem index) {
        Context ctx = home.this;
        Intent i;
        switch (index.getItemId()) {
            case 0:
                i = new Intent (ctx, about.class);
                break;
            case 1:
                i = new Intent (ctx, instructions.class);
                break;
            default:
                return false;
        }
        ctx.startActivity (i);
        this.finish();
        return true;
    }

    View.OnClickListener survey_button_listener = new View.OnClickListener () {
        public void onClick (View v) {
            home.this.startActivity (new Intent (home.this, survey.class));
            home.this.finish ();
        }
    };

    View.OnClickListener map_button_listener = new View.OnClickListener () {
        public void onClick (View v) {
            home.this.startActivity (new Intent (home.this, map.class));
            home.this.finish ();
        }
    };
}
