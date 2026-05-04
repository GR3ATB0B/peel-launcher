package com.peel.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class AppTileAdapter(
    private val tiles: List<AppTile>,
    private val onTileClick: (AppTile) -> Unit,
) : RecyclerView.Adapter<AppTileAdapter.TileViewHolder>() {

    class TileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.tile_card)
        val icon: ImageView = view.findViewById(R.id.tile_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val tile = tiles[position]
        val ctx = holder.itemView.context
        holder.card.setCardBackgroundColor(ContextCompat.getColor(ctx, tile.colorRes))
        holder.icon.setImageResource(tile.iconRes)
        holder.icon.contentDescription = tile.label
        holder.itemView.setOnClickListener { onTileClick(tile) }
    }

    override fun getItemCount(): Int = tiles.size
}
