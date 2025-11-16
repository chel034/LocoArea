package com.example.painterbudgetapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.painterbudgetapp.databinding.ActivityMainBinding
import com.wwdablu.simplypdf.SimplyPdf
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val rooms = mutableListOf<Room>()
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addRoomButton.setOnClickListener {
            addRoom()
        }

        binding.generatePdfButton.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE
                )
            } else {
                generatePdf()
            }
        }
    }

    private fun addRoom() {
        val roomName = binding.roomNameEditText.text.toString()
        val width = binding.widthEditText.text.toString().toDoubleOrNull()
        val length = binding.lengthEditText.text.toString().toDoubleOrNull()
        val height = binding.heightEditText.text.toString().toDoubleOrNull()
        val isColor = binding.colorSwitch.isChecked

        if (roomName.isNotEmpty() && width != null && length != null && height != null) {
            val room = Room(roomName, width, length, height, isColor)
            rooms.add(room)
            updateRoomsTextView()
            clearInputFields()
        } else {
            Toast.makeText(this, getString(R.string.please_fill_all_fields_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRoomsTextView() {
        val roomsText = StringBuilder(getString(R.string.added_rooms_label) + "\n")
        rooms.forEach { room ->
            roomsText.append("- ${room.name}\n")
        }
        binding.roomsTextView.text = roomsText.toString()
    }

    private fun clearInputFields() {
        binding.roomNameEditText.text.clear()
        binding.widthEditText.text.clear()
        binding.lengthEditText.text.clear()
        binding.heightEditText.text.clear()
        binding.colorSwitch.isChecked = false
    }

    private fun generatePdf() {
        val ownerName = binding.ownerNameEditText.text.toString()
        if (ownerName.isEmpty()) {
            Toast.makeText(this, "Por favor, introduce el nombre del propietario", Toast.LENGTH_SHORT).show()
            return
        }

        val whitePaintPrice = binding.whitePaintPriceEditText.text.toString().toDoubleOrNull()
        val colorPaintPrice = binding.colorPaintPriceEditText.text.toString().toDoubleOrNull()
        val ceilingPrice = binding.ceilingPriceEditText.text.toString().toDoubleOrNull()

        if (whitePaintPrice == null || colorPaintPrice == null || ceilingPrice == null) {
            Toast.makeText(this, "Por favor, introduce todos los precios", Toast.LENGTH_SHORT).show()
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

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    val simplyPdf = SimplyPdf.with(this, outputStream)
                        .build()

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
                        simplyPdf.insertText("", 12, 0, 0, 0) // Add a blank line
                    }

                    simplyPdf.insertText("Coste total: $$totalCost", 20, 0, 0, 0)
                    simplyPdf.finish()

                    val toastMessage = "PDF generado en Descargas/PainterBudgetApp/$pdfName"
                    Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePdf()
            } else {
                Toast.makeText(this, getString(R.string.write_permission_denied_toast), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
