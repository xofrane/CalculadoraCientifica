package com.example.calculadora

import android.os.Bundle
import android.widget.*
import android.view.View
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var expresion: EditText
    private lateinit var resultado: TextView

    private var sonidoActivo = true
    private var oscuro = false

    private val historial = mutableListOf<String>()

    private val generadorSonido =
        ToneGenerator(AudioManager.STREAM_MUSIC, 70)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        expresion = findViewById(R.id.tvExpresion)
        resultado = findViewById(R.id.tvResultado)

        cargarHistorial()
        configurarBotones()
    }

    private fun configurarBotones() {

        val botones = mapOf(
            R.id.btn0 to "0",
            R.id.btn1 to "1",
            R.id.btn2 to "2",
            R.id.btn3 to "3",
            R.id.btn4 to "4",
            R.id.btn5 to "5",
            R.id.btn6 to "6",
            R.id.btn7 to "7",
            R.id.btn8 to "8",
            R.id.btn9 to "9",
            R.id.btnPunto to ".",
            R.id.btnSumar to "+",
            R.id.btnRestar to "-",
            R.id.btnMultiplicar to "*",
            R.id.btnDividir to "/",
            R.id.btnParAbre to "(",
            R.id.btnParCierra to ")",
            R.id.btnSin to "sin(",
            R.id.btnCos to "cos(",
            R.id.btnTan to "tan("
        )

        botones.forEach { (id, valor) ->
            findViewById<Button>(id).setOnClickListener {
                sonido()
                agregar(valor)
            }
        }

        findViewById<Button>(R.id.btnIgual).setOnClickListener {
            sonido()
            calcular()
        }

        findViewById<Button>(R.id.btnBorrar).setOnClickListener {
            sonido()
            expresion.setText("")
            resultado.text = "Resultado"
        }

        findViewById<Button>(R.id.btnEliminar).setOnClickListener {
            sonido()
            val texto = expresion.text.toString()
            if (texto.isNotEmpty()) {
                expresion.setText(texto.dropLast(1))
                expresion.setSelection(expresion.text.length)
            }
        }

        findViewById<Button>(R.id.btnTema).setOnClickListener {
            oscuro = !oscuro
            AppCompatDelegate.setDefaultNightMode(
                if (oscuro)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        findViewById<Button>(R.id.btnSonido).setOnClickListener {
            sonidoActivo = !sonidoActivo
            it as Button
            it.text = if (sonidoActivo) "Sonido ON" else "Sonido OFF"
        }

        findViewById<Button>(R.id.btnHistorial).setOnClickListener {
            mostrarHistorial()
        }

        findViewById<Button>(R.id.btnGrafica).setOnClickListener {
            graficar()
        }
    }

    private fun agregar(valor: String) {
        val inicio = expresion.selectionStart.coerceAtLeast(0)
        val texto = expresion.text.toString()

        val nuevo = texto.substring(0, inicio) +
                valor + texto.substring(inicio)

        expresion.setText(nuevo)
        expresion.setSelection(inicio + valor.length)
    }

    private fun sonido() {
        if (sonidoActivo) {
            generadorSonido.startTone(
                ToneGenerator.TONE_PROP_BEEP,
                80
            )
        }
    }

    // Funciones trigonométricas en grados

    private fun funcionSeno() = object :
        Function("sin", 1) {
        override fun apply(vararg args: Double): Double {
            return sin(Math.toRadians(args[0]))
        }
    }

    private fun funcionCoseno() = object :
        Function("cos", 1) {
        override fun apply(vararg args: Double): Double {
            return cos(Math.toRadians(args[0]))
        }
    }

    private fun funcionTangente() = object :
        Function("tan", 1) {
        override fun apply(vararg args: Double): Double {
            return tan(Math.toRadians(args[0]))
        }
    }

    private fun evaluar(texto: String): Double {
        val expresionMatematica = ExpressionBuilder(texto)
            .function(funcionSeno())
            .function(funcionCoseno())
            .function(funcionTangente())
            .build()

        return expresionMatematica.evaluate()
    }

    private fun calcular() {
        try {
            val operacion = expresion.text.toString()

            if (operacion.isBlank()) return

            val valor = evaluar(operacion)

            if (!valor.isFinite()) {
                throw ArithmeticException("Resultado no válido")
            }

            val respuesta = if (valor % 1.0 == 0.0) {
                valor.toLong().toString()
            } else {
                "%.8f".format(valor).trimEnd('0').trimEnd('.')
            }

            resultado.text = respuesta

            val registro = "$operacion = $respuesta"
            historial.add(0, registro)
            guardarHistorial()

        } catch (e: Exception) {
            resultado.text = "Error en operación"
        }
    }

    private fun guardarHistorial() {
        val preferencias = getSharedPreferences(
            "CalculadoraDatos",
            MODE_PRIVATE
        )

        preferencias.edit()
            .putString("historial", historial.joinToString(";;"))
            .apply()
    }

    private fun cargarHistorial() {
        val preferencias = getSharedPreferences(
            "CalculadoraDatos",
            MODE_PRIVATE
        )

        val datos = preferencias.getString("historial", "") ?: ""

        if (datos.isNotEmpty()) {
            historial.addAll(datos.split(";;"))
        }
    }

    private fun mostrarHistorial() {
        val contenido = if (historial.isEmpty()) {
            "Todavía no hay operaciones."
        } else {
            historial.joinToString("\n\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Historial de operaciones")
            .setMessage(contenido)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Borrar historial") { _, _ ->
                historial.clear()
                guardarHistorial()
                Toast.makeText(
                    this,
                    "Historial eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    // Gráfica de funciones con variable x
    private fun graficar() {
        try {
            val funcion = expresion.text.toString()

            if (funcion.isBlank() || !funcion.contains("x")) {
                Toast.makeText(
                    this,
                    "Escribe una función con x, por ejemplo: x*x",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val puntos = mutableListOf<Entry>()

            for (i in -100..100) {
                val x = i / 10.0

                try {
                    val y = ExpressionBuilder(funcion)
                        .function(funcionSeno())
                        .function(funcionCoseno())
                        .function(funcionTangente())
                        .variables("x")
                        .build()
                        .setVariable("x", x)
                        .evaluate()

                    if (y.isFinite() && abs(y) < 1000) {
                        puntos.add(Entry(x.toFloat(), y.toFloat()))
                    }
                } catch (_: Exception) {
                    // Se omiten puntos no definidos
                }
            }

            if (puntos.isEmpty()) {
                Toast.makeText(
                    this,
                    "No se pudo generar la gráfica",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val conjunto = LineDataSet(puntos, funcion)
            conjunto.color = Color.BLUE
            conjunto.setDrawCircles(false)
            conjunto.lineWidth = 2f

            val grafica = LineChart(this)
            grafica.data = LineData(conjunto)
            grafica.description.isEnabled = false
            grafica.setTouchEnabled(true)
            grafica.setPinchZoom(true)
            grafica.invalidate()

            AlertDialog.Builder(this)
                .setTitle("Gráfica matemática")
                .setView(grafica)
                .setPositiveButton("Cerrar", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error al graficar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        generadorSonido.release()
        super.onDestroy()
    }
}