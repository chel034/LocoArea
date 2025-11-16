package com.example.painterbudgetapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wwdablu.simplypdf.SimplyPdf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PainterBudgetScreen()
        }
    }
}

data class PdfGenerationState(
    val ownerName: String,
    val whitePaintPrice: Double,
    val colorPaintPrice: Double,
    val ceilingPrice: Double,
    val rooms: List<Room>
)

@Composable
fun PainterBudgetScreen() {
    val rooms = remember { mutableStateListOf<Room>() }
    var ownerName by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var isColor by remember { mutableStateOf(false) }
    var whitePaintPrice by remember { mutableStateOf("") }
    var colorPaintPrice by remember { mutableStateOf("") }
    var ceilingPrice by remember { mutableStateOf("") }

    var pdfGenerationState by remember { mutableStateOf<PdfGenerationState?>(null) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                pdfGenerationState?.let {
                    generatePdf(context, it.ownerName, it.whitePaintPrice, it.colorPaintPrice, it.ceilingPrice, it.rooms)
                }
            } else {
                Toast.makeText(context, "Permiso de escritura denegado", Toast.LENGTH_SHORT).show()
            }
            pdfGenerationState = null
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Nombre del Propietario") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = roomName, onValueChange = { roomName = it }, label = { Text("Nombre de la Habitación") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = width, onValueChange = { width = it }, label = { Text("Ancho") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = length, onValueChange = { length = it }, label = { Text("Largo") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = height, onValueChange = { height = it }, label = { Text("Alto") })
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pintura de Color")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = isColor, onCheckedChange = { isColor = it })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val widthDouble = width.toDoubleOrNull()
            val lengthDouble = length.toDoubleOrNull()
            val heightDouble = height.toDoubleOrNull()
            if (roomName.isNotEmpty() && widthDouble != null && lengthDouble != null && heightDouble != null) {
                rooms.add(Room(roomName, widthDouble, lengthDouble, heightDouble, isColor))
                roomName = ""
                width = ""
                length = ""
                height = ""
                isColor = false
            } else {
                Toast.makeText(context, "Por favor, introduce valores válidos", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Añadir Habitación")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Habitaciones Añadidas:")
        LazyColumn {
            items(rooms) { room ->
                Text("- ${room.name}")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextField(value = whitePaintPrice, onValueChange = { whitePaintPrice = it }, label = { Text("Precio Pintura Blanca") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = colorPaintPrice, onValueChange = { colorPaintPrice = it }, label = { Text("Precio Pintura de Color") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = ceilingPrice, onValueChange = { ceilingPrice = it }, label = { Text("Precio Techo") })
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val whitePaintPriceDouble = whitePaintPrice.toDoubleOrNull()
            val colorPaintPriceDouble = colorPaintPrice.toDoubleOrNull()
            val ceilingPriceDouble = ceilingPrice.toDoubleOrNull()
            if (whitePaintPriceDouble != null && colorPaintPriceDouble != null && ceilingPriceDouble != null) {
                val currentState = PdfGenerationState(ownerName, whitePaintPriceDouble, colorPaintPriceDouble, ceilingPriceDouble, rooms.toList())
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    pdfGenerationState = currentState
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    generatePdf(context, currentState.ownerName, currentState.whitePaintPrice, currentState.colorPaintPrice, currentState.ceilingPrice, currentState.rooms)
                }
            } else {
                Toast.makeText(context, "Por favor, introduce precios válidos", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Generar PDF")
        }
    }
}

private fun generatePdf(context: Context, ownerName: String, whitePaintPrice: Double, colorPaintPrice: Double, ceilingPrice: Double, rooms: List<Room>) {
    if (ownerName.isEmpty()) {
        Toast.makeText(context, "Por favor, introduce el nombre del propietario", Toast.LENGTH_SHORT).show()
        return
    }

    val pdfName = "presupuesto_${ownerName.replace(" ", "_")}.pdf"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, pdfName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/PainterBudgetApp")
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

    if (uri != null) {
        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                val simplyPdf = SimplyPdf.with(context, outputStream).build()

                var totalCost = 0.0

                simplyPdf.insertText("Presupuesto de Pintura para: $ownerName", 24, 0, 0, 0)
                rooms.forEach { room ->
                    val ceilingArea = room.width * room.length
                    val wallArea = 2 * (room.width + room.length) * room.height
                    val paintPrice = if (room.isColor) colorPaintPrice else whitePaintPrice
                    val ceilingCost = ceilingArea * ceilingPrice
                    val wallCost = wallArea * paintPrice
                    val roomTotal = ceilingCost + wallCost
                    totalCost += roomTotal

                    simplyPdf.insertText("Habitación: ${room.name}", 16, 0, 0, 0)
                    simplyPdf.insertText("Área del techo: $ceilingArea m²", 12, 0, 0, 0)
                    simplyPdf.insertText("Área de las paredes: $wallArea m²", 12, 0, 0, 0)
                    simplyPdf.insertText("Coste del techo: $$ceilingCost", 12, 0, 0, 0)
                    simplyPdf.insertText("Coste de las paredes: $$wallCost", 12, 0, 0, 0)
                    simplyPdf.insertText("Total de la habitación: $$roomTotal", 12, 0, 0, 0)
                    simplyPdf.insertText("", 12, 0, 0, 0)
                }

                simplyPdf.insertText("Coste total: $$totalCost", 20, 0, 0, 0)
                simplyPdf.finish()

                val toastMessage = "PDF generado en Descargas/PainterBudgetApp/$pdfName"
                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PainterBudgetScreen()
}
