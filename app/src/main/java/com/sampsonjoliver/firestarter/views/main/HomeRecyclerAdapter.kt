package com.sampsonjoliver.firestarter.views.main

import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.model.LatLng
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Session
import com.sampsonjoliver.firestarter.utils.DistanceUtils
import com.sampsonjoliver.firestarter.utils.inflate
import kotlinx.android.synthetic.main.row_session.view.*
import java.util.*

class HomeRecyclerAdapter(var latLng: LatLng, val listener: OnSessionClickedListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnSessionClickedListener {
        fun onSessionClicked(session: Session)
    }

    val sessions: MutableList<Session> = mutableListOf()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if (holder is SessionViewHolder)
            holder.bindView(sessions[position], listener)

    }

    override fun getItemCount(): Int {
        return sessions.size
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        return SessionViewHolder(parent?.inflate(R.layout.row_session, false))
    }

    inner class SessionViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
        fun bindView(session: Session, listener: OnSessionClickedListener) {
            itemView.title.text = session.topic
            itemView.subtitle.text = session.username
            itemView.image.setImageURI(session.bannerUrl)

            itemView.distance.text = DistanceUtils.formatDistance(DistanceUtils.latLngDistance(latLng, session.location)[0].toDouble())

            itemView.time.text = DateUtils.getRelativeTimeSpanString(
                    session.startDateAsDate.time ?: Date().time,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE)

            itemView.setOnClickListener { listener.onSessionClicked(session) }
        }
    }
}