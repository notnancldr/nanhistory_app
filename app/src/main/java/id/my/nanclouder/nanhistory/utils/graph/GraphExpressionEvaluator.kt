package id.my.nanclouder.nanhistory.utils.graph

import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import id.my.nanclouder.nanhistory.utils.history.LocationData
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Expression evaluator for custom graph data series.
 * Supports math operators, built-in functions, telemetry properties,
 * and sibling comparisons with index offsets (e.g. `speed - speed[-1]`).
 */
object GraphExpressionEvaluator {

    fun validate(expression: String): Result<Unit> = runCatching {
        if (expression.isBlank()) throw IllegalArgumentException("Expression cannot be blank")
        val dummyPoint = HistoryLocationData(
            speed = 10f,
            acceleration = 1f,
            distance = 5f,
            points = listOf(Coordinate(0.0, 0.0), Coordinate(0.001, 0.001)),
            start = ZonedDateTime.now().minusSeconds(5),
            end = ZonedDateTime.now(),
            locationData = listOf(
                LocationData(
                    time = ZonedDateTime.now(),
                    location = Coordinate(0.0, 0.0),
                    speed = 10f,
                    bearing = 45f,
                    altitude = 100.0,
                    accuracy = 5f
                )
            )
        )
        evaluate(expression, listOf(dummyPoint, dummyPoint))
    }

    fun evaluate(expression: String, data: List<HistoryLocationData>): List<Float> {
        if (data.isEmpty() || expression.isBlank()) return emptyList()
        val tokens = tokenize(expression)
        val parser = Parser(tokens)
        val ast = parser.parse()

        return data.indices.map { index ->
            val context = EvaluationContext(data, index)
            ast.eval(context).toFloat()
        }
    }

    internal fun extractProperty(point: HistoryLocationData, propertyName: String): Double {
        val lower = propertyName.lowercase().trim()
        val lastLocation = point.locationData.lastOrNull()
        val lastCoord = point.points.lastOrNull()

        return when (lower) {
            "speed" -> point.speed.toDouble()
            "acceleration" -> point.acceleration.toDouble()
            "distance" -> point.distance.toDouble()
            "altitude" -> lastLocation?.altitude ?: 0.0
            "bearing" -> (lastLocation?.bearing ?: 0f).toDouble()
            "accuracy" -> (lastLocation?.accuracy ?: 0f).toDouble()
            "speedaccuracy" -> (lastLocation?.speedAccuracy ?: 0f).toDouble()
            "bearingaccuracy" -> (lastLocation?.bearingAccuracy ?: 0f).toDouble()
            "verticalaccuracy" -> (lastLocation?.verticalAccuracy ?: 0f).toDouble()
            "latitude", "lat" -> lastCoord?.latitude ?: 0.0
            "longitude", "lon", "lng" -> lastCoord?.longitude ?: 0.0
            "time" -> {
                val durationMs = point.end.toInstant().toEpochMilli() - point.start.toInstant().toEpochMilli()
                durationMs / 1000.0
            }
            else -> 0.0
        }
    }

    // --- Tokenizer ---
    sealed class Token {
        data class NumberToken(val value: Double) : Token()
        data class IdentToken(val name: String) : Token()
        data class StringToken(val value: String) : Token()
        data class OperatorToken(val op: Char) : Token()
        data object LParen : Token()
        data object RParen : Token()
        data object LBracket : Token()
        data object RBracket : Token()
        data object Comma : Token()
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++
                c in "()[]," -> {
                    tokens.add(
                        when (c) {
                            '(' -> Token.LParen
                            ')' -> Token.RParen
                            '[' -> Token.LBracket
                            ']' -> Token.RBracket
                            ',' -> Token.Comma
                            else -> error("Unexpected char $c")
                        }
                    )
                    i++
                }
                c in "+-*/%^" -> {
                    tokens.add(Token.OperatorToken(c))
                    i++
                }
                c == '"' || c == '\'' -> {
                    val quote = c
                    val start = i + 1
                    var end = start
                    while (end < input.length && input[end] != quote) end++
                    if (end >= input.length) throw IllegalArgumentException("Unterminated string quote at $start")
                    tokens.add(Token.StringToken(input.substring(start, end)))
                    i = end + 1
                }
                c.isDigit() || (c == '.' && i + 1 < input.length && input[i + 1].isDigit()) -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    val numStr = input.substring(start, i)
                    tokens.add(Token.NumberToken(numStr.toDoubleOrNull() ?: 0.0))
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < input.length && (input[i].isLetterOrDigit() || input[i] == '_')) i++
                    tokens.add(Token.IdentToken(input.substring(start, i)))
                }
                else -> throw IllegalArgumentException("Unexpected character '$c' at position $i")
            }
        }
        return tokens
    }

    // --- Parser & AST ---
    class EvaluationContext(
        val data: List<HistoryLocationData>,
        val currentIndex: Int
    )

    sealed interface Node {
        fun eval(ctx: EvaluationContext): Double
    }

    data class NumberNode(val value: Double) : Node {
        override fun eval(ctx: EvaluationContext): Double = value
    }

    data class PropertyNode(val name: String, val offset: Int) : Node {
        override fun eval(ctx: EvaluationContext): Double {
            if (ctx.data.isEmpty()) return 0.0
            val targetIndex = (ctx.currentIndex + offset).coerceIn(0, ctx.data.lastIndex)
            val point = ctx.data[targetIndex]
            return extractProperty(point, name)
        }
    }

    data class FunctionCallNode(val name: String, val args: List<Node>) : Node {
        override fun eval(ctx: EvaluationContext): Double {
            val evalArgs = args.map { it.eval(ctx) }
            return when (name.lowercase()) {
                "abs" -> abs(evalArgs.getOrElse(0) { 0.0 })
                "sqrt" -> sqrt(evalArgs.getOrElse(0) { 0.0 })
                "round" -> round(evalArgs.getOrElse(0) { 0.0 })
                "floor" -> floor(evalArgs.getOrElse(0) { 0.0 })
                "ceil" -> ceil(evalArgs.getOrElse(0) { 0.0 })
                "min" -> min(evalArgs.getOrElse(0) { 0.0 }, evalArgs.getOrElse(1) { 0.0 })
                "max" -> max(evalArgs.getOrElse(0) { 0.0 }, evalArgs.getOrElse(1) { 0.0 })
                "avg", "mean", "smooth" -> {
                    if (args.isEmpty()) return 0.0
                    val exprNode = args[0]
                    val (lookBehind, lookAhead) = if (args.size >= 3) {
                        val behind = args[1].eval(ctx).toInt().coerceAtLeast(0)
                        val ahead = args[2].eval(ctx).toInt().coerceAtLeast(0)
                        behind to ahead
                    } else if (args.size == 2) {
                        val window = args[1].eval(ctx).toInt().coerceAtLeast(1)
                        val half = (window - 1) / 2
                        val ahead = window / 2
                        half to ahead
                    } else {
                        0 to 0
                    }

                    val startIdx = (ctx.currentIndex - lookBehind).coerceIn(0, ctx.data.lastIndex)
                    val endIdx = (ctx.currentIndex + lookAhead).coerceIn(0, ctx.data.lastIndex)
                    val count = endIdx - startIdx + 1
                    if (count <= 0) return 0.0

                    var sum = 0.0
                    for (k in startIdx..endIdx) {
                        sum += exprNode.eval(EvaluationContext(ctx.data, k))
                    }
                    sum / count
                }
                "property", "prop" -> {
                    val propName = (args.getOrNull(0) as? StringLiteralNode)?.value ?: ""
                    val offset = evalArgs.getOrNull(1)?.toInt() ?: 0
                    val targetIndex = (ctx.currentIndex + offset).coerceIn(0, ctx.data.lastIndex)
                    extractProperty(ctx.data[targetIndex], propName)
                }
                else -> throw IllegalArgumentException("Unknown function '$name'")
            }
        }
    }

    data class StringLiteralNode(val value: String) : Node {
        override fun eval(ctx: EvaluationContext): Double = 0.0
    }

    data class UnaryOpNode(val op: Char, val operand: Node) : Node {
        override fun eval(ctx: EvaluationContext): Double {
            val v = operand.eval(ctx)
            return if (op == '-') -v else v
        }
    }

    data class BinaryOpNode(val left: Node, val op: Char, val right: Node) : Node {
        override fun eval(ctx: EvaluationContext): Double {
            val l = left.eval(ctx)
            val r = right.eval(ctx)
            return when (op) {
                '+' -> l + r
                '-' -> l - r
                '*' -> l * r
                '/' -> if (r != 0.0) l / r else 0.0
                '%' -> if (r != 0.0) l % r else 0.0
                '^' -> l.pow(r)
                else -> error("Unsupported binary operator $op")
            }
        }
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun parse(): Node {
            val node = parseExpression()
            if (pos < tokens.size) {
                throw IllegalArgumentException("Unexpected token after expression: ${tokens[pos]}")
            }
            return node
        }

        private fun peek(): Token? = tokens.getOrNull(pos)
        private fun consume(): Token = tokens[pos++]

        private fun parseExpression(): Node {
            var left = parseTerm()
            while (peek() is Token.OperatorToken && (peek() as Token.OperatorToken).op in "+-") {
                val op = (consume() as Token.OperatorToken).op
                val right = parseTerm()
                left = BinaryOpNode(left, op, right)
            }
            return left
        }

        private fun parseTerm(): Node {
            var left = parseFactor()
            while (peek() is Token.OperatorToken && (peek() as Token.OperatorToken).op in "*/%") {
                val op = (consume() as Token.OperatorToken).op
                val right = parseFactor()
                left = BinaryOpNode(left, op, right)
            }
            return left
        }

        private fun parseFactor(): Node {
            val left = parsePower()
            if (peek() is Token.OperatorToken && (peek() as Token.OperatorToken).op == '^') {
                consume()
                val right = parseFactor()
                return BinaryOpNode(left, '^', right)
            }
            return left
        }

        private fun parsePower(): Node {
            if (peek() is Token.OperatorToken) {
                val op = (peek() as Token.OperatorToken).op
                if (op == '+' || op == '-') {
                    consume()
                    return UnaryOpNode(op, parsePrimary())
                }
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Node {
            return when (val token = peek()) {
                is Token.NumberToken -> {
                    consume()
                    NumberNode(token.value)
                }
                is Token.StringToken -> {
                    consume()
                    StringLiteralNode(token.value)
                }
                is Token.LParen -> {
                    consume()
                    val expr = parseExpression()
                    if (peek() != Token.RParen) throw IllegalArgumentException("Expected ')' but found ${peek()}")
                    consume()
                    expr
                }
                is Token.IdentToken -> {
                    consume()
                    val name = token.name
                    if (peek() == Token.LParen) {
                        consume()
                        val args = mutableListOf<Node>()
                        if (peek() != Token.RParen) {
                            args.add(parseExpression())
                            while (peek() == Token.Comma) {
                                consume()
                                args.add(parseExpression())
                            }
                        }
                        if (peek() != Token.RParen) throw IllegalArgumentException("Expected ')' after function arguments")
                        consume()
                        FunctionCallNode(name, args)
                    } else if (peek() == Token.LBracket) {
                        consume()
                        var sign = 1
                        if (peek() is Token.OperatorToken) {
                            val op = (consume() as Token.OperatorToken).op
                            if (op == '-') sign = -1
                            else if (op != '+') throw IllegalArgumentException("Invalid index sign $op")
                        }
                        val indexToken = consume() as? Token.NumberToken
                            ?: throw IllegalArgumentException("Expected integer index inside []")
                        if (peek() != Token.RBracket) throw IllegalArgumentException("Expected ']' after index")
                        consume()
                        val offset = (indexToken.value.toInt()) * sign
                        PropertyNode(name, offset)
                    } else {
                        PropertyNode(name, 0)
                    }
                }
                else -> throw IllegalArgumentException("Unexpected token: $token")
            }
        }
    }
}
