package com.reftgres.taihelper.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConnectivityService @Inject constructor(
    private val context: Context
) {
    // Тег для логирования
    private val TAG = "NetworkConnectivity"

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // StateFlow для хранения состояния соединения
    private val _networkStatus = MutableStateFlow(isNetworkAvailable())
    val networkStatus: StateFlow<Boolean> = _networkStatus

    // Сохраняем ссылку на callback для возможности отмены регистрации
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        Log.d(TAG, "Инициализация NetworkConnectivityService")
        Log.d(TAG, "Начальное состояние сети: ${_networkStatus.value}")
        setupNetworkCallback()
    }


    private fun setupNetworkCallback() {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "onAvailable: Сеть стала доступна")
                _networkStatus.value = true
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "onLost: Сеть потеряна")
                _networkStatus.value = false
            }

            override fun onUnavailable() {
                Log.d(TAG, "onUnavailable: Сеть недоступна")
                _networkStatus.value = false
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                Log.d(TAG, "onCapabilitiesChanged: hasInternet=$hasInternet")

                if (hasInternet) {
                    _networkStatus.value = true
                }
            }
        }

        // Сохраняем ссылку на callback
        networkCallback = callback

        // Создаем запрос на отслеживание соединения
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            // Регистрируем callback
            connectivityManager.registerNetworkCallback(networkRequest, callback)
            Log.d(TAG, "NetworkCallback успешно зарегистрирован")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при регистрации NetworkCallback: ${e.message}")
        }

        // Обновляем текущее состояние
        val currentState = isNetworkAvailable()
        if (currentState != _networkStatus.value) {
            Log.d(TAG, "Обновляем начальное состояние сети: $currentState")
            _networkStatus.value = currentState
        }
    }

    // Проверка доступности интернета на устройстве
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        if (network != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            Log.d(TAG, "isNetworkAvailable: $hasInternet")
            return hasInternet
        }

        Log.d(TAG, "isNetworkAvailable: false (нет активной сети)")
        return false
    }

    // Метод для остановки мониторинга
    fun stopMonitoring() {
        networkCallback?.let {
            try {
                Log.d(TAG, "Отмена регистрации NetworkCallback")
                connectivityManager.unregisterNetworkCallback(it)
                networkCallback = null
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отмене регистрации NetworkCallback: ${e.message}")
            }
        }
    }
}