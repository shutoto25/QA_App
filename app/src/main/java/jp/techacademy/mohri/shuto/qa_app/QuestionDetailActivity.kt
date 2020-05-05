package jp.techacademy.mohri.shuto.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

/**
 * 質問詳細画面.
 */
class QuestionDetailActivity : AppCompatActivity(), View.OnClickListener {
    /**
     * クラス名.
     */
    private val CLASS_NAME = "QuestionDetailActivity"
    /**
     * ログイン状態.
     */
    private var LOGIN = true
    /**
     * 設定ジャンル一時保存.
     */
    private var mGenreTemp:Long? = null

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var favRef: DatabaseReference

    /**
     * イベントリスナ.
     */
    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {}
    }

    /**
     * お気に入り用リスナ.
     */
    private val mFavListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            // 画面表示時に現在のジャンルを取得しておく.
            mGenreTemp = dataSnapshot.value as Long
            btButton.text = "favorite"
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)
        Log.d(TAG, "$CLASS_NAME.onCreate")

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        lvQuestionDetail.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(CONTENTS_PATH)
            .child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(ANSWERS_PATH)
        mAnswerRef.addChildEventListener(mEventListener)
    }


    override fun onStart() {
        super.onStart()
        // ログインしている場合にのみお気に入りボタンを表示する.
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            favRef = dataBaseReference.child(FAVORITE_PATH)
                .child(FirebaseAuth.getInstance().currentUser!!.uid).child(mQuestion.questionUid)
            favRef.addChildEventListener(mFavListener)
            btButton.setOnClickListener(this)
            LOGIN = true
        } else {
            // ボタン非表示.
            btButton.visibility = View.GONE
            LOGIN = false
        }
    }


    override fun onStop() {
        super.onStop()
        if(LOGIN) {
            favRef.removeEventListener(mFavListener)
        }
    }

    override fun onClick(view: View) {

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        favRef = dataBaseReference.child(FAVORITE_PATH)
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child(mQuestion.questionUid)

        if (btButton.text.toString() == "favorite") {
            // お気に入りから削除.
            btButton.text = "not favorite"
            favRef.removeValue()
        } else {
            // お気に入りに追加.
            val favData = HashMap<String, Long>()
            if (mGenreTemp != null) {
                // 一時取得したジャンルがある場合はそのIDをセット(お気に入りでセットされることを防ぎたい為)
                favData["genre"] = mGenreTemp!!
                mGenreTemp = null
            } else {
                favData["genre"] = mQuestion.genre.toLong()
            }
            favRef.setValue(favData)
        }
    }
}