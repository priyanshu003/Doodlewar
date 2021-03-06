package com.codesmashers.doodle.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codesmashers.doodle.R
import com.codesmashers.doodle.model.ChatText
import kotlinx.android.synthetic.main.chats_print.view.*
import com.codesmashers.doodle.GameActivity

/** Adapter used to populate Chat Messages List in [GameActivity]. */
class ChatAdapter(
    val chats: MutableList<ChatText>, val colorSet: MutableSet<String>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.chats_print, parent, false)
        return ChatViewHolder(itemView)
    }

    override fun getItemCount(): Int = chats.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.itemView.username_person.text = chats[position].userName + ": "
        holder.itemView.text_person.text = chats[position].text

        if (chats[position].text == "word guessed!!")
            holder.itemView.text_person.setTextColor(Color.parseColor("#0F5C53"))
        else
            holder.itemView.text_person.setTextColor(Color.BLACK)
    }

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

}
