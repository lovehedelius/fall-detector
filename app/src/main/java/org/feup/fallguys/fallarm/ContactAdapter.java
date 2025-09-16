package org.feup.fallguys.fallarm;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.LayoutInflater;

public class ContactAdapter extends CursorAdapter {

    private final ProfileDataHelper helper;

    public ContactAdapter(Context context, Cursor cursor, ProfileDataHelper helper) {
        super(context, cursor, 0);
        this.helper = helper;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.contact_row, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Rebind the data to the existing view
        TextView nameView = view.findViewById(R.id.name);
        TextView phoneView = view.findViewById(R.id.phone);
        ImageButton deleteContactButton = view.findViewById(R.id.delete_button);

        // Fetch data from the cursor and use it to create the row
        int contactId = cursor.getInt(0); // used in case of deletion to keep track of which contact is deleted
        String name = cursor.getString(1);
        String phone = cursor.getString(2);
        nameView.setText(name);
        phoneView.setText(phone);

        deleteContactButton.setOnClickListener(v -> {
            // Delete contact and refresh the cursor
            helper.deleteContact(contactId);
            Cursor newCursor = helper.getContactsCursor();
            changeCursor(newCursor);
        });
    }
}

