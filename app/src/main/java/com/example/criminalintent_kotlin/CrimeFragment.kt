package com.example.criminalintent_kotlin

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.icu.text.MessageFormat.format
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.text.format.DateFormat.format
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.Observer
import java.lang.String.format


import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val DATE_FORMAT = "EEE,MMM,dd"

class CrimeFragment: Fragment(), DatePickerFragment.Callbacks {
    private lateinit var crime: Crime

    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }

    val pickContact = registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri ->
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val cursor = contactUri?.let {
            requireActivity().contentResolver.query (it, queryFields, null, null, null)
        }
        cursor?.use {
            // Verify cursor contains at least one result
            if (it.count > 0) {
                // Pull out first column of the first row of data, that's our suspect name
                it.moveToFirst()
                val suspect = it.getString(0)
                crime.suspect = suspect
                crimeDetailViewModel.saveCrime(crime)
                suspectButton.text = suspect
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime,container,false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox= view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button



        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner, Observer { crime ->
                crime?.let {
                    this.crime = crime
                    updateUI()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?,
                                           start: Int,
                                           count: Int,
                                           after: Int) {
                //Left empty
            }

            override fun onTextChanged(sequence: CharSequence?,
                                       start: Int,
                                       count: Int,
                                       after: Int) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                //Left empty
            }
        }

        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener{_,isChecked ->
            crime.isSolved = isChecked}
        }


        dateButton.setOnClickListener{
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject)
                )
            }.also {
                intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {

                //Changed inviking method startActivityOnResult() on pickContact variable
                //due the deprecation of method above. The variable pickContact duplicates a function
                //startActivityForResult(pickContactIntent, REQUEST_CONTACT)
                pickContact.launch(null)


                val packageManager: PackageManager = requireActivity().packageManager
                val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(pickContactIntent,
                PackageManager.MATCH_DEFAULT_ONLY)

                if (resolvedActivity == null){
                    isEnabled = false
                }


            }
        }




    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDateSelected(date: Date){
        crime.date = date
        updateUI()
    }

    private fun updateUI(){
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }

        if(crime.suspect.isNotEmpty()){
            suspectButton.text = crime.suspect
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when{
            requestCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                // Select for which fields the request must return value
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                // processing the request - contactUri is similar to the syntax "where"
                val cursor = requireActivity().contentResolver.query(contactUri!!,
                    queryFields,
                    null,
                    null,
                    null)
                cursor?.use {
                    //Verify cursor contains at least one result
                    if(it.count ==0){
                        return
                    }

                    //the first column of the first line of data - the name you suspect
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect= suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
            }
        }
    }

    private fun getCrimeReport():String {
        val solvedString = if(crime.isSolved){
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT,crime.date).toString()

        var suspect = if(crime.suspect.isBlank()){
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect,crime.suspect)
        }
        return getString(R.string.crime_report,crime.title,dateString,solvedString,suspect)
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply{
                putSerializable(ARG_CRIME_ID,crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }


}