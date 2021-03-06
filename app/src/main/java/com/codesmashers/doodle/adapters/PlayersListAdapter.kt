package com.codesmashers.doodle.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codesmashers.doodle.R
import com.codesmashers.doodle.model.playerInfo
import kotlinx.android.synthetic.main.list_players.view.*
import com.codesmashers.doodle.WaitingActivity

/** Adapter used to populate Players List in [WaitingActivity]. */
class PlayersListAdapter(
    private val players: MutableList<playerInfo>
) : RecyclerView.Adapter<PlayersListAdapter.PlayersViewHolder>() {

    class PlayersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_players, parent, false)
        return PlayersViewHolder(itemView)
    }

    override fun getItemCount(): Int = players.size

    override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {
        holder.itemView.player_name.text = players[position].Name
    }
}
