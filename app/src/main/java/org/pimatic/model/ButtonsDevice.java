package org.pimatic.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Oliver Schneider <oliverschneider89+sweetpi@gmail.com>
 */
public class ButtonsDevice extends Device implements Serializable {

    protected List<Button> buttons;

    public ButtonsDevice(JSONObject obj) throws JSONException {
        super(obj);
        this.buttons = new ArrayList<>();
        JSONArray buttons = getConfig().getJSONArray("buttons");
        if(buttons != null) {
            for (int i = 0; i < buttons.length(); i++) {
                JSONObject button = buttons.getJSONObject(i);
                this.buttons.add(new Button(
                        button.getString("id"),
                        button.getString("text"),
                        button.optBoolean("confirm", false)
                ));
            }
        }
    }

    public List<Button> getButtons() {
        return buttons;
    }

    public <T> T visit(DeviceVisitor<T> v) {
        return v.visitButtonsDevice(this);
    }

    public class Button implements Serializable {
        public String id;
        public String text;
        boolean confirm;

        public Button(String id, String text, boolean confirm) {
            this.id = id;
            this.text = text;
            this.confirm = confirm;
        }
    }

}
