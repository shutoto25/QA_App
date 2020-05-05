package jp.techacademy.mohri.shuto.qa_app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.ViewUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import kotlin.collections.HashMap

/**
 * 質問詳細リストアダプタ.
 */
class QuestionDetailListAdapter(context: Context, private val mQuestion: Question) : BaseAdapter() {

    companion object {
        /**
         * 質問リストレイアウト.
         */
        private val TYPE_QUESTION = 0
        /**
         * 回答リストレイアウト.
         */
        private val TYPE_ANSWER = 1
    }

    private var mSharedPreferences: SharedPreferences? =null
    private var mLayoutInflater: LayoutInflater? = null

    init {
        mLayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun getCount(): Int {
        return 1 + mQuestion.answers.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            // 1行目(position0)場合に質問タイプを返す.
            TYPE_QUESTION
        } else {
            TYPE_ANSWER
        }
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Any {
        return mQuestion
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view

        if (getItemViewType(position) == TYPE_QUESTION) {
            // 質問レイアウトを設定.
            if (convertView == null) {
                convertView =
                    mLayoutInflater!!.inflate(R.layout.list_question_detail, parent, false)!!
            }
            val body = mQuestion.body
            val name = mQuestion.name

            val bodyTextView = convertView.findViewById<View>(R.id.tvBody) as TextView
            bodyTextView.text = body

            val nameTextView = convertView.findViewById<View>(R.id.tvName) as TextView
            nameTextView.text = name

            val bytes = mQuestion.imageBytes
            if (bytes.isNotEmpty()) {
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    .copy(Bitmap.Config.ARGB_8888, true)
                val imageView = convertView.findViewById<View>(R.id.ivQuestionDetail) as ImageView
                imageView.setImageBitmap(image)
            }
        } else if (getItemViewType(position) == TYPE_ANSWER) {
            // 回答レイアウトを設定.
            if (convertView == null) {
                convertView = mLayoutInflater!!.inflate(R.layout.list_answer, parent, false)!!
            }
            val answer = mQuestion.answers[position - 1]
            val body = answer.body
            val name = answer.name

            val bodyTextView = convertView.findViewById<View>(R.id.tvAnswerBody) as TextView
            bodyTextView.text = body

            val nameTextView = convertView.findViewById<View>(R.id.tvAnswerName) as TextView
            nameTextView.text = name
        }
        return convertView!!
    }
}