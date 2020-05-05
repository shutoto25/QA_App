package jp.techacademy.mohri.shuto.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

/**
 * TOP画面.
 */
class MainActivity : AppCompatActivity()
    , NavigationView.OnNavigationItemSelectedListener {
    /**
     * クラス名.
     */
    private val CLASS_NAME = "MainActivity"
    /**
     * ツールバー.
     */
    private lateinit var mToolbar: Toolbar
    /**
     * ジャンル.
     */
    private var mGenre = 0

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mListView: ListView
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionListAdapter

    private var mGenreRef: DatabaseReference? = null

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {

            // お気に入りの場合.
            if (mGenre == 5){
                for(i in 1 until 4){
                mGenreRef = mDatabaseReference.child(CONTENTS_PATH).child(i.toString())
                }
            }

            val map = dataSnapshot.value as Map<String, String>
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    answerArrayList.add(answer)
                }
            }
            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                mGenre, bytes, answerArrayList)
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        override fun onCancelled(p0: DatabaseError) {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "$CLASS_NAME.onCreate")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)

        val fab = findViewById<FloatingActionButton>(R.id.faButton)
        fab.setOnClickListener { _ ->
            // ログイン済みのユーザを取得.
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログイン画面遷移.
                val intent = Intent(applicationContext, LoginActivity::class.java)

                startActivity(intent)
            } else {
                // 質問投稿画面遷移
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra(INTENT_EXTRA_KEY_GENRE, mGenre)
                startActivity(intent)
            }
        }

        // ナビゲーションドロワーの設定
        val drawer = findViewById<DrawerLayout>(R.id.drawerLayout)
        val toggle =
            ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.navView)
        navigationView.setNavigationItemSelectedListener(this)

        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mListView = findViewById(R.id.lvQuestion)
        mAdapter = QuestionListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()
        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }


    override fun onResume() {
        Log.d(TAG, "$CLASS_NAME.onResume")

        super.onResume()
        val navigationView = findViewById<NavigationView>(R.id.navView)

        // 趣味を既定の選択とする
        if(mGenre == 0) {
            //TODO 共通的な場所作って起動時はそのにしたい今は一番上の趣味が選択される
            onNavigationItemSelected(navigationView.menu.getItem(0))
        }
    }


    /**
     * オプションメニューをインフレート.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "$CLASS_NAME.onCreateOptionsMenu")

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


    /**
     * オプションメニュー選択.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "$CLASS_NAME.onOptionsItemSelected")

        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    /**
     * ドロワーアイテム選択.
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "$CLASS_NAME.onNavigationItemSelected")

        when (item.itemId) {
            R.id.navHobby -> {
                mToolbar.title = getString(R.string.navigation_item_hobby)
                mGenre = 1
            }
            R.id.navLife -> {
                mToolbar.title = getString(R.string.navigation_item_life)
                mGenre = 2
            }
            R.id.navHealth -> {
                mToolbar.title = getString(R.string.navigation_item_health)
                mGenre = 3
            }
            R.id.navCompter -> {
                mToolbar.title = getString(R.string.navigation_item_computer)
                mGenre = 4
            }
            R.id.navFavorite -> {
                mToolbar.title = getString(R.string.navigation_item_favorite)
                mGenre = 5
            }
        }
        // ドロワーを閉じる.
        val drawer = findViewById<DrawerLayout>(R.id.drawerLayout)
        drawer.closeDrawer(GravityCompat.START)

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        mListView.adapter = mAdapter

        // 選択したジャンルにリスナーを登録する
        if (mGenreRef != null) {
            mGenreRef!!.removeEventListener(mEventListener)
        }

        if (mGenre == 5){
            mGenreRef = mDatabaseReference.child(FAVORITE_PATH).child(FirebaseAuth.getInstance().currentUser!!.uid)
        } else {
            mGenreRef = mDatabaseReference.child(CONTENTS_PATH).child(mGenre.toString())
        }
        mGenreRef!!.addChildEventListener(mEventListener)

        return true
    }
}
