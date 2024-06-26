package com.example.horsebooking.Perfil

import EditDataDialogFragment
import EditDataListener
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.horsebooking.Clases.ClasesActivity
import com.example.horsebooking.Novedades.NovedadesActivity
import com.example.horsebooking.R
import com.example.horsebooking.Reservas.ReservasActivity
import com.example.horsebooking.SinCuenta.FirebaseDB
import com.example.horsebooking.SinCuenta.IniciarSesionActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream


class PerfilUsuarioActivity : AppCompatActivity(), EditDataListener {

    private val REQUEST_CAMERA = 123
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imagen: ImageView
    private lateinit var lapiz: ImageView
    private lateinit var miStorage: StorageReference
    private lateinit var cardViewPerfil: CardView
    private lateinit var nombreUsuario: TextView
    private lateinit var apellidosUsuario: TextView
    private lateinit var correoUsuario: TextView
    private lateinit var telefonoUsuario: TextView
    private lateinit var direccionUsuario: TextView
    private var userId: String? = null
    private lateinit var auth: FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_usuario)
        bottomNavigationView = findViewById(R.id.bottom_navigation_perfil)
        bottomNavigationView.selectedItemId = R.id.menu_perfil
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener)
        lapiz = findViewById(R.id.lapiz_editar)
        imagen = findViewById(R.id.roundedImageView)
        cardViewPerfil = findViewById(R.id.cardview_perfil)
        nombreUsuario = findViewById(R.id.nombre_usuario)
        apellidosUsuario = findViewById(R.id.apellidos_usuario)
        correoUsuario = findViewById(R.id.correo_usuario)
        telefonoUsuario = findViewById(R.id.telefono_usuario)
        direccionUsuario = findViewById(R.id.direccion_usuario)
        auth = FirebaseDB.getInstanceFirebase()
        cargarImagenPerfil()

        val email = FirebaseDB.getInstanceFirebase().currentUser?.email?.replace(".", ",")
        if (email != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(email.toString())
            userRef.child("nombre").get().addOnSuccessListener { dataSnapshot ->
                val nombre = dataSnapshot.getValue(String::class.java)
                nombreUsuario.text = nombre
            }
            userRef.child("apellidos").get().addOnSuccessListener { dataSnapshot ->
                val apellidos = dataSnapshot.getValue(String::class.java)
                apellidosUsuario.text = apellidos
            }
            userRef.child("telefono").get().addOnSuccessListener { dataSnapshot ->
                val telefono = dataSnapshot.getValue(String::class.java)
                telefonoUsuario.text = telefono
            }
            userRef.child("direccion").get().addOnSuccessListener { dataSnapshot ->
                val direccion = dataSnapshot.getValue(String::class.java)
                direccionUsuario.text = direccion
            }
        } else {
            Toast.makeText(this@PerfilUsuarioActivity, "Id nulo", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            downloadImage2()
        }

        lapiz.setOnClickListener {
            mostrarDialogoElegirOrigen()
        }
        mostrarImagenGrande()
        correoUsuario.text = FirebaseDB.getInstanceFirebase().currentUser?.email
    }

    private fun mostrarImagenSeleccionada(uri: Uri?) {
        uri?.let {
            Glide.with(this)
                .load(it)
                .into(imagen)
        }
    }

    private val navListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_novedades -> {
                    startActivity(Intent(this@PerfilUsuarioActivity, NovedadesActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    finish()
                    return@OnNavigationItemSelectedListener true
                }

                R.id.menu_reservas -> {
                    startActivity(Intent(this@PerfilUsuarioActivity, ReservasActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    finish()
                    return@OnNavigationItemSelectedListener true
                }

                R.id.menu_precios -> {
                    startActivity(Intent(this@PerfilUsuarioActivity, ClasesActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    finish()
                    return@OnNavigationItemSelectedListener true
                }

                R.id.menu_perfil -> {
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    /**
     * Cambia la imagen de perfil del usuario y la carga en Firebase Storage.
     *
     * @param oldImageUrl URI de la imagen anterior.
     */
    private suspend fun changeAndUploadImage(oldImageUrl: Uri) {
        withContext(Dispatchers.IO) {
            val userId = FirebaseDB.getInstanceFirebase().currentUser?.uid

            // Descarga la imagen en un Bitmap
            val bitmap = Glide.with(this@PerfilUsuarioActivity)
                .asBitmap()
                .load(oldImageUrl)
                .submit()
                .get()

            // Comprimir el bitmap
            val compressedBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
            val baos = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
            val data = baos.toByteArray()

            // Subir la nueva imagen comprimida a Firebase Storage con el nombre del usuario
            val newImageRef = FirebaseDB.getInstanceStorage().reference.child("imagenesPerfilGente").child("$userId.jpg")
            try {
                Tasks.await(newImageRef.putBytes(data))

                // Ahora puedes usar newImageRef para mostrar la imagen en tu aplicación si es necesario
            } catch (exception: Exception) {
                println("Error al cargar la nueva imagen: ${exception.message}")
            }
        }
    }

    private fun cargarImagenPerfil() {
        val userId = FirebaseDB.getInstanceFirebase().currentUser?.uid
        if (userId != null) {
            val storageRef = FirebaseDB.getInstanceStorage().reference.child("imagenesPerfilGente/$userId.jpg")

            // Obtener la URL de descarga
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                // Usar la URI con Glide
                Glide.with(this)
                    .load(uri.toString()) // Convertir la URI a String
                    .into(imagen)
            }.addOnFailureListener {
                // Manejar el error, por ejemplo, usando una imagen predeterminada
                Glide.with(this)
                    .load(R.mipmap.img_logo)
                    .into(imagen)
            }
        } else {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * Descarga la imagen de perfil del usuario desde Firebase Storage y la muestra en la interfaz
     * de usuario. Luego, llama a la función para cambiar el nombre y cargar la imagen.
     */
    private fun downloadImage2() {
        val storageRef = FirebaseDB.getInstanceStorage().reference
        val userId = FirebaseDB.getInstanceFirebase().currentUser?.uid
        val imagesRef = storageRef.child("imagenesPerfilGente").child("$userId.jpg")

        imagesRef.downloadUrl.addOnSuccessListener { url ->
            Glide.with(this)
                .load(url)
                .into(imagen)

            // Call the function to change the name and upload the image
            runBlocking {
                changeAndUploadImage(url)
            }
        }.addOnFailureListener { exception ->
            println("Error al cargar la imagen: ${exception.message}")

            imagen.setImageResource(R.mipmap.img_logo)
        }
    }

    private fun mostrarImagenGrande() {
        cardViewPerfil.setOnClickListener {
            mostrarDialogImagen(imagen)
        }

        lapiz.setOnClickListener {
            mostrarDialogoElegirOrigen()
        }
    }

    /*/**
     * Inicializa los datos del usuario en la interfaz, mostrando información como la fecha de
     * registro, nombre, correo, experiencia, nivel y precisión.
     */
    @SuppressLint("SetTextI18n")
    private fun inicializarConBase() = runBlocking{
        fechaRegistro.text = "Se unió en " + UtilsDB.obtenerFechaActualEnTexto()
        nombreUsuarioTextView.text = UtilsDB.getNombre()
        gmailUsuarioTextView.text = UtilsDB.getCorreo()
        experienciaTextView.text = UtilsDB.getExperiencia().toString()
        nivelTextView.text= UtilsDB.getNivelMaximo()?.minus(1).toString()
        precisionTextView.text =UtilsDB.getMediaPrecisiones().toString()+"%"
    }
*/

    /**
     * Muestra un cuadro de diálogo que permite al usuario elegir entre tomar una foto con la cámara
     * o seleccionar una imagen de la galería.
     */
    private fun mostrarDialogoElegirOrigen() {
        val opciones = arrayOf("Tomar foto", "Elegir de la galería")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Seleccionar origen de la imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        // Opción "Tomar foto" seleccionada
                        requestCameraPermission()
                    }

                    1 -> {
                        // Opción "Elegir de la galería" seleccionada
                        abrirGaleria()
                    }
                }
            }

        val dialog = builder.create()
        dialog.show()
    }

    /**
     * Muestra la imagen de perfil en grande en un cuadro de diálogo al hacer clic en la imagen.
     *
     * @param imageView ImageView que se mostrará en el cuadro de diálogo.
     */
    private fun mostrarDialogImagen(imageView: ImageView) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_imagen)

        val imagenAmpliada = dialog.findViewById<ImageView>(R.id.imagenAmpliada)
        imagenAmpliada.setImageDrawable(imageView.drawable)

        // Animación de escala
        val escala = ScaleAnimation(
            0.2f,  // Escala de inicio
            1.0f,  // Escala de fin
            0.2f,  // Punto focal de inicio (X)
            0.2f,  // Punto focal  de inicio (Y)
            Animation.RELATIVE_TO_SELF, 0.5f,  // Punto focal de fin (X)
            Animation.RELATIVE_TO_SELF, 0.5f   // Punto focal de fin (Y)
        )

        escala.duration = 200  // Duración de la animación en milisegundos
        imagenAmpliada.startAnimation(escala)

        // Cierra el dialog al hacer clic en la imagen
        imagenAmpliada.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Abre la galería de imágenes para seleccionar una.
     */
    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startForActivityGallery.launch(intent)
    }

    /**
     * Callback que se ejecuta después de seleccionar una imagen desde la galería.
     * Actualiza la URI de la imagen seleccionada, muestra la imagen en un ImageView,
     * y realiza acciones como descargar la imagen y guardarla en Firebase.
     *
     * @see [startForActivityGallery]
     */
    private var selectedImageUri: Uri? = null
    val startForActivityGallery =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.data
                selectedImageUri = data
                imagen.setImageURI(data)

                // Descargar la imagen desde la URI y guardarla en un archivo
                //descargarYGuardarImagen(selectedImageUri)
                guardarImagenEnFirebase(selectedImageUri)
            }

            val preferences = getSharedPreferences("UserProfile", MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString("profileImageUri", selectedImageUri.toString())
            editor.apply()
        }

    /**
     * Guarda la imagen seleccionada de la galería en Firebase Storage.
     *
     * @param imageUri URI de la imagen seleccionada.
     */
    private fun guardarImagenEnFirebase(imageUri: Uri?) {
        if (imageUri != null) {
            val userId = FirebaseDB.getInstanceFirebase().currentUser?.uid
            if (userId != null) {
                val storageRef = FirebaseDB.getInstanceStorage().reference
                val imagesRef = storageRef.child("imagenesPerfilGente").child("$userId.jpg")

                // Subir la imagen a Firebase Storage con el nuevo nombre
                imagesRef.putFile(imageUri)
                    .addOnSuccessListener {
                        println("Éxito al subir la imagen con el nombre $userId")
                    }
                    .addOnFailureListener { exception ->
                        println("Error al subir la imagen: ${exception.message}")
                    }
            } else {
                println("El ID del usuario es nulo.")
            }
        }
    }

    /**
     * Función que maneja el resultado de la solicitud de permisos para la cámara.
     *
     * @param isGranted Indica si el permiso fue concedido o no.
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, abrir la cámara
                abrirCamara()
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }

    /**
     * Solicita permiso para acceder a la cámara y la abre si el permiso ya está concedido.
     */
    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido, abrir la cámara
                abrirCamara()
            }
            else -> {
                // Solicitar permiso para la cámara
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Sube la imagen a Firebase Storage.
     *
     * @param bitmap Imagen en formato Bitmap que se va a subir.
     */
    private fun guardarImagen(bitmap: Bitmap?) {
        val userId = FirebaseDB.getInstanceFirebase().currentUser?.uid

        if (userId != null) {
            val storageRef = FirebaseDB.getInstanceStorage().reference
            val imagesRef = storageRef.child("imagenesPerfilGente").child("$userId.jpg")

            // Guardar la imagen en Firebase Storage
            val baos = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            imagesRef.putBytes(data)
                .addOnSuccessListener {
                    println("Éxito al subir la imagen con el nombre $userId.jpg")
                }
                .addOnFailureListener { exception ->
                    println("Error al subir la imagen: ${exception.message}")
                }
        } else {
            println("El ID del usuario es nulo.")
        }
    }

    /**
     * Maneja el resultado de la actividad de la cámara para obtener una foto.
     *
     * @param requestCode Código de solicitud.
     * @param resultCode Código de resultado.
     * @param data Datos de la actividad.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap?
            imagen.setImageBitmap(imageBitmap)
            guardarImagen(imageBitmap)
            val uri = data?.data

            val filePath = uri?.lastPathSegment?.let { miStorage.child("hit").child(it) }
            if (uri != null) {
                filePath?.putFile(uri)?.addOnSuccessListener { taskSnapshot ->
                    Toast.makeText(this, "Éxito al subir el archivo", Toast.LENGTH_SHORT).show()
                }?.addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Error al subir el archivo: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Abre la cámara para capturar una foto utilizando la intención [MediaStore.ACTION_IMAGE_CAPTURE].
     */
    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    /** @author Almudena Iparraguirre Castillo
     * Función que redirige a la ventana de chat con los trabajadores
     * de la hípica
     * @param view */
    fun chateaConNosotros(view:View){

    }

    /** @author Almudena Iparraguirre Castillo
     * Función que dirige al usuario a la pantalla de cambiar
     * los datos de la cuenta
     * @param view */
    fun cambiarDatos(view: View){
        showEditDialog()
    }

    /** @author Almudena Iparraguirre Castillo
     * Función que cierra sesión en la cuenta actual
     * @param view */
    fun cerrarSesion(view: View){
        auth.signOut()
        val intent = Intent(this@PerfilUsuarioActivity, IniciarSesionActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun showEditDialog() {
        val dialog: DialogFragment = EditDataDialogFragment()
        dialog.show(supportFragmentManager, "EditDataDialogFragment")
    }

    override fun onUpdateData(username: String, email: String) {
        FirebaseDB.getInstanceFirebase().currentUser?.email?.let { updateDataInDatabase(it.replace(".",","), username, email) }
        updateEmailAndId(email.replace(".",","))
    }

    private fun updateDataInDatabase(userId: String, nombre: String, email: String) {
        if (userId.isEmpty()) {
            println("El ID del usuario es inválido.")
            return
        }

        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("usuarios/$userId")

        val updates = hashMapOf<String, Any>()
        if (nombre.isNotEmpty()) {
            updates["nombre"] = nombre
        }
        if (email.isNotEmpty()) {
            updates["email"] = email
        }

        if (updates.isNotEmpty()) {
            userRef.updateChildren(updates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Datos actualizados correctamente.")
                } else {
                    task.exception?.message?.let { error ->
                        println("Error al actualizar datos: $error")
                    }
                }
            }
        } else {
            println("No hay datos para actualizar.")
        }
    }

    private fun updateEmailAndId(newEmail: String) {
        val user = Firebase.auth.currentUser
        Log.d("USER", user.toString())
        user?.updateEmail(newEmail)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val database = Firebase.database
                    val userRef = database.getReference("usuarios/${user.uid}")

                    // Actualiza el correo electrónico en la base de datos
                    userRef.setValue(hashMapOf("email" to newEmail))
                        .addOnSuccessListener {
                            Log.d(TAG, "ID de usuario actualizado")
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Error al actualizar el ID de usuario")
                        }

                    // Crea un nuevo nodo con el nuevo ID de usuario
                    val newUid = "nuevo_id_del_usuario"
                    val newUserRef = database.getReference("usuarios/$newUid")

                    // Obtiene los datos del usuario actual
                    userRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            val userData = snapshot.value as? HashMap<String, Any>
                            if (userData != null) {
                                // Copia los datos al nuevo nodo
                                newUserRef.setValue(userData)
                                    .addOnSuccessListener {
                                        // Elimina el nodo anterior
                                        userRef.removeValue()
                                        Log.d(TAG, "Datos de usuario actualizados")
                                    }
                                    .addOnFailureListener {
                                        Log.e(TAG, "Error al copiar los datos del usuario")
                                    }
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Error al actualizar el correo electrónico")
                }
            }
    }
}