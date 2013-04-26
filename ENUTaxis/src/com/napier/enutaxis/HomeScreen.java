package com.napier.enutaxis;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class HomeScreen extends Activity implements OnClickListener {

	private static final String TAG = "HOME_SCREEN";
	private static final boolean DEBUG_SKIP_HOME_SCREEN = false;

	SharedPreferences pref;

	TextView title;
	TextView napierName;
	TextView univName;
	TextView question;

	EditText firstName;
	EditText lastName;
//	EditText email;
	EditText phone;
	EditText matNb;

	Button bookingBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "HomeScreen is starting");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.home_screen);
		
		title 		= (TextView) findViewById(R.id.homeScreenTitle);
		napierName	= (TextView) findViewById(R.id.napierName);
		univName 	= (TextView) findViewById(R.id.universityName);
		question 	= (TextView) findViewById(R.id.question);

		firstName	= (EditText) findViewById(R.id.firstName);
		lastName	= (EditText) findViewById(R.id.lastName);
//		email		= (EditText) findViewById(R.id.email);
		phone		= (EditText) findViewById(R.id.phone);
		matNb		= (EditText) findViewById(R.id.matriculation);

		bookingBtn	= (Button)	 findViewById(R.id.makeBooking);
		bookingBtn.setOnClickListener(this);

		/*
		 * Set Existence font to the home screen title
		 * with the correct color and a size of 56
		 */
		Typeface existence = Typeface.createFromAsset(getAssets(), "fonts/Existence-Light.otf");
		title.setTypeface(existence, Typeface.BOLD);
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 70);
		title.setTextColor(Color.WHITE);

		napierName.setTypeface(existence);
		napierName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
		napierName.setTextColor(Color.WHITE);

		univName.setTypeface(existence);
		univName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
		univName.setTextColor(Color.WHITE);

		question.setTypeface(existence);
		question.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
		question.setTextColor(Color.WHITE);

		/*
		 * Get/Set shared preferences
		 */
		preFilledFormIfExistingPreferences();
		
		if(DEBUG_SKIP_HOME_SCREEN && checkEnteredValuesAndSavedPreferences())
			startActivity(new Intent(this, MakeBooking.class));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home_screen_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.clear_preferences:
			clearPreferences();
			return true;
		case R.id.help:
			showHelp();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showHelp() {
		Toast.makeText(this, "Coming soon...", Toast.LENGTH_LONG).show();
	}

	/**
	 * Get the shared preferences
	 * If there are preferences, it fills
	 * all the EditText field
	 */
	private void preFilledFormIfExistingPreferences() {
		pref = getSharedPreferences("PreFilledForm", MODE_PRIVATE);

		if(pref.contains("firstName")) {
			String _firstName = pref.getString("firstName", "");
			firstName.setText(_firstName);
		}

		if(pref.contains("lastName")) {
			String _lastName = pref.getString("lastName", "");
			lastName.setText(_lastName);
		}

//		if(pref.contains("email")) {
//			String _email = pref.getString("email", "");
//			email.setText(_email);
//		}

		if(pref.contains("phone")) {
			String _phone = pref.getString("phone", "");
			phone.setText(_phone);
		}

		if(pref.contains("matriculationNumber")) {
			String _matNb = pref.getString("matriculationNumber", "");
			matNb.setText(_matNb);
		}
	}

	private void clearPreferences() {
		Editor editPref = pref.edit();
		editPref.clear();
		editPref.commit();

		firstName.setText("");
		lastName.setText("");
//		email.setText("");
		phone.setText("");
		matNb.setText("");
	}

	private boolean checkEnteredValuesAndSavedPreferences() {
		String _firstName	= firstName.getText().toString();
		String _lastName	=  lastName.getText().toString();
//		String _email		= 	  email.getText().toString();
		String _phone		= 	  phone.getText().toString();
		String _matNb		= 	  matNb.getText().toString();

		// Check if each EditText field is filled 
		if(_firstName.equals("") || _lastName.equals("") || _phone.equals("") || _matNb.equals("")) {
			Toast.makeText(this, "You need to fill all the form!", Toast.LENGTH_SHORT).show();
			return false;
		}

		Editor editPref = pref.edit();


		// Check the first name
		if(doesNotContainNumbers(_firstName)) {
			editPref.putString("firstName", _firstName);
		} else {
			Toast.makeText(this, "Your first name contains number(s) O_o", Toast.LENGTH_SHORT).show();
			return false;
		}

		// Check the first name
		if(doesNotContainNumbers(_lastName)) {
			editPref.putString("lastName", _lastName);
		} else {
			Toast.makeText(this, "Your last name contains number(s) O_o", Toast.LENGTH_SHORT).show();
			return false;
		}

		// Check the email address
//		if(isValidEmail(_email)) {
//			editPref.putString("email", _email);
//		} else {
//			Toast.makeText(this, "Your email address is not correct", Toast.LENGTH_SHORT).show();
//			return false;
//		}

		// Check phone number
		if(isValidPhoneNumber(_phone)) {
			editPref.putString("phone", _phone);
		} else {
			Toast.makeText(this, "Your phone number is not correct", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		// Check matriculation number
		if(isValidMatriculationNumber(_matNb)) {
			editPref.putString("matriculationNumber", _matNb);
		} else {
			Toast.makeText(this, "Your matriculation number is not correct", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		editPref.commit();
		return true;
	}

	/**
	 * Check if the name does not contain number in it
	 * @param Name
	 * @return true if there is no number on the name
	 */
	private boolean doesNotContainNumbers(String word) {
		if(word != null){
			for(char c : word.toCharArray()){
				if(Character.isDigit(c)){
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Check if the email address is correct
	 * @param Email address
	 * @return true if it's a correct email address
	 */
	private boolean isValidEmail(String email)
	{
		String emailRegex ="^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		if(email.matches(emailRegex))
			return true;
		return false;
	}

	/**
	 * Check if the phone number contains 10 or 11 digits
	 * and check if there is no letters in it
	 * @param Phone number
	 * @return true if it's a correct phone number
	 */
	private boolean isValidPhoneNumber(String number) {
		if(number.length() < 10 || number.length() > 11) return false;

		for(char c : number.toCharArray()){
			if(!Character.isDigit(c)){
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if the matriculation number contains 8 digits
	 * and check if there is no letters in it
	 * @param Matriculation number
	 * @return true if it's a correct matriculation number
	 */
	private boolean isValidMatriculationNumber(String number) {
		if(number.length() != 8) return false;

		for(char c : number.toCharArray()){
			if(!Character.isDigit(c)){
				return false;
			}
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.makeBooking) {
			if(checkEnteredValuesAndSavedPreferences()) {
//				Toast.makeText(this, "Well done, you arrived to filled the form ;)", Toast.LENGTH_LONG).show();
				startActivity(new Intent(this, MakeBooking.class));
			}
		}
	}

}
