package jp.techacademy.mohri.shuto.qa_app

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*

/**
 * ログイン画面.
 *
 * ログインまたはアカウント作成を行う.
 */
class LoginActivity : AppCompatActivity() {
    /**
     * クラス名.
     */
    private val CLASS_NAME = "LoginActivity"
    /**
     * FirebaseAuth.
     */
    private lateinit var mAuth: FirebaseAuth
    /**
     * アカウント作成処理の完了を受け取るリスナー.
     */
    private lateinit var mCreateAccountListener: OnCompleteListener<AuthResult>
    /**
     * ログイン処理の完了を受け取るリスナー.
     */
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    /**
     * DatabaseReference(Firebaseデータベースからデータを読み取る為)
     */
    private lateinit var mDataBaseReference: DatabaseReference
    /**
     * アカウント作成時フラグ.
     */
    private var mIsCreateAccount = false

    /**
     * Preference Key.
     */
    private val PREFERENCE_KEY_ACCOUNT_NAME =
        "jp.techacademy.mohri.shuto.qa_app.PREFERENCE_KEY_ACCOUNT_NAME"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // インスタンス取得.
        mDataBaseReference = FirebaseDatabase.getInstance().reference
        mAuth = FirebaseAuth.getInstance()

        // Firebase関連リスナー作成.
        createListeners()

        // アカウント作成ボタンクリック処理を設定.
        btCreateAccount.setOnClickListener { view ->
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                view.windowToken,
                InputMethodManager.HIDE_IMPLICIT_ONLY
            )

            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val name = etName.text.toString()

            // 入力値チェック.
            if (email.isNotEmpty() && password.length >= 6 && name.isNotEmpty()) {
                // ログイン時に表示名を保存する.
                mIsCreateAccount = true

                createAccount(email, password)
            } else {
                // エラー表示.
                Snackbar.make(
                    view,
                    R.string.snackbar_error_input,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        // ログインボタンクリック処理を設定.
        btLogin.setOnClickListener { view ->
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                view.windowToken,
                InputMethodManager.HIDE_IMPLICIT_ONLY
            )

            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            // 入力値チェック.
            if (email.isNotEmpty() && password.length >= 6) {
                // 表示名は保存済みの為フラグを落とす.
                mIsCreateAccount = false

                login(email, password)
            } else {
                // エラー表示.
                Snackbar.make(
                    view,
                    R.string.snackbar_error_input,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        // タイトルバーのタイトルを設定.
        title = "ログイン"
    }


    /**
     * Firebaseアカウント作成.
     */
    private fun createAccount(email: String, password: String) {
        // プログレスバー表示.
        pbProgressBar.visibility = View.VISIBLE

        // TODO これ毎回リスナ追加して平気なん?
        // アカウント作成(タスク完了時に呼び出されるリスナを追加).
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(mCreateAccountListener)
    }


    /**
     * Firebaseにログイン.
     */
    private fun login(email: String, password: String) {
        // プログレスバー表示.
        pbProgressBar.visibility = View.VISIBLE

        // ログイン(タスク完了時に呼び出されるリスナを追加).
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(mLoginListener)
    }


    /**
     * 表示名保存.
     */
    private fun saveName(name: String) {
        // Preferenceに保存.
        // TODO 非推奨になってる
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        editor.putString(PREFERENCE_KEY_ACCOUNT_NAME, name)
        editor.apply()
    }


    /**
     *
     */
    private fun createListeners() {

        //アカウント作成処理リスナー.
        mCreateAccountListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // 成功時.
                val email = etEmail.text.toString()
                val password = etPassword.text.toString()
                // Firebaseログイン.
                login(email, password)
            } else {
                // 失敗時.
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(
                    view,
                    R.string.snackbar_error_create_account,
                    Snackbar.LENGTH_SHORT
                ).show()
                // プログレスバー非表示.
                pbProgressBar.visibility = View.GONE
            }
        }


        // ログイン処理リスナー.
        mLoginListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // 成功時.
                val user = mAuth.currentUser
                val userRef = mDataBaseReference.child(USERS_PATH).child(user!!.uid)

                // TODO アカウント作成されてない場合.??
                if (mIsCreateAccount) {
                    val name = etName.text.toString()
                    val data = HashMap<String, String>()
                    // keyをnameに設定.
                    data["name"] = name
                    userRef.setValue(data)
                    // 表示名をPreferenceに保存.
                    saveName(name)
                } else {
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val data = snapshot.value as Map<*, *>?
                            saveName(data!!["name"] as String)
                        }

                        override fun onCancelled(firebaseError: DatabaseError) {}
                    })
                }

                // プログレスバーを非表示.
                pbProgressBar.visibility = View.GONE
                // アクティビティ終了.
                finish()

            } else {
                // 失敗時.
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(
                    view,
                    R.string.snackbar_error_login,
                    Snackbar.LENGTH_SHORT
                ).show()

                // プログレスバー非表示.
                pbProgressBar.visibility = View.GONE
            }
        }

    }


}