package com.example.ODBII;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.Selection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by wavegisAAA on 10/30/2014.
 */
public class InputCarID extends DialogFragment  {

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        //return super.onCreateDialog(savedInstanceState);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //LayoutInflater inflater = LayoutInflater.from(getActivity());



        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.input_car_id, null);
        ((MyActivity)getActivity()).CarID=((MyActivity)getActivity()).LoadSavePreFerences("CarID");
        //Toast.makeText(getActivity(),((MyActivity)getActivity()).CarID,Toast.LENGTH_SHORT).show();

        if(((MyActivity)getActivity()).CarID==null)
        {
            EditText editText =(EditText)view.findViewById(R.id.editText);
            editText.setText("");
        }
        else
        {
            EditText editText =(EditText)view.findViewById(R.id.editText);
            editText.setText(((MyActivity)getActivity()).CarID);
        }
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view);



        builder.setTitle("Input your car ID:");
        //builder.setMessage("This is an alert with no consequence");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Send the positive button event back to the host activity
                mListener.onDialogPositiveClick(InputCarID.this);
            }
        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send the negative button event back to the host activity
                        mListener.onDialogNegativeClick(InputCarID.this);
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //LayoutInflater inflater = LayoutInflater.from(getActivity());

        ((MyActivity)getActivity()).CarID=((MyActivity)getActivity()).LoadSavePreFerences("CarID");
        Toast.makeText(getActivity(),((MyActivity)getActivity()).CarID,Toast.LENGTH_SHORT).show();
        View view = inflater.inflate(R.layout.input_car_id, null);
        if(((MyActivity)getActivity()).CarID==null)
        {
            EditText editText =(EditText)view.findViewById(R.id.editText);
            editText.setText("");
        }
        else
        {
            EditText editText =(EditText)view.findViewById(R.id.editText);
            editText.setText(((MyActivity)getActivity()).CarID);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
