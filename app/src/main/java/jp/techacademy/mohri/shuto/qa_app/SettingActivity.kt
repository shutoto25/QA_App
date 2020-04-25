package jp.techacademy.mohri.shuto.qa_app

import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_setting.*

/**
 * 設定画面.
 */
class SettingActivity : AppCompatActivity() {
    /**
     * クラス名.
     */
    private val CLASS_NAME = "MainActivity"
    /**
     *
     */
    private lateinit var mDataBaseReference: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "$CLASS_NAME.onCreate")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // Preferenceから表示名を取得してEditTextに反映させる
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sharedPreferences.getString(PREFERENCE_KEY_ACCOUNT_NAME, "")
        etDisplayName.setText(name)
        mDataBaseReference = FirebaseDatabase.getInstance().reference

        // リスナ設定.
        setListeners()

        // タイトルバーのタイトルを設定.
        title = getString(R.string.title_bar_setting)
    }


    /**
     * リスナ設定.
     */
    private fun setListeners() {
        Log.d(TAG, "$CLASS_NAME.setListeners")

        // 変更ボタンタップ時処理.
        btChangeName.setOnClickListener { view ->
            // キーボードが出ていた場合は閉じる
            val im = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            // ログイン済みのユーザーを取得
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていない場合は何もしない
                Snackbar.make(
                    view,
                    getString(R.string.snackbar_error_not_login),
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                // 変更した表示名をFirebaseに保存する
                val name = etDisplayName.text.toString()
                val userRef = mDataBaseReference.child(USERS_PATH).child(user.uid)
                val data = HashMap<String, String>()
                data["name"] = name
                userRef.setValue(data)

                // 変更した表示名をPreferenceに保存する
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val editor = sharedPreferences.edit()
                editor.putString(PREFERENCE_KEY_ACCOUNT_NAME, name)
                editor.apply()

                Snackbar.make(
                    view,
                    getString(R.string.snackbar_success_change_name),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }


        // ログアウトボタンタップ時処理.
        btLogout.setOnClickListener { view ->
            FirebaseAuth.getInstance().signOut()
            etDisplayName.setText("")
            Snackbar.make(view, getString(R.string.snackbar_logout), Snackbar.LENGTH_LONG).show()
        }
    }
}