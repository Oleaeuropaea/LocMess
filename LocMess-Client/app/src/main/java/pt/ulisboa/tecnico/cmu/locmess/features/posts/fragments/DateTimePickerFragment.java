package pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

public class DateTimePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    public interface NoticeDateTimePickerListener {
        void onDateTimeSet(View view, LocalDateTime dateTime);
    }

    public static final String ARGUMENT_VIEW_ID = "ARGUMENT_VIEW_ID";
    private static final String BUNDLE_VIEW_ID = "BUNDLE_VIEW_ID";
    private static final String BUNDLE_DATE = "BUNDLE_DATE";

    private int mViewId;
    private static NoticeDateTimePickerListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            mListener = (NoticeDateTimePickerListener) getContext();
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getContext().toString()
                    + " must implement NoticeDialogListener");
        }

        mViewId = getArguments().getInt(ARGUMENT_VIEW_ID);

        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(getActivity(), this, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());

        return datePickerDialog;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        LocalDate localDate = new LocalDate(year, month + 1, day);

        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_VIEW_ID, mViewId);
        bundle.putSerializable(BUNDLE_DATE, localDate);

        DialogFragment timePicker = new DateTimePickerFragment.TimePickerFragment();
        timePicker.setArguments(bundle);
        timePicker.show(getFragmentManager(), "timePicker");
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            return new TimePickerDialog(getActivity(), this, hour, minute + 1, true);
        }

        public void onTimeSet(TimePicker timePickerView, int hourOfDay, int minute) {
            int editTextViewID = getArguments().getInt(DateTimePickerFragment.BUNDLE_VIEW_ID);
            View view = getActivity().findViewById(editTextViewID);

            LocalDate localDate = (LocalDate) getArguments().getSerializable(DateTimePickerFragment.BUNDLE_DATE);
            LocalDateTime localDateTime = localDate.toLocalDateTime(new LocalTime(hourOfDay, minute));

            mListener.onDateTimeSet(view, localDateTime);
        }
    }
}
