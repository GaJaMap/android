package com.pg.gajamap.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.pg.gajamap.R

class CustomerDialogAdapter(private val context: Context, private val items: Array<String>) : BaseAdapter() {

    companion object {
        private const val KAKAO_TYPE = 0
        private const val NAVER_TYPE = 1
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = getItem(position) as String
        val inflater = LayoutInflater.from(context)

        val view: View = when {
            item.contains("카카오") -> {
                inflater.inflate(R.layout.kako_dialog_item, parent, false)
            }
            item.contains("네이버") -> {
                inflater.inflate(R.layout.naver_dialog_item, parent, false)
            }
            else -> {
                throw IllegalArgumentException("Unknown item type: $item")
            }
        }

        return view
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) as String
        return when {
            item.contains("카카오") -> KAKAO_TYPE
            item.contains("네이버") -> NAVER_TYPE
            else -> throw IllegalArgumentException("Unknown item type: $item")
        }
    }

    override fun getViewTypeCount(): Int {
        return 2
    }
}