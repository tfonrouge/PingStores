package com.fonrouge.pingstores

import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Find the password EditText
            // Find the password EditText
            val etpPassword: EditTextPreference? = preferenceManager.findPreference("password")

            etpPassword?.setOnBindEditTextListener(EditTextPreference.OnBindEditTextListener { editText -> // Set keyboard layout and some behaviours of the field
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                // Replace -> android:singleLine="true"
                // Not needed for password field, or set it before setTransformationMethod
                // otherwise the password will not be hidden
                //editText.setSingleLine(true);

                // Replace -> android:inputType="textPassword"
                // For hiding text
                editText.transformationMethod = PasswordTransformationMethod.getInstance()

                // Replace -> android:selectAllOnFocus="true"
                // On password field, you cannot make a partial selection with .setSelection(start, stop)
                editText.selectAll()

                // Replace -> android:maxLength="99"
                editText.filters = arrayOf<InputFilter>(LengthFilter(99))
            })
        }


    }
}