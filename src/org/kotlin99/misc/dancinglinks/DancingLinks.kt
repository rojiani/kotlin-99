package org.kotlin99.misc.dancinglinks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kotlin99.common.tail
import java.util.*

class DLMatrix(matrix: List<List<Int>>) {
    val h: Node

    init {
        val width = matrix.first().size
        val height = matrix.size

        h = Node("h")
        h.linkDown(h)
        val headers = 0.rangeTo(width - 1).map{ Node("$it") }
        headers.forEach { it.linkDown(it) }
        headers.pairs().forEach { it.first.linkRight(it.second) }
        headers.last().linkRight(h).linkRight(headers.first())

        0.rangeTo(height - 1).forEach { row ->
            var firstNode: Node? = null
            var prevNode: Node? = null
            0.rangeTo(width - 1).forEach { column ->
                if (matrix[row][column] == 1) {
                    val node = Node("$column,$row")
                    val headerNode = headers[column]

                    node.header = headerNode
                    headerNode.up.linkDown(node).linkDown(headerNode)
                    if (prevNode == null) {
                        firstNode = node
                        prevNode = node
                    }
                    prevNode!!.linkRight(node).linkRight(firstNode!!)

                    prevNode = node
                }
            }
        }
    }

    val answer: ArrayList<Node> = ArrayList()

    fun search(): List<Node> {
        if (h.right == h) return answer

        var column = chooseColumn()
        column.coverColumn()

        var row = column.down
        while (row != column) {
            answer.add(row)
            row.eachRight { it.header.coverColumn() }

            val result = search()
            if (result.isNotEmpty()) return result

            row = answer.removeAt(answer.size - 1)
            column = row.header
            row.eachLeft { it.header.uncoverColumn() }

            row = row.down
        }

        column.uncoverColumn()
        
        return emptyList()
    }

    private fun chooseColumn() = h.right

    override fun toString(): String {
        fun Node.distanceToHeader(): Int {
            return header.toListDown().indexOf(this) - 1
        }

        val lines = ArrayList<String>()

        val headers = h.toListRight().tail()
        lines.add(headers.joinToString(""){ it.label.toString() })

        var nodeStacks = headers.map{ it.toListDown().tail() }
        while (!nodeStacks.all{ it.isEmpty() }) {
            val node = nodeStacks
                .filter{ it.isNotEmpty() }
                .minBy { it.first().toListRight().sumBy(Node::distanceToHeader) }!!.first()

            val nodesInRow = node.toListRight()
            val line = nodeStacks.map { stack ->
                    if (stack.any{ nodesInRow.contains(it) }) "1" else "0"
                }.joinToString("")
            lines.add(line)

            nodeStacks = nodeStacks.map{ stack ->
                stack.filter{ !nodesInRow.contains(it) }
            }
        }
        return lines.joinToString("\n")
    }

    private fun <T> List<T>.pairs(): List<Pair<T, T>> {
        return if (size <= 1) emptyList()
        else listOf(Pair(this[0], this[1])) + tail().pairs()
    }
}


class DLMatrixTest {
    @Test fun `creating new dancing links matrix`() {
        val matrix = DLMatrix(listOf(
            listOf(0, 0, 1, 0, 1, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 1),
            listOf(0, 1, 1, 0, 0, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 1),
            listOf(0, 0, 0, 1, 1, 0, 1)
        ))

        assertLinkedInAllDirections(matrix.h)

        assertThat(matrix.h.right.label!!, equalTo("0"))
        assertThat(matrix.h.left.label!!, equalTo("6"))

        assertThat(matrix.h.right.down.label!!, equalTo("0,1"))
        assertThat(matrix.h.left.down.label!!, equalTo("6,1"))
    }

    @Test fun `dancing links matrix conversion to string`() {
        val matrix = DLMatrix(listOf(
            listOf(0, 0, 1, 0, 1, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 1),
            listOf(0, 1, 1, 0, 0, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 1),
            listOf(0, 0, 0, 1, 1, 0, 1)
        ))

        assertThat(matrix.toString(), equalTo("""
            |0123456
            |1001001
            |0010110
            |1001000
            |0110010
            |0100001
            |0001101
        """.trimMargin()))
    }

    @Test fun `cover first column in matrix`() {
        val matrix = DLMatrix(listOf(
                listOf(0, 0, 1, 0, 1, 1, 0),
                listOf(1, 0, 0, 1, 0, 0, 1),
                listOf(0, 1, 1, 0, 0, 1, 0),
                listOf(1, 0, 0, 1, 0, 0, 0),
                listOf(0, 1, 0, 0, 0, 0, 1),
                listOf(0, 0, 0, 1, 1, 0, 1)
        ))

        matrix.h.right.coverColumn()

        assertLinkedInAllDirections(matrix.h)
        assertThat(matrix.toString(), equalTo("""
            |123456
            |010110
            |100001
            |110010
            |001101
        """.trimMargin()))
    }

    @Test fun `find solution for cover problem from dancing links paper`() {
        val matrix = DLMatrix(listOf(
                listOf(0, 0, 1, 0, 1, 1, 0),
                listOf(1, 0, 0, 1, 0, 0, 1),
                listOf(0, 1, 1, 0, 0, 1, 0),
                listOf(1, 0, 0, 1, 0, 0, 0),
                listOf(0, 1, 0, 0, 0, 0, 1),
                listOf(0, 0, 0, 1, 1, 0, 1)
        ))
        assertThat(matrix.search().map{ it.toListRight().map{ it.header.label }.joinToString() }, equalTo(listOf(
                "0, 3",
                "1, 6",
                "2, 4, 5"
        )))
    }

    private fun assertLinkedInAllDirections(node: Node, visited: HashSet<Node> = HashSet()) {
        assertTrue(node != Node.none)

        if (visited.contains(node)) return
        visited.add(node)

        assertLinkedInAllDirections(node.up, visited)
        assertLinkedInAllDirections(node.down, visited)
        assertLinkedInAllDirections(node.left, visited)
        assertLinkedInAllDirections(node.right, visited)
    }
}