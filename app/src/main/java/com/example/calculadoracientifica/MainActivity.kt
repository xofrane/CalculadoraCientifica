package com.example.calculadoracientifica

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class MainActivity : AppCompatActivity() {

    private lateinit var expresion: EditText
    private lateinit var resultado: TextView

    private var sonidoActivo = true
    private var oscuro = false

    private val historial = mutableListOf<String>()
    private var generadorSonido: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        expresion = findViewById(R.id.tvExpresion)
        resultado = findViewById(R.id.tvResultado)

        // Evitar que el teclado virtual del sistema se muestre al tocar el campo de texto
        expresion.showSoftInputOnFocus = false

        // Inicializar sonido de forma segura
        try {
            generadorSonido = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (_: Exception) {
            generadorSonido = null
        }

        // Detectar estado inicial de tema
        oscuro = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        findViewById<Button>(R.id.btnTema)?.text = if (oscuro) "Modo claro" else "Modo oscuro"

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
            findViewById<Button>(id)?.setOnClickListener {
                sonido()
                agregar(valor)
            }
        }

        findViewById<Button>(R.id.btnIgual)?.setOnClickListener {
            sonido()
            calcular()
        }

        findViewById<Button>(R.id.btnBorrar)?.setOnClickListener {
            sonido()
            expresion.setText("")
            resultado.text = "Resultado"
        }

        findViewById<Button>(R.id.btnEliminar)?.setOnClickListener {
            sonido()
            val texto = expresion.text.toString()
            if (texto.isNotEmpty()) {
                expresion.setText(texto.dropLast(1))
                expresion.setSelection(expresion.text.length)
            }
        }

        findViewById<Button>(R.id.btnTema)?.setOnClickListener {
            sonido()
            oscuro = !oscuro
            AppCompatDelegate.setDefaultNightMode(
                if (oscuro) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        findViewById<Button>(R.id.btnSonido)?.setOnClickListener {
            sonidoActivo = !sonidoActivo
            (it as? Button)?.text = if (sonidoActivo) "Sonido ON" else "Sonido OFF"
        }

        findViewById<Button>(R.id.btnHistorial)?.setOnClickListener {
            sonido()
            mostrarHistorial()
        }

        findViewById<Button>(R.id.btnGrafica)?.setOnClickListener {
            sonido()
            mostrarOpcionesGraficador()
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
            try {
                generadorSonido?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            } catch (_: Exception) {
            }
        }
    }

    // Funciones trigonométricas en grados

    private fun funcionSeno() = object : Function("sin", 1) {
        override fun apply(vararg args: Double): Double {
            return sin(Math.toRadians(args[0]))
        }
    }

    private fun funcionCoseno() = object : Function("cos", 1) {
        override fun apply(vararg args: Double): Double {
            return cos(Math.toRadians(args[0]))
        }
    }

    private fun funcionTangente() = object : Function("tan", 1) {
        override fun apply(vararg args: Double): Double {
            return tan(Math.toRadians(args[0]))
        }
    }

    private fun evaluar(texto: String): Double {
        var limpio = texto
            .replace("×", "*")
            .replace("÷", "/")
            .replace("–", "-")

        val abreCount = limpio.count { it == '(' }
        val cierraCount = limpio.count { it == ')' }
        if (abreCount > cierraCount) {
            limpio += ")".repeat(abreCount - cierraCount)
        }

        val expresionMatematica = ExpressionBuilder(limpio)
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

    // --- OPCIONES DE GRAFICACIÓN ---

    private fun mostrarOpcionesGraficador() {
        val opciones = arrayOf(
            "Escribir función manualmente",
            "Graficar operación del historial",
            "Graficar operación / resultado actual"
        )

        AlertDialog.Builder(this)
            .setTitle("Opciones de Graficación")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirGraficadorDialog("")
                    1 -> seleccionarDelHistorialParaGraficar()
                    2 -> graficarActual()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun seleccionarDelHistorialParaGraficar() {
        if (historial.isEmpty()) {
            Toast.makeText(this, "El historial está vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val items = historial.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Selecciona una operación del historial")
            .setItems(items) { _, which ->
                val operacion = items[which]
                val expresionSolo = if (operacion.contains("=")) {
                    operacion.split("=")[0].trim()
                } else {
                    operacion
                }
                abrirGraficadorDialog(expresionSolo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun graficarActual() {
        val exp = expresion.text.toString().trim()
        val res = resultado.text.toString().trim()

        val valorPrefil = when {
            exp.isNotBlank() -> exp
            res.isNotBlank() && res != "Resultado" && res != "Error en operación" -> res
            else -> "x*x"
        }

        abrirGraficadorDialog(valorPrefil)
    }

    // --- DIÁLOGO Y MOTOR DE GRAFICACIÓN ---

    private fun abrirGraficadorDialog(inicial: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_graficador, null)

        val etFuncionInput = view.findViewById<EditText>(R.id.etFuncionInput)
        val etXMin = view.findViewById<EditText>(R.id.etXMin)
        val etXMax = view.findViewById<EditText>(R.id.etXMax)
        val btnGenerarGrafica = view.findViewById<Button>(R.id.btnGenerarGrafica)
        val btnLimpiarGrafica = view.findViewById<Button>(R.id.btnLimpiarGrafica)
        val tvInfoGrafica = view.findViewById<TextView>(R.id.tvInfoGrafica)
        val chartGrafica = view.findViewById<LineChart>(R.id.chartGrafica)

        val btnModoManual = view.findViewById<Button>(R.id.btnModoManual)
        val btnModoHistorial = view.findViewById<Button>(R.id.btnModoHistorial)
        val btnModoActual = view.findViewById<Button>(R.id.btnModoActual)

        if (inicial.isNotBlank()) {
            etFuncionInput.setText(inicial)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Cerrar", null)
            .create()

        btnModoManual.setOnClickListener {
            etFuncionInput.setText("")
            etFuncionInput.requestFocus()
        }

        btnModoHistorial.setOnClickListener {
            dialog.dismiss()
            seleccionarDelHistorialParaGraficar()
        }

        btnModoActual.setOnClickListener {
            val exp = expresion.text.toString().trim()
            val res = resultado.text.toString().trim()
            val valor = when {
                exp.isNotBlank() -> exp
                res.isNotBlank() && res != "Resultado" && res != "Error en operación" -> res
                else -> "x*x"
            }
            etFuncionInput.setText(valor)
        }

        btnGenerarGrafica.setOnClickListener {
            sonido()
            procesarYGraficar(etFuncionInput, etXMin, etXMax, chartGrafica, tvInfoGrafica)
        }

        btnLimpiarGrafica.setOnClickListener {
            sonido()
            chartGrafica.clear()
            tvInfoGrafica.text = "Gráfica limpia."
        }

        dialog.show()

        // Graficar automáticamente si ya se ingresó una función
        if (etFuncionInput.text.isNotBlank()) {
            procesarYGraficar(etFuncionInput, etXMin, etXMax, chartGrafica, tvInfoGrafica)
        }
    }

    private fun procesarYGraficar(
        etFuncionInput: EditText,
        etXMin: EditText,
        etXMax: EditText,
        chart: LineChart,
        tvInfo: TextView
    ) {
        val entradaRaw = etFuncionInput.text.toString().trim()

        if (entradaRaw.isBlank()) {
            Toast.makeText(this, "Ingresa una función o valor", Toast.LENGTH_SHORT).show()
            return
        }

        // Detección de funciones 3D (Eje Z)
        if (entradaRaw.contains("z=") || entradaRaw.contains("z =") || (entradaRaw.contains("x") && entradaRaw.contains("y"))) {
            AlertDialog.Builder(this)
                .setTitle("Función Tridimensional 3D (Eje Z)")
                .setMessage(
                    "MPAndroidChart es una biblioteca bidimensional (2D).\n\n" +
                            "Las gráficas 3D (z = f(x, y)) requieren un motor gráfico tridimensional (OpenGL ES / Sceneview), " +
                            "funcionalidad propuesta para la Etapa 2.\n\n" +
                            "A continuación se representará el corte transversal 2D f(x)."
                )
                .setPositiveButton("Entendido", null)
                .show()
        }

        var funcionLimpia = entradaRaw
            .replace("z=", "")
            .replace("z =", "")
            .replace("y=", "")
            .replace("y =", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("–", "-")
            .replace("y", "0") // Para representar corte transversal si incluye y

        val xMinVal = etXMin.text.toString().toDoubleOrNull() ?: -10.0
        val xMaxVal = etXMax.text.toString().toDoubleOrNull() ?: 10.0

        if (xMinVal >= xMaxVal) {
            Toast.makeText(this, "El mínimo de X debe ser menor que el máximo", Toast.LENGTH_SHORT).show()
            return
        }

        // Configuración de estilo según tema
        val textColor = ContextCompat.getColor(this, R.color.axis_text)
        val gridColor = ContextCompat.getColor(this, R.color.axis_grid)
        val lineColor = ContextCompat.getColor(this, R.color.chart_line)
        val dotColor = ContextCompat.getColor(this, R.color.chart_dot)

        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.legend.textColor = textColor

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = textColor
        xAxis.gridColor = gridColor

        val axisLeft = chart.axisLeft
        axisLeft.textColor = textColor
        axisLeft.gridColor = gridColor

        val axisRight = chart.axisRight
        axisRight.isEnabled = false

        // CASO 1: La función contiene la variable 'x' -> Curva f(x)
        if (funcionLimpia.contains("x")) {
            val abreCount = funcionLimpia.count { it == '(' }
            val cierraCount = funcionLimpia.count { it == ')' }
            if (abreCount > cierraCount) {
                funcionLimpia += ")".repeat(abreCount - cierraCount)
            }

            val puntos = mutableListOf<Entry>()
            val pasos = 200
            val pasoTamano = (xMaxVal - xMinVal) / pasos

            for (i in 0..pasos) {
                val x = xMinVal + (i * pasoTamano)
                try {
                    val y = ExpressionBuilder(funcionLimpia)
                        .function(funcionSeno())
                        .function(funcionCoseno())
                        .function(funcionTangente())
                        .variables("x")
                        .build()
                        .setVariable("x", x)
                        .evaluate()

                    if (y.isFinite() && abs(y) < 10000) {
                        puntos.add(Entry(x.toFloat(), y.toFloat()))
                    }
                } catch (_: Exception) {
                }
            }

            if (puntos.isEmpty()) {
                tvInfo.text = "No se pudieron calcular puntos válidos para f(x)."
                return
            }

            val setCurva = LineDataSet(puntos, "f(x) = $entradaRaw")
            setCurva.color = lineColor
            setCurva.setDrawCircles(false)
            setCurva.lineWidth = 2.5f

            chart.data = LineData(setCurva)
            chart.invalidate()
            tvInfo.text = "Gráfica de f(x) = $entradaRaw"

        } else {
            // CASO 2: Resultado constante o número -> Referencia horizontal y punto
            try {
                val valorConstante = evaluar(funcionLimpia)

                if (!valorConstante.isFinite()) {
                    tvInfo.text = "Resultado no válido"
                    return
                }

                // Línea horizontal y = C
                val puntosLinea = mutableListOf<Entry>(
                    Entry(xMinVal.toFloat(), valorConstante.toFloat()),
                    Entry(xMaxVal.toFloat(), valorConstante.toFloat())
                )
                val setLinea = LineDataSet(puntosLinea, "Referencia horizontal (y = $valorConstante)")
                setLinea.color = lineColor
                setLinea.lineWidth = 2f
                setLinea.setDrawCircles(false)

                // Punto destacado en (0, C)
                val puntosPunto = mutableListOf<Entry>(
                    Entry(0f, valorConstante.toFloat())
                )
                val setPunto = LineDataSet(puntosPunto, "Punto (0, $valorConstante)")
                setPunto.color = dotColor
                setPunto.setCircleColor(dotColor)
                setPunto.circleRadius = 5f
                setPunto.setDrawCircleHole(false)
                setPunto.setDrawValues(true)
                setPunto.valueTextColor = textColor
                setPunto.valueTextSize = 10f

                chart.data = LineData(setLinea, setPunto)
                chart.invalidate()

                tvInfo.text = "Valor constante: y = %.4f (Punto de referencia en x=0)".format(valorConstante)

            } catch (e: Exception) {
                tvInfo.text = "Error al evaluar la expresión constante."
            }
        }
    }

    override fun onDestroy() {
        try {
            generadorSonido?.release()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}