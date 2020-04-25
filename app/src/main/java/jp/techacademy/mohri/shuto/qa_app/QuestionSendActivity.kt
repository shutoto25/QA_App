package jp.techacademy.mohri.shuto.qa_app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_send.*
import java.io.ByteArrayOutputStream

/**
 * 質問投稿画面.
 */
class QuestionSendActivity : AppCompatActivity(),
    View.OnClickListener, DatabaseReference.CompletionListener {

    /**
     * クラス名.
     */
    private val CLASS_NAME = "MainActivity"

    private var mGenre: Int = 0
    private var mPictureUri: Uri? = null
    /**
     *
     */
    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "$CLASS_NAME.onCreate")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_send)

        // 渡ってきたジャンルの番号を保持する
        val extras = intent.extras
        mGenre = extras!!.getInt("genre")

        btSend.setOnClickListener(this)
        ivCapture.setOnClickListener(this)

        // タイトルバーのタイトルの設定.
        title = "質問作成"
    }


    /**
     * Intent連携:画像を取得してImageViewに設定.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "$CLASS_NAME.onActivityResult")

        // TODO super読んでいいの？
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHOOSER_REQUEST_CODE) {
            // TODO
            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    contentResolver.delete(mPictureUri!!, null, null)
                    mPictureUri = null
                }
                return
            }

            // 画像を取得(trueの場合はカメラで撮影したときに該当)
            val uri = if (data == null || data.data == null) mPictureUri else data.data

            // URIからBitmap取得.
            val image: Bitmap
            try {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri!!)
                image = BitmapFactory.decodeStream(inputStream)
                inputStream!!.close()
            } catch (e: Exception) {
                return
            }

            // 取得したBitmapの長辺を500ピクセルにリサイズ.
            val imageWidth = image.width
            val imageHeight = image.height
            val scale = Math.min(500.toFloat() / imageWidth, 500.toFloat() / imageHeight)

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            val resizedImage =
                Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)

            // BitmapをImageViewに設定.
            ivCapture.setImageBitmap(resizedImage)

            mPictureUri = null
        }
    }


    override fun onClick(view: View) {
        Log.d(TAG, "$CLASS_NAME.onClick")

        when (view.id) {
            R.id.ivCapture -> {
                // パーミッション許可状態を確認.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                        showChooser()
                    } else {
                        // 許可ダイアログを表示.
                        requestPermissions(
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            PERMISSIONS_REQUEST_CODE)
                        return
                    }
                } else {
                    // Android M未満.
                    showChooser()
                }

            }
            R.id.btSend -> {
                // キーボードが出てたら閉じる
                val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                im.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

                val dataBaseReference = FirebaseDatabase.getInstance().reference
                // TODO
                val genreRef = dataBaseReference.child(CONTENTS_PATH).child(mGenre.toString())

                val data = HashMap<String, String>()
                // UID(利用者識別用番号).
                data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

                // タイトルと本文を取得,
                val title = etTitle.text.toString()
                val body = etSubject.text.toString()

                if (title.isEmpty()) {
                    // タイトルが入力されていない時はエラー
                    Snackbar.make(view, "タイトルを入力して下さい", Snackbar.LENGTH_LONG).show()
                    return
                }
                if (body.isEmpty()) {
                    // 質問が入力されていない時はエラー
                    Snackbar.make(view, "質問を入力して下さい", Snackbar.LENGTH_LONG).show()
                    return
                }

                // Preferenceから名前取得.
                val sp = PreferenceManager.getDefaultSharedPreferences(this)
                val name = sp.getString(PREFERENCE_KEY_ACCOUNT_NAME, "")

                data["title"] = title
                data["body"] = body
                data["name"] = name!!

                // 添付画像を取得(キャストに失敗した場合nullを返す).
                val drawable = ivCapture.drawable as? BitmapDrawable

                // 添付画像が設定されていれば画像を取り出してBASE64エンコード.
                // Firebaseは文字列や数字しか保存できない為.

                if (drawable != null) {
                    val bitmap = drawable.bitmap
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                    val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

                    data["image"] = bitmapString
                }

                genreRef.push().setValue(data, this)
                progressBar.visibility = View.VISIBLE
            }
        }
    }


    /**
     * 許可ダイアログでユーザが選択した結果を受け取る.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.d(TAG, "$CLASS_NAME.onRequestPermissionsResult")

        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showChooser()
                }
                return
            }
        }
    }


    /**
     * Firebaseへの保存が完了.
     */
    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        Log.d(TAG, "$CLASS_NAME.onComplete")

        progressBar.visibility = View.GONE

        if (databaseError == null) {
            // activity終了.
            finish()
        } else {
            Snackbar.make(findViewById(android.R.id.content), "投稿に失敗しました", Snackbar.LENGTH_LONG).show() }
    }


    /**
     * Intent連携の選択ダイアログを表示.
     */
    private fun showChooser() {
        Log.d(TAG, "$CLASS_NAME.showChooser")

        // ギャラリーから選択するIntent.TODO
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        // カメラで撮影するIntent.
        // ファイル名:現在日時.jpg
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        mPictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri)

        // ギャラリー選択のIntentを与えてcreateChooserメソッドを呼びだす.
        val chooserIntent = Intent.createChooser(galleryIntent, "画像を取得")

        // EXTRA_INITIAL_INTENTSにカメラ撮影のIntentを追加.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }
}