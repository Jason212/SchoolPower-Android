/**
 * Copyright (C) 2017 Gustav Wang
 */

package com.carbonylgroup.schoolpower.activities

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.EditText

import com.carbonylgroup.schoolpower.R
import com.carbonylgroup.schoolpower.classes.Utils.Utils
import com.carbonylgroup.schoolpower.classes.Utils.postData


class LoginActivity : Activity() {

    private var utils: Utils? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        setTheme(R.style.Design)
        super.onCreate(savedInstanceState)
        checkIfLoggedIn()
        setContentView(R.layout.login_content)

        initDialog()
        initValue()

        utils!!.checkUpdate()
    }

    private fun checkIfLoggedIn() {

        val sharedPreferences = getSharedPreferences(getString(R.string.accountData), Activity.MODE_PRIVATE)
        if (sharedPreferences.getBoolean(getString(R.string.loggedIn), false))
            startMainActivity()
    }

    private fun initDialog() {

        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.only_alert))
        builder.setTitle(getString(R.string.notification))
        builder.setPositiveButton(getString(R.string.i_understand), null)
        builder.create().show()
    }

    private fun initValue() {

        utils = Utils(this)

        val input_username : EditText = findViewById(R.id.input_username)
        val input_password : EditText = findViewById(R.id.input_password)

        findViewById<View>(R.id.login_fab).setOnClickListener {
            saveUserId(input_username.text.toString())
            loginAction(input_username.text.toString(), input_password.text.toString())
        }
    }

    private fun saveUserId(stringId: String) {

        val spEditor = getSharedPreferences(getString(R.string.accountData), Activity.MODE_PRIVATE).edit()
        spEditor.putString(getString(R.string.user_id), stringId)
        spEditor.apply()
    }

    fun loginAction(username: String, password: String) {

        val progressDialog = ProgressDialog(this@LoginActivity)
        progressDialog.isIndeterminate = true
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setMessage(getString(R.string.authenticating))
        progressDialog.show()

        Thread(postData(
                getString(R.string.postURL),
                getString(R.string.username_equals) + username + "&" + getString(R.string.password_equals) + password,
                object : Handler() {
                    override fun handleMessage(msg: Message) {

                        progressDialog.dismiss()
                        val strMessage = msg.obj.toString()
                        val messages = strMessage.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (strMessage.contains(getString(R.string.error_wrong_password)))
                            utils!!.showSnackBar(this@LoginActivity, findViewById(R.id.login_coordinate_layout), getString(R.string.wrong_password), true)
                        else if (strMessage.contains(getString(R.string.json_begin)) || messages[2] == "[]") {

                            val spEditor = getSharedPreferences(getString(R.string.accountData), Activity.MODE_PRIVATE).edit()
                            spEditor.putString(getString(R.string.usernameKEY), username)
                            spEditor.putString(getString(R.string.passwordKEY), password)
                            spEditor.putBoolean(getString(R.string.loggedIn), true)
                            spEditor.putString(getString(R.string.student_name), messages[1])
                            spEditor.apply()

                            utils!!.saveDataJson(messages[2])
                            utils!!.saveHistoryGrade(utils!!.parseJsonResult(messages[2]))

                            startMainActivity()

                        } else utils!!.showSnackBar(this@LoginActivity, findViewById(R.id.login_coordinate_layout), getString(R.string.no_connection), true)
                    }
                })).start()

    }

    private fun startMainActivity() {

        startActivity(Intent(application, MainActivity::class.java))
        this@LoginActivity.finish()
    }
}
