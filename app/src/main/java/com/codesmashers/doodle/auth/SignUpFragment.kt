package com.codesmashers.doodle.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.codesmashers.doodle.MainActivity
import com.codesmashers.doodle.R
import kotlinx.android.synthetic.main.fragment_sign_up.*

/** [SignUpFragment] is called to Sign-Up users by authenticating with Firebase Authentication. */
class SignUpFragment : Fragment() {

    lateinit var frameView: FrameLayout
    private val mAuth = Firebase.auth
    private lateinit var prefs: SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        frameView = activity?.findViewById(R.id.auth_view)!!
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireContext().getSharedPreferences(
            getString(R.string.packageName), Context.MODE_PRIVATE
        )

        register_loading.setMinAndMaxFrame(3, 50)
        register_loading.visibility = View.INVISIBLE

        /** [SignInFragment] is called to switch to Sign In users. */
        sign_up_signin.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(frameView.id, SignInFragment())
                ?.commit()
        }

        /** Called when user press on Sign Up Register button.
         *  First the fields are validated and then authenticated with Firebase and thereafter User is Signed Up. */
        register_btn.setOnClickListener {
            val email = email_register.text.toString().trim()
            val password = pass_register.text.toString().trim()

            if (TextUtils.isEmpty(email)) {
                email_register.error = getString(R.string.enterEmail)
            } else if (TextUtils.isEmpty(password) || password.length < 5) {
                pass_register.error = getString(R.string.passwordLengthSmall)
            } else if(!register_checkPrivacy.isChecked){
                register_checkPrivacy.error = "This is must."
            } else {
                register_loading.elevation = 8.0f
                register_loading.visibility = View.VISIBLE
                register_loading.playAnimation()

                register_btn.isEnabled = false
                register_btn.isClickable = false
                mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task: Task<AuthResult> ->
                        if (task.isSuccessful) {
                            // Sign Up successful. Store the USERNAME and UUID in local preference and then user is Signed Up.
                            prefs.edit().putString(
                                getString(R.string.userId),
                                FirebaseAuth.getInstance().currentUser?.uid.toString()
                            ).apply();
                            prefs.edit().putString(
                                getString(R.string.userName),
                                name_register.text.toString().trim()
                            ).apply();
                            startActivity(Intent(context, MainActivity::class.java))
                            register_loading.cancelAnimation()
                            activity?.finish()

                        } else {
                            register_loading.cancelAnimation()
                            register_loading.visibility = View.INVISIBLE
                            // Sign Up failed. Error message displayed.
                            Toast.makeText(
                                context, getString(R.string.authFailed),
                                Toast.LENGTH_SHORT
                            ).show()
                            register_btn.isEnabled = true
                            register_btn.isClickable = true

                        }
                    }
            }
        }
    }
}
