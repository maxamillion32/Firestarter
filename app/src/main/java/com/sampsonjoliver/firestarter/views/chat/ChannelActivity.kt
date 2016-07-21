package com.sampsonjoliver.firestarter.views.chat

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.*
import com.sampsonjoliver.firestarter.FirebaseActivity
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Message
import com.sampsonjoliver.firestarter.models.Session
import com.sampsonjoliver.firestarter.service.FirebaseService
import com.sampsonjoliver.firestarter.service.References
import com.sampsonjoliver.firestarter.service.SessionManager
import com.sampsonjoliver.firestarter.utils.TAG
import com.sampsonjoliver.firestarter.utils.copyToClipboard
import kotlinx.android.synthetic.main.activity_channel.*

class ChannelActivity : FirebaseActivity(),
        ChannelMessageRecyclerAdapter.ChatListener,
        ChildEventListener {
    companion object {
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
    }

    val session: Session? = null
    val sessionId: String? by lazy { intent.getStringExtra(EXTRA_SESSION_ID) }
    val adapter by lazy { ChannelMessageRecyclerAdapter(SessionManager.getUid(), this) }

    val sessionListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError?) {
            Log.w(this@ChannelActivity.TAG, "onCancelled")
        }

        override fun onDataChange(p0: DataSnapshot?) {
            Log.w(this@ChannelActivity.TAG, "onDataChange: ${p0?.key}")
            val session = p0?.getValue(Session::class.java)
            session?.sessionId = p0?.key
        }
    }

    override fun onItemInsertedListener() {
        recycler.smoothScrollToPosition(0)
    }

    override fun onMessageLongPress(message: Message) {
        Snackbar.make(messageText, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show()
        this.copyToClipboard(message.messageId, message.message)
    }

    override fun onChildMoved(p0: DataSnapshot?, previousChildName: String?) = Unit

    override fun onChildChanged(p0: DataSnapshot?, previousChildName: String?) {
        Log.w(this.TAG, "onChildChanged: ${p0?.key}")

        val key = p0?.key ?: ""
        val message = p0?.getValue(Message::class.java)

        // todo need to update message inside of its message group; this is likely to be an expensive
        // op with our current data model
    }

    override fun onChildAdded(p0: DataSnapshot?, previousChildName: String?) {
        Log.w(this.TAG, "onChildAdded: ${p0?.key}")

        val key = p0?.key ?: ""
        val message = p0?.getValue(Message::class.java)

        message?.let {
            adapter.addMessage(message)
        }
    }

    override fun onChildRemoved(p0: DataSnapshot?) {
        Log.w(this.TAG, "onChildRemoved: ${p0?.key}")
        val key = p0?.key ?: ""

        // todo need to update message inside of its message group; this is likely to be an expensive
        // op with our current local data model
    }

    override fun onCancelled(p0: DatabaseError?) {
        Log.w(this.TAG, "onCancelled", p0?.toException());
        Toast.makeText(this@ChannelActivity, "Failed to load chat.",
                Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_channel)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        collapsingToolbar.isTitleEnabled = true
        collapsingToolbar.setTitle("Chatorama") // todo
        toolbar.setOnClickListener { appBarLayout.setExpanded(true, true) }
        messageText.setOnClickListener { appBarLayout.setExpanded(false, true) }

        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        recycler.adapter = adapter

        attachDataListener()

        sendButton.setOnClickListener { sendNewMessage(messageText) }

        messageText.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendNewMessage(messageText)
                return@OnEditorActionListener true
            }
            false
        })
    }

    fun sendNewMessage(messageWidget: EditText) {
        if (messageWidget.text.isNullOrBlank().not()) {
            // Upload the message to firebase and clear the message textbox
            sendNewMessage(messageWidget.text.toString(), SessionManager.getUid())
            messageWidget.setText("")
        }
    }

    fun sendNewMessage(messageText: String, userId: String) {
        val message = Message(userId, SessionManager.getUserPhotoUrl(), sessionId ?: "", messageText)

        FirebaseService.getReference(References.Messages)
                .child(sessionId)
                .push()
                .setValue(message, DatabaseReference.CompletionListener { databaseError, databaseReference ->
                    Log.w(this@ChannelActivity.TAG, "onPushMessage: error=" + databaseError?.message)
                })
    }

    fun attachDataListener(detach: Boolean = false) {
        val ref = FirebaseService.getReference(References.Messages)
                .child(sessionId)
                .orderByChild("timestamp")
                .limitToLast(100)

        if (detach)
            ref.removeEventListener(this)
        else
            ref.addChildEventListener(this)

        FirebaseService.getReference(References.Sessions).child(sessionId).run {
            when (detach) {
                true -> this.removeEventListener(sessionListener)
                else -> this.addValueEventListener(sessionListener)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        attachDataListener(true)

    }
}