package com.example.ODBII;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * Created by wavegisAAA on 10/30/2014.
 */
public class InputCarID extends DialogFragment  {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //return super.onCreateDialog(savedInstanceState);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("App Title");
        builder.setMessage("This is an alert with no consequence");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // You don't have to do anything here if you just want it dismissed when clicked
                ((MyActivity)getActivity()).StartBTDeviceClass();
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }

}
