// Student Name: Ian Oduor Omung'a
// Student ID: S2110910
package com.omunga_ian.S2110910;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ianomunga.testweather.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.text.Editable;
import android.text.TextWatcher;
import java.util.Arrays;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class MainActivity extends AppCompatActivity {

    String CITY;
    String API = "bd5e378503939ddaee76f12ad7a97608";

    private String[] cities = {"Glasgow", "London", "New York", "Oman", "Port Louis", "Bangladesh"};
    private int currentCityIndex = 0;

    TextView addressTxt, updated_atTxt, statusTxt, tempTxt, temp_minTxt, temp_maxTxt, sunriseTxt,
            sunsetTxt, windTxt, pressureTxt, humidityTxt;

    // New member variables to hold forecast data
    String nextDayWeatherDescription, dayAfterNextWeatherDescription;
    String nextDayMinTemp, nextDayMaxTemp, nextDaySunrise, nextDaySunset, nextDayWindSpeed, nextDayPressure, nextDayHumidity;
    String dayAfterNextMinTemp, dayAfterNextMaxTemp, dayAfterNextSunrise, dayAfterNextSunset, dayAfterNextWindSpeed, dayAfterNextPressure, dayAfterNextHumidity;

    Button buttonBack, buttonForward;

    // New buttons for toggling between weather views
    Button buttonToday, buttonTomorrow, buttonAfterTomorrow;
    EditText enterCity;
    Button search;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addressTxt = findViewById(R.id.address);
        updated_atTxt = findViewById(R.id.updated_at);
        statusTxt = findViewById(R.id.status);
        tempTxt = findViewById(R.id.temp);
        temp_minTxt = findViewById(R.id.temp_min);
        temp_maxTxt = findViewById(R.id.temp_max);
        sunriseTxt = findViewById(R.id.sunrise);
        sunsetTxt = findViewById(R.id.sunset);
        windTxt = findViewById(R.id.wind);
        pressureTxt = findViewById(R.id.pressure);
        humidityTxt = findViewById(R.id.humidity);

        enterCity = findViewById(R.id.enterCity);
        search = findViewById(R.id.search);

        buttonBack = findViewById(R.id.button_back);
        buttonForward = findViewById(R.id.button_forward);

        enterCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No need to implement this for now
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No need to implement this for now
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Reset the current index if the user clears the search text
                if (s.toString().isEmpty()) {
                    currentCityIndex = 0;
                }
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchQuery = enterCity.getText().toString();
                int cityIndex = Arrays.asList(cities).indexOf(searchQuery);
                if(cityIndex != -1) { // If the city is in the list
                    currentCityIndex = cityIndex;
                } else {
                    // Not found, show error or do nothing
                    // Optionally, add the city to the list if needed
                }
                fetchWeatherDataForCity();
            }
        });


        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateCities(false); // Navigate to previous city
            }
        });

        buttonForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateCities(true); // Navigate to next city
            }
        });

        // Initialize new buttons for BBC forecast data
        buttonToday = findViewById(R.id.button_today);
        buttonTomorrow = findViewById(R.id.button_tomorrow);
        buttonAfterTomorrow = findViewById(R.id.button_aftertomorrow);

        // Set their onClickListeners
        buttonToday.setOnClickListener(view -> displayTodayWeather());
        buttonTomorrow.setOnClickListener(view -> displayTomorrowWeather());
        buttonAfterTomorrow.setOnClickListener(view -> displayAfterTomorrowWeather());

        // Fetch weather data for the first city on app launch from both data sources
        fetchWeatherDataForCity();
        fetchBBCWeatherForecastData();
    }

    private void fetchWeatherDataForCity() {
        String city = cities[currentCityIndex];
        CITY = city; // Assign the selected city to the CITY variable
        new weatherTask().execute(city);
    }

    private void navigateCities(boolean isForward) {
        if (isForward) {
            currentCityIndex = (currentCityIndex + 1) % cities.length;
        } else {
            currentCityIndex = (currentCityIndex - 1 + cities.length) % cities.length;
        }
        fetchWeatherDataForCity();
    }

    private void fetchBBCWeatherForecastData() {
        String bbcUrlBase = "https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/";
        String cityCode = getCityCode(CITY); // Implement this method to return city code based on CITY
        new FetchForecastTask().execute(bbcUrlBase + cityCode);
    }
    private String getCityCode(String cityName) {
        if (cityName == null) {
            return "2648579"; // Return an empty string or default to The Glasgow Campus
        }
        switch (cityName) {
            case "Glasgow":
                return "2648579";
            case "London":
                return "2643743";
            case "New York":
                return "5128581";
            case "Oman":
                return "287286";
            case "Port Louis":
                return "934154";
            case "Bangladesh":
                return "1185241";
            default:
                return ""; // Handle error
        }
    }

    // AsyncTask to fetch and parse BBC RSS Feed weather data
    class FetchForecastTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(url.openConnection().getInputStream(), "UTF-8");

                int eventType = xpp.getEventType();
                int itemCount = 0;

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equalsIgnoreCase("item")) {
                            itemCount++;
                        } else if (itemCount == 2 && xpp.getName().equalsIgnoreCase("title")) {
                            String titleText = xpp.nextText();
                            String[] titleParts = titleText.split(",");
                            if (titleParts.length >= 3) {
                                nextDayWeatherDescription = titleParts[0].split(":")[1].trim();
                                nextDayMinTemp = titleParts[1].split(":")[1].trim().split("Â°C")[0].trim();
                                nextDayMaxTemp = titleParts[2].split(":")[1].trim().split("Â°C")[0].trim();
                            }
                        } else if (itemCount == 2 && xpp.getName().equalsIgnoreCase("description")) {
                            String descriptionText = xpp.nextText();
                            String[] descriptionParts = descriptionText.split(",");
                            if (descriptionParts.length >= 11) {
                                nextDaySunrise = descriptionParts[9].split(":")[1].trim();
                                nextDaySunset = descriptionParts[10].split(":")[1].trim();
                                nextDayWindSpeed = descriptionParts[3].split(":")[1].trim().split("mph")[0].trim();
                                nextDayPressure = descriptionParts[5].split(":")[1].trim().split("mb")[0].trim();
                                nextDayHumidity = descriptionParts[6].split(":")[1].trim().split("%")[0].trim();
                            }
                        } else if (itemCount == 3 && xpp.getName().equalsIgnoreCase("title")) {
                            String titleText = xpp.nextText();
                            String[] titleParts = titleText.split(",");
                            if (titleParts.length >= 3) {
                                dayAfterNextWeatherDescription = titleParts[0].split(":")[1].trim();
                                dayAfterNextMinTemp = titleParts[1].split(":")[1].trim().split("Â°C")[0].trim();
                                dayAfterNextMaxTemp = titleParts[2].split(":")[1].trim().split("Â°C")[0].trim();
                            }
                        } else if (itemCount == 3 && xpp.getName().equalsIgnoreCase("description")) {
                            String descriptionText = xpp.nextText();
                            String[] descriptionParts = descriptionText.split(",");
                            if (descriptionParts.length >= 11) {
                                dayAfterNextSunrise = descriptionParts[9].split(":")[1].trim();
                                dayAfterNextSunset = descriptionParts[10].split(":")[1].trim();
                                dayAfterNextWindSpeed = descriptionParts[3].split(":")[1].trim().split("mph")[0].trim();
                                dayAfterNextPressure = descriptionParts[5].split(":")[1].trim().split("mb")[0].trim();
                                dayAfterNextHumidity = descriptionParts[6].split(":")[1].trim().split("%")[0].trim();
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            displayTodayWeather(); // Display today's weather by default
        }
    }


    private void displayTodayWeather() {
        // Making sure this data is the most current from the OpenWeatherAPI
        fetchWeatherDataForCity();
    }


    private void displayTomorrowWeather() {
        statusTxt.setText(nextDayWeatherDescription);
        temp_minTxt.setText("Min Temp: " + nextDayMinTemp + "°C");
        temp_maxTxt.setText("Max Temp: " + nextDayMaxTemp + "°C");
        sunriseTxt.setText(nextDaySunrise);
        sunsetTxt.setText(nextDaySunset);
        windTxt.setText(nextDayWindSpeed + "mph");
        pressureTxt.setText(nextDayPressure + "mb");
        humidityTxt.setText(nextDayHumidity + "%");
    }

    private void displayAfterTomorrowWeather() {
        statusTxt.setText(dayAfterNextWeatherDescription);
        temp_minTxt.setText("Min Temp: " + dayAfterNextMinTemp + "°C");
        temp_maxTxt.setText("Max Temp: " + dayAfterNextMaxTemp + "°C");
        sunriseTxt.setText(dayAfterNextSunrise);
        sunsetTxt.setText(dayAfterNextSunset);
        windTxt.setText(dayAfterNextWindSpeed + "mph");
        pressureTxt.setText(dayAfterNextPressure + "mb");
        humidityTxt.setText(dayAfterNextHumidity + "%");
    }


    /** @noinspection deprecation*/
    class weatherTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            /* Showing the ProgressBar, Making the main design GONE */
            findViewById(R.id.loader).setVisibility(View.VISIBLE);
            findViewById(R.id.mainContainer).setVisibility(View.GONE);
            findViewById(R.id.errorText).setVisibility(View.GONE);
        }

        protected String doInBackground(String... args) {
            String response = HttpRequest.executeGet("https://api.openweathermap.org/data/2.5/weather?q=" + args[0] + "&units=metric&appid=" + API);
            return response;
        }

        @Override
        protected void onPostExecute(String result) {

            try {
                JSONObject jsonObj = new JSONObject(result);
                JSONObject main = jsonObj.getJSONObject("main");
                JSONObject sys = jsonObj.getJSONObject("sys");
                JSONObject wind = jsonObj.getJSONObject("wind");
                JSONObject weather = jsonObj.getJSONArray("weather").getJSONObject(0);

                Long updatedAt = jsonObj.getLong("dt");
                String updatedAtText = "Updated on: " + new SimpleDateFormat("EEEE dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(new Date(updatedAt * 1000));
                String temp = main.getString("temp") + "°C";
                String tempMin = "Min Temp: " + main.getString("temp_min") + "°C";
                String tempMax = "Max Temp: " + main.getString("temp_max") + "°C";
                String pressure = main.getString("pressure");
                String humidity = main.getString("humidity");

                Long sunrise = sys.getLong("sunrise");
                Long sunset = sys.getLong("sunset");
                String windSpeed = wind.getString("speed");
                String weatherDescription = weather.getString("description");

                String address = jsonObj.getString("name") + ", " + sys.getString("country");


                /* Populating extracted data into our views */
                addressTxt.setText(address);
                updated_atTxt.setText(updatedAtText);
                statusTxt.setText(weatherDescription.toUpperCase());
                tempTxt.setText(temp);
                temp_minTxt.setText(tempMin);
                temp_maxTxt.setText(tempMax);
                sunriseTxt.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date(sunrise * 1000)));
                sunsetTxt.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date(sunset * 1000)));
                windTxt.setText(windSpeed);
                pressureTxt.setText(pressure);
                humidityTxt.setText(humidity);

                /* Views populated, Hiding the loader, Showing the main design */
                findViewById(R.id.loader).setVisibility(View.GONE);
                findViewById(R.id.mainContainer).setVisibility(View.VISIBLE);


            } catch (JSONException e) {
                findViewById(R.id.loader).setVisibility(View.GONE);
                findViewById(R.id.errorText).setVisibility(View.VISIBLE);
            }

        }
    }
}