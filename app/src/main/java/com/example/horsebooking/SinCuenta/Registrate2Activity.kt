package com.example.horsebooking.SinCuenta

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.horsebooking.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class Registrate2Activity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var inputRegistroFechaNacimiento: EditText
    private lateinit var inputRegistroDireccion: EditText
    private lateinit var inputRegistroContrasena: EditText
    private lateinit var inputRegistroRepetirContrasena: EditText
    private lateinit var mensajeError: TextView
    private lateinit var storage: FirebaseStorage

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrate2)
        FirebaseApp.initializeApp(this@Registrate2Activity)
        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        inputRegistroFechaNacimiento = findViewById(R.id.inputRegistroFechaNacimiento)
        inputRegistroDireccion = findViewById(R.id.inputRegistroDireccion)
        inputRegistroContrasena = findViewById(R.id.inputRegistroContrasenaUsuario)
        inputRegistroRepetirContrasena = findViewById(R.id.inputRegistroRepetirContrasenaUsuario)
        mensajeError = findViewById(R.id.mensajeError)
    }

    /**
     * Método para establecer la foto de perfil por defecto para el usuario recién registrado.
     * @param userId El ID del usuario recién registrado.
     */
    private fun establecerFotoPerfilPorDefecto(userId: String) {
        val storageRef = storage.reference

        // Seleccionar aleatoriamente una imagen de la lista
        val imagenAleatoria = R.mipmap.img_logo

        // Nombre de la imagen por defecto en Firebase Storage
        val imagenPorDefecto = "$imagenAleatoria.jpg"
        val defaultProfileImageRef = storageRef.child("imagenesPerfilGente/$imagenPorDefecto")
        val userImageRef = storageRef.child("imagenesPerfilGente/$userId.jpg")

        // Crear un archivo temporal para descargar la imagen por defecto
        val localFile = File.createTempFile("temp_image", "jpg")

        defaultProfileImageRef.getFile(localFile)
            .addOnSuccessListener {
                userImageRef.putFile(Uri.fromFile(localFile))
                    .addOnSuccessListener {
                        guardarUrlImagenPorDefectoEnBaseDeDatos(userId)
                        Log.d(ContentValues.TAG, "Imagen de perfil predeterminada establecida para el usuario: $userId")
                    }
                    .addOnFailureListener { exception ->
                        Log.e(
                            ContentValues.TAG,
                            "Error al establecer la imagen de perfil predeterminada: ${exception.message}"
                        )
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(ContentValues.TAG, "Error al descargar la imagen por defecto: ${exception.message}")
            }
    }

    /**
     * Método para guardar la URL de la imagen por defecto en la base de datos para el usuario recién registrado.
     * @param userId El ID del usuario recién registrado.
     */
    private fun guardarUrlImagenPorDefectoEnBaseDeDatos(userId: String) {
        var file = Uri.fromFile(File("res/mipmap/fotoperfil_acordeon.png"))
        val nuevoNombre = "pruebaSubida"
        val riversRef = storage.reference.child("imagenesPerfilGente/${nuevoNombre}")
        var uploadTask = riversRef.putFile(file)

        uploadTask.addOnFailureListener {
            println("ERROR - Fallo al subir la imagen en el registro")
        }.addOnSuccessListener { taskSnapshot ->
            println("ÉXITO al subir la imagen en el registro")
        }
        Log.d(ContentValues.TAG, "URL de imagen predeterminada guardada en la base de datos para el usuario: $userId")
    }

    /**
     * @author Almudena Iparraguirre Castillo
     * Función que permite al usuario registrarse e implementa sus
     * datos en la base de datos de Firebase
     */
    fun registrarUsuarioEnFirebase() {
        val email = intent.getStringExtra("email")
        firebaseAuth.createUserWithEmailAndPassword(
            email.toString(),
            inputRegistroContrasena.text.toString()
        )
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = firebaseAuth.currentUser
                    val emailUsuario = intent.getStringExtra("email")
                    val nombre = intent.getStringExtra("nombre")
                    val apellidos = intent.getStringExtra("apellidos")
                    val telefono = intent.getStringExtra("telefono")

                    val userId = emailUsuario?.replace(".", ",")
                    userId?.let {
                        val userData = HashMap<String, Any>()
                        userData["email"] = emailUsuario.toString()
                        userData["nombre"] = nombre.toString()
                        userData["apellidos"] = apellidos.toString()
                        userData["fechaNacimiento"] = inputRegistroFechaNacimiento.text.toString()
                        userData["direccion"] = inputRegistroDireccion.text.toString()
                        userData["telefono"] = telefono.toString()
                        databaseReference.child("usuarios").child(userId).setValue(userData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Usuario registrado correctamente en Firebase",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(this@Registrate2Activity, IniciarSesionActivity::class.java)
                                startActivity(intent)
                                establecerFotoPerfilPorDefecto(userId)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "FirebaseDatabase",
                                    "Error al registrar el usuario en Firebase",
                                    e
                                )
                                Toast.makeText(
                                    this,
                                    "Error al registrar el usuario en Firebase",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Error al registrar usuario: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("FirebaseAuth", "Error al registrar usuario", task.exception)
                }
            }
    }

    /**
     * @author Almudena Iparraguirre Castillo
     * Función que valida los campos del registro introducidos por el usuario
     * @param view */
    fun comprobarCampos(view: View){
        // Validar que todos los campos estén completos
        if (inputRegistroFechaNacimiento.text.isEmpty() || inputRegistroDireccion.text.isEmpty()
            || inputRegistroContrasena.text.isEmpty() || inputRegistroRepetirContrasena.text.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            mensajeError.text = "Por favor, completa todos los campos"
            return
        }

        // Validar que las contraseñas coincidan
        val contraseña = inputRegistroContrasena.text.toString()
        val repetirContraseña = inputRegistroRepetirContrasena.text.toString()
        if (contraseña != repetirContraseña) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            mensajeError.text = "Las contraseñas no coinciden"
            return
        }

        // Validar la longitud y composición de la contraseña
        if (!validarContraseña(contraseña)) {
            Toast.makeText(
                this,
                "La contraseña debe tener al menos 8 caracteres, 1 minúscula, 1 mayúscula y 1 número",
                Toast.LENGTH_SHORT
            ).show()
            mensajeError.text = "La contraseña debe tener al menos 8 caracteres, 1 minúscula, 1 mayúscula y 1 número"
            return
        }


        // Llamar a una función para registrar al usuario en Firebase
        registrarUsuarioEnFirebase()
    }

    /**
     * @author Almudena Iparraguirre Castillo
     * Función que valida la contraseña introducida por el usuario
     * @param contraseña
     * @return Boolean */
    fun validarContraseña(contraseña: String): Boolean {
        val regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$")
        return regex.matches(contraseña)
    }

    /**
     * @author Almudena Iparraguirre Castillo
     * Función que vuelve a la primera página de registro
     * @param view */
    fun anteriorPaginaRegistro(view: View){
        val intent = Intent(this@Registrate2Activity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}