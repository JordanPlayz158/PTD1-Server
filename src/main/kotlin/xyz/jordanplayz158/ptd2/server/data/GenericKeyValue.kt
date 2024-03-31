package xyz.jordanplayz158.ptd2.server.data

import org.jetbrains.exposed.sql.SizedIterable
import xyz.jordanplayz158.ptd2.server.orm.PTD2Extra
import xyz.jordanplayz158.ptd2.server.orm.PTD2Item

data class GenericKeyValue(val num: Byte, val quantity: Int) {
    companion object {
        // Can't be named the same due to type erasure (think that is what it is called)
        fun convertItems(items: SizedIterable<PTD2Item>): List<GenericKeyValue> {
            val array = ArrayList<GenericKeyValue>(items.count().toInt())

            for (item in items) {
                array.add(GenericKeyValue(item.num, item.value))
            }

            return array
        }

        fun convertExtra(extras: SizedIterable<PTD2Extra>): List<GenericKeyValue> {
            val array = ArrayList<GenericKeyValue>(extras.count().toInt())

            for (item in extras) {
                array.add(GenericKeyValue(item.num, item.value))
            }

            return array
        }
    }
}