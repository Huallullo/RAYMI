package com.raymi.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.raymi.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ejemplo: mensaje de bienvenida o dashboard
        binding.textViewWelcome.text = "Has iniciado sesión correctamente 🎉"
    }
}
