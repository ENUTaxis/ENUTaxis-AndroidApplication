package com.napier.enutaxis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MakeBooking extends Activity 
	implements
		OnClickListener,
		OnMapClickListener,
		OnMapLongClickListener {

	/*
	 * General variables
	 */
	private static final String TAG = "BOOKING";
	private final Context that = this;
	
	/*
	 * User information
	 */
	private String firstName		= null;
	private String lastName			= null;
	private String phoneNumber	= null;
	private String emailAddress	= null;
	private String matNumber		= null;

	/*
	 * Date/Time variables
	 */
	private int when = 0;
	static final private int ASAP = 1;
	static final private int CHOSEN_DATE_TIME = 2;
	private int hour;
	private int minute;
	private int day;
	private int month;
	private int year;

	/*
	 * Google Map stuff
	 */
	private GoogleMap map;
	private LatLng Edinburgh     = new LatLng(55.973414, -3.188782);
	private LatLng ArthursSeat   = new LatLng(55.944285, -3.161895);
	private LatLng WestTollcross = new LatLng(55.943252, -3.205712);
	private CameraPosition cam   = new CameraPosition(Edinburgh, 12, 45, 0);
	private Marker fromMarker;
	private Marker toMarker;
	
	/*
	 * Geocoder stuff + handler for AutoCompleteTextView
	 */
	private Geocoder mGeocoder;
	private static final int THRESHOLD = 3;
	private List<Address> autoCompleteSuggestionAddresses;
	
	/*
	 *  Graphical elements
	 */
	private Button selectDateTime;
	private Button setDateTimeASAP;
	private Button findTaxis;
	private AutoCompleteTextView fromStreet;
	private AutoCompleteTextView toStreet;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.booking);
		
		/*
		 * Initialize user information from saved preferences
		 */
		SharedPreferences pref = getSharedPreferences("PreFilledForm", MODE_PRIVATE);
		if(pref.contains("firstName")) {
			firstName = pref.getString("firstName", "");
		}

		if(pref.contains("lastName")) {
			lastName = pref.getString("lastName", "");
		}

		if(pref.contains("phone")) {
			phoneNumber = pref.getString("phone", "");
			emailAddress = phoneNumber + "@live.napier.ac.uk";
		}

		if(pref.contains("matriculationNumber")) {
			matNumber = pref.getString("matriculationNumber", "");
		}

		/*
		 * Create map options for the initialization of the map
		 */
		GoogleMapOptions options = new GoogleMapOptions();
		options.mapType(GoogleMap.MAP_TYPE_TERRAIN)
			.compassEnabled(true)
			.rotateGesturesEnabled(false)
			.tiltGesturesEnabled(false)
			.zoomControlsEnabled(false)
			.zoomGesturesEnabled(true)
			.camera(cam);
		
		/*
		 * Initialize the global variable of the current displayed Map
		 */
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		if(map != null) {
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(Edinburgh, 12), 2000, null);
			map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
			map.setMyLocationEnabled(true);
			map.setOnMapClickListener(this);
			map.setOnMapLongClickListener(this);
		}
		
		/*
		 * Initialize from/to markers
		 */
		fromMarker = map.addMarker(new MarkerOptions()
											.position(Edinburgh)
											.title("Departure")
											.visible(false));
		toMarker = map.addMarker(new MarkerOptions()
											.position(Edinburgh)
											.title("Arrival")
											.visible(false));
		
		/*
		 * Geocoder stuffs
		 */
		mGeocoder = new Geocoder(this, Locale.getDefault());

		/*
		 * Handle other graphical elements
		 */
		selectDateTime = (Button) findViewById(R.id.btnDateTime);
		selectDateTime.setOnClickListener(this);
		
		setDateTimeASAP = (Button) findViewById(R.id.btnASAP);
		setDateTimeASAP.setOnClickListener(this);
		
		findTaxis	= (Button) findViewById(R.id.btnFindTaxis);
		findTaxis.setOnClickListener(this);
		
		fromStreet = (AutoCompleteTextView) findViewById(R.id.fromStreet);
		fromStreet.setThreshold(THRESHOLD);
		fromStreet.setAdapter(new AutoCompleteAdapter(this));
		fromStreet.setOnItemClickListener(new OnItemClickListener() {
			@Override
      public void onItemClick(AdapterView<?> a0, View a1, int a2, long a3) {
				Address address = (Address) a0.getAdapter().getItem(a2);
				Log.d(TAG, "Set departure Marker: " + address.getAddressLine(0));
				fromMarker.setPosition(new LatLng(address.getLatitude(), address.getLongitude()));
				fromMarker.setVisible(true);
				InputMethodManager imm = (InputMethodManager)getSystemService(
			      Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(fromStreet.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				toStreet.requestFocus();
				imm.hideSoftInputFromWindow(toStreet.getWindowToken(), InputMethodManager.SHOW_IMPLICIT);
      }
		});
		
		toStreet = (AutoCompleteTextView) findViewById(R.id.toStreet);
		toStreet.setThreshold(THRESHOLD);
		toStreet.setAdapter(new AutoCompleteAdapter(this));
		toStreet.setOnItemClickListener(new OnItemClickListener() {
			@Override
      public void onItemClick(AdapterView<?> a0, View a1, int a2, long a3) {
				Address address = (Address) a0.getAdapter().getItem(a2);
				Log.d(TAG, "Set arrival Marker: " + address.getAddressLine(0));
				toMarker.setPosition(new LatLng(address.getLatitude(), address.getLongitude()));
				toMarker.setVisible(true);
				InputMethodManager imm = (InputMethodManager)getSystemService(
			      Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(toStreet.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
      }
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void testGeocoding() {
		Geocoder geocoder = new Geocoder(this, Locale.getDefault());
		Log.d(TAG, "Geocoder ready");
		try {
			List<Address> list = geocoder.getFromLocation(55.973414, -3.188782, 1);
			String address = list.get(0).getAddressLine(0) + ", " + list.get(0).getAddressLine(1) + ", " + list.get(0).getAddressLine(2);   
			Log.d(TAG, address);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			List<Address> list = geocoder.getFromLocationName("Princess Street, Edinburgh", 1);
			String address1 = list.get(0).getAddressLine(0) + ", " + list.get(0).getAddressLine(1) + ", " + list.get(0).getAddressLine(2) + " " + list.get(0).getLatitude() + ":" + list.get(0).getLongitude();
			//			address = list.get(0).toString();
			Log.d(TAG, address1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.btnDateTime) {
			Log.d(TAG, "onClick: btnDate");
			Log.d(TAG, "call datePicker");
			when = CHOSEN_DATE_TIME;
			toggleButtons();
			showDatePickerDialog();
		} else if(v.getId() == R.id.btnASAP) {
			Log.d(TAG, "onClick: btnASAP");
			when = ASAP;
			toggleButtons();
		} else if(v.getId() == R.id.btnFindTaxis) {
			Log.d(TAG, "onClick: btnFindTaxis");
//			Log.d(TAG, getJSONFromUrl("http://davidguyon.olympe.in/test.php").toString());
			checkForm();
		}
	}
	
	private void toggleButtons() {
		if(when == CHOSEN_DATE_TIME) {
			setDateTimeASAP.setBackgroundColor(getResources().getColor(R.color.btnBackgroundColor));
			selectDateTime.setBackgroundColor(getResources().getColor(R.color.btnPressedBackgroundColor));
		} else {
			setDateTimeASAP.setBackgroundColor(getResources().getColor(R.color.btnPressedBackgroundColor));
			selectDateTime.setBackgroundColor(getResources().getColor(R.color.btnBackgroundColor));
		}
	}
	
	private boolean checkForm() {
		if(when == 0) {
			Toast.makeText(this, "Press ASAP button or select a specific date/time", Toast.LENGTH_SHORT).show();
			return false;
		}
		return false;
	}
	
	public JSONObject getJSONFromUrl(String url) {
		InputStream is = null;
		String json = null;
		JSONObject jObj = null;
		
        // Making HTTP request
        try {
            // defaultHttpClient
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
 
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();           
 
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
 
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            json = sb.toString();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }
 
        // try parse the string to a JSON object
        try {
            jObj = new JSONObject(json);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }
 
        // return JSON String
        return jObj;
 
    }

	private void showDatePickerDialog() {
		DatePickerFragment datePicker = new DatePickerFragment();
		datePicker.show(getFragmentManager(), "datePicker");
		datePicker.that = this;
	}

	private void showTimePickerDialog() {
		TimePickerFragment timePicker = new TimePickerFragment();
		timePicker.show(getFragmentManager(), "timePicker");
		timePicker.that = this;
	}

	public void dateAndTimeSelected() {
		Log.d(TAG, String.valueOf(hour) + ":" + String.valueOf(minute) + " - " + String.valueOf(day) + "/" + String.valueOf(month) + "/" + String.valueOf(year));
	}

	public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

		public MakeBooking that;
		private boolean fired = false;

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);

			return new DatePickerDialog(getActivity(), DatePickerDialog.THEME_HOLO_DARK,this, year, month, day);
		}

		@Override
		public void onDateSet(DatePicker view, int year, int month, int day) {
			if(fired) return;

			if(that != null) {
				that.day = day;
				that.month = month + 1;	// Month start from 0 (January = 0)
				that.year = year;
				Log.d(TAG, "call timePicker");
				that.showTimePickerDialog();
			}

			fired = true;
		}
	}

	public static class TimePickerFragment extends DialogFragment implements OnTimeSetListener {

		public MakeBooking that;
		private boolean fired = false;

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current time as the default values for the picker
			final Calendar c = Calendar.getInstance();
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);

			return new TimePickerDialog(getActivity(), TimePickerDialog.THEME_HOLO_DARK, this, hour, minute, DateFormat.is24HourFormat(getActivity()));
		}

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			if(fired) return;

			if(that != null) {
				that.hour = hourOfDay;
				that.minute = minute;
				that.dateAndTimeSelected();
			}

			fired = true;
		}
	}

	@Override
	public void onMapLongClick(final LatLng point) {
		
		/*
		 * Find the closest address with the given latitude and longitude
		 * using the reverse Geocoder by Google
		 */
		StringBuilder mSb = new StringBuilder();
		List<Address> list = null;
		Log.d(TAG, "Reverse Geocoder..");
		try {
			list = mGeocoder.getFromLocation(point.latitude, point.longitude, 1);  
			Log.d(TAG, "..returns : " + list.get(0).toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*
		 * If a problem occurs with the reverse Geocoder
		 * This method is leaved
		 */
		if(list == null) return;
		
		/*
		 * Build a String containing the full address
		 * from an Address object
		 */
		Address address = list.get(0);
		mSb.setLength(0);
		final int addressLineSize = address.getMaxAddressLineIndex();
		for (int i = 0; i < addressLineSize; i++) {
			mSb.append(address.getAddressLine(i));
			if (i != addressLineSize - 1) {
				mSb.append(", ");
			}
		}
		final String street = mSb.toString();
		
		AlertDialog dialog = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK).create();
		dialog.setTitle("Flag type");
		dialog.setMessage("Is it a departure or an arrival flag?");
		dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Departure flag", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				fromMarker.setPosition(point);
				fromMarker.setVisible(true);
				fromStreet.setText(street);
			}
		});
		dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Arrival flag", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				toMarker.setPosition(point);
				toMarker.setVisible(true);
				toStreet.setText(street);
			}
		});
		dialog.show();
	}

	@Override
	public void onMapClick(LatLng point) {
		Toast.makeText(this, "Normal click: " + point.toString(), Toast.LENGTH_SHORT).show();
	}

	private class AutoCompleteAdapter extends ArrayAdapter<Address> implements Filterable {
		
		private LayoutInflater mInflater;
		private StringBuilder mSb = new StringBuilder();
	 
		public AutoCompleteAdapter(final Context context) {
			super(context, -1);
			mInflater = LayoutInflater.from(context);
		}
		
		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			final TextView tv;
			if (convertView != null) {
				tv = (TextView) convertView;
			} else {
				tv = (TextView) mInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
			}
	 
			tv.setText(createFormattedAddressFromAddress(getItem(position)));
			return tv;
		}
	 
		private String createFormattedAddressFromAddress(final Address address) {
			mSb.setLength(0);
			final int addressLineSize = address.getMaxAddressLineIndex();
			for (int i = 0; i < addressLineSize; i++) {
				mSb.append(address.getAddressLine(i));
				if (i != addressLineSize - 1) {
					mSb.append(", ");
				}
			}
			return mSb.toString();
		}
		
		@Override
		public Filter getFilter() {
			Filter myFilter = new Filter() {
				
				protected FilterResults performFiltering(final CharSequence constraint) {
					List<Address> addressList = null;
					
					if(constraint != null) {
						try {
							addressList = mGeocoder.getFromLocationName((String) constraint, 5);
						} catch (IOException e) {}
					}
					
					if(addressList == null) {
						addressList = new ArrayList<Address>();
					}
	 
					final FilterResults filterResults = new FilterResults();
					filterResults.values = addressList;
					filterResults.count = addressList.size();
	 
					return filterResults;
				}
				
				@SuppressWarnings ("unchecked")
				@Override
				protected void publishResults(final CharSequence contraint, final FilterResults results) {
					clear();
					
					for (Address address : (List<Address>) results.values) {
						add(address);
					}
					
					if(results.count > 0) {
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
				}
				
				@Override public CharSequence convertResultToString(final Object resultValue) {
					return resultValue == null ? "" : createFormattedAddressFromAddress((Address) resultValue);
				}
			};
			
			return myFilter;
		}
	}
}
