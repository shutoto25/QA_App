package jp.techacademy.mohri.shuto.qa_app

import java.io.Serializable

/**
 *
 */
class Question (val title: String,
                val body: String,
                val name: String,
                val uid: String,
                val questionUid: String,
                val genre: Int,
                bytes: ByteArray,
                val answers: ArrayList<Answer>) : Serializable {

    /**
     *
     */
    val imageBytes: ByteArray

    /**
     * 初期化.
     */
    init {
        imageBytes = bytes.clone()
    }
}