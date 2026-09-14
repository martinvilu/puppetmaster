package com.sounddeck.core.obs

import android.util.Base64
import android.util.Log
import com.sounddeck.core.model.ObsConfig
import com.sounddeck.core.model.ObsConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

class ObsManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val tag = "ObsManager"

    private val json = Json { ignoreUnknownKeys = true }

    private val _connectionState = MutableStateFlow(ObsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ObsConnectionState> = _connectionState.asStateFlow()

    private val _lastMessage = MutableStateFlow("")
    val lastMessage: StateFlow<String> = _lastMessage.asStateFlow()

    private val _streamStats = MutableStateFlow(com.sounddeck.core.model.ObsStreamStats())
    val streamStats: StateFlow<com.sounddeck.core.model.ObsStreamStats> = _streamStats.asStateFlow()

    private val _scenesList = MutableStateFlow<List<String>>(emptyList())
    val scenesList: StateFlow<List<String>> = _scenesList.asStateFlow()

    private val _inputsList = MutableStateFlow<List<String>>(emptyList())
    val inputsList: StateFlow<List<String>> = _inputsList.asStateFlow()

    private val _inputMuteStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val inputMuteStates: StateFlow<Map<String, Boolean>> = _inputMuteStates.asStateFlow()

    private val _inputVolumeLevels = MutableStateFlow<Map<String, Float>>(emptyMap())
    val inputVolumeLevels: StateFlow<Map<String, Float>> = _inputVolumeLevels.asStateFlow()

    private val _sceneItemStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val sceneItemStates: StateFlow<Map<String, Boolean>> = _sceneItemStates.asStateFlow()

    private val _currentScene = MutableStateFlow("")
    val currentScene: StateFlow<String> = _currentScene.asStateFlow()

    private var activeConfig: ObsConfig? = null
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var statsPollJob: Job? = null
    private var reconnectAttempts = 0

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    fun connect(config: ObsConfig) {
        activeConfig = config
        reconnectJob?.cancel()
        reconnectAttempts = 0
        doConnect(config)
    }

    fun reconnect() {
        activeConfig?.let { connect(it) }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        statsPollJob?.cancel()
        activeConfig = null
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ObsConnectionState.DISCONNECTED
        _lastMessage.value = "Disconnected"
    }

    private fun doConnect(config: ObsConfig) {
        webSocket?.cancel()
        _connectionState.value = ObsConnectionState.CONNECTING
        _lastMessage.value = "Connecting to ws://${config.host}:${config.port}..."

        val wsUrl = if (config.host.startsWith("ws://") || config.host.startsWith("wss://")) {
            config.host
        } else {
            "ws://${config.host}:${config.port}"
        }

        val request = try {
            Request.Builder().url(wsUrl).build()
        } catch (e: Exception) {
            _connectionState.value = ObsConnectionState.DISCONNECTED
            _lastMessage.value = "Invalid URL: ${e.message}"
            return
        }

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket open, waiting for Hello (Op 0)...")
                _lastMessage.value = "Connection opened, handshaking..."
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text, config)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket failure: ${t.message}")
                _connectionState.value = ObsConnectionState.DISCONNECTED
                _lastMessage.value = "Error: ${t.localizedMessage ?: "Connection failed"}"
                scheduleReconnect(config)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closed ($code): $reason")
                _connectionState.value = ObsConnectionState.DISCONNECTED
                _lastMessage.value = "Closed ($code)"
                scheduleReconnect(config)
            }
        })
    }

    private fun handleIncomingMessage(text: String, config: ObsConfig) {
        try {
            val root = json.parseToJsonElement(text).jsonObject
            val op = root["op"]?.jsonPrimitive?.int ?: return
            val d = root["d"]?.jsonObject ?: JsonObject(emptyMap())

            when (op) {
                0 -> {
                    // OpCode 0: Hello
                    handleHello(d, config)
                }
                2 -> {
                    // OpCode 2: Identified
                    _connectionState.value = ObsConnectionState.AUTHENTICATED
                    _lastMessage.value = "Authenticated with OBS"
                    reconnectAttempts = 0
                    Log.i(tag, "Successfully identified with OBS WebSocket v5")
                    startStatsPolling()
                }
                7 -> {
                    // OpCode 7: RequestResponse
                    val reqType = d["requestType"]?.jsonPrimitive?.content ?: "Request"
                    val requestStatus = d["requestStatus"]?.jsonObject
                    val result = requestStatus?.get("result")?.jsonPrimitive?.content ?: "OK"
                    _lastMessage.value = "$reqType: $result"
                    val responseData = d["responseData"]?.jsonObject
                    if (responseData != null) {
                        handleResponseData(reqType, responseData)
                    }
                }
                5 -> {
                    // OpCode 5: Event
                    val eventType = d["eventType"]?.jsonPrimitive?.content
                    val eventData = d["eventData"]?.jsonObject
                    if (eventType != null) {
                        if (eventType != "InputVolumeMeters") {
                            _lastMessage.value = "Event: $eventType"
                        }
                        handleEvent(eventType, eventData)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing OBS message: ${e.message}", e)
        }
    }

    private fun handleResponseData(reqType: String, data: JsonObject) {
        when (reqType) {
            "GetSceneList" -> {
                val current = data["currentProgramSceneName"]?.jsonPrimitive?.content ?: ""
                if (current.isNotEmpty()) _currentScene.value = current
                val scenesArr = data["scenes"]
                if (scenesArr is kotlinx.serialization.json.JsonArray) {
                    val names = scenesArr.mapNotNull {
                        it.jsonObject["sceneName"]?.jsonPrimitive?.content
                    }
                    if (names.isNotEmpty()) _scenesList.value = names
                }
            }
            "GetInputList" -> {
                val inputsArr = data["inputs"]
                if (inputsArr is kotlinx.serialization.json.JsonArray) {
                    val names = inputsArr.mapNotNull {
                        it.jsonObject["inputName"]?.jsonPrimitive?.content
                    }
                    if (names.isNotEmpty()) _inputsList.value = names
                }
            }
            "GetStreamStatus" -> {
                val isStreaming = data["outputActive"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                val timecode = data["outputTimecode"]?.jsonPrimitive?.content ?: ""
                val bytes = data["outputBytes"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                _streamStats.value = _streamStats.value.copy(
                    isStreaming = isStreaming,
                    outputTimecode = timecode,
                    outputBytes = bytes
                )
            }
            "GetRecordStatus" -> {
                val isRecording = data["outputActive"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                _streamStats.value = _streamStats.value.copy(isRecording = isRecording)
            }
            "GetStats" -> {
                val fps = data["activeFps"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                val cpu = data["cpuUsage"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                val dropped = data["outputSkippedFrames"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val total = data["outputTotalFrames"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                _streamStats.value = _streamStats.value.copy(
                    fps = fps,
                    cpuUsage = cpu,
                    droppedFrames = dropped,
                    totalFrames = total
                )
            }
        }
    }

    private fun handleEvent(eventType: String, data: JsonObject?) {
        if (data == null) return
        when (eventType) {
            "CurrentProgramSceneChanged" -> {
                val sceneName = data["sceneName"]?.jsonPrimitive?.content ?: ""
                _currentScene.value = sceneName
            }
            "SceneListChanged", "SceneCreated", "SceneRemoved", "SceneNameChanged" -> {
                sendRequest("GetSceneList")
            }
            "InputListChanged", "InputCreated", "InputRemoved", "InputNameChanged" -> {
                sendRequest("GetInputList")
            }
            "InputMuteStateChanged" -> {
                val inputName = data["inputName"]?.jsonPrimitive?.content ?: ""
                val muted = data["inputMuted"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                val m = _inputMuteStates.value.toMutableMap()
                m[inputName] = muted
                _inputMuteStates.value = m
            }
            "InputVolumeMeters" -> {
                // Audio VU meters event: { inputs: [ { inputName: "Mic", inputLevelsMul: [[peak, mag, inputPeak]] } ] }
                val inputs = data["inputs"]
                if (inputs is kotlinx.serialization.json.JsonArray) {
                    val levelsMap = _inputVolumeLevels.value.toMutableMap()
                    for (item in inputs) {
                        val obj = item.jsonObject
                        val name = obj["inputName"]?.jsonPrimitive?.content ?: continue
                        val levelsArr = obj["inputLevelsMul"]
                        var peak = 0.0f
                        if (levelsArr is kotlinx.serialization.json.JsonArray && levelsArr.isNotEmpty()) {
                            val firstCh = levelsArr[0]
                            if (firstCh is kotlinx.serialization.json.JsonArray && firstCh.isNotEmpty()) {
                                peak = firstCh[0].jsonPrimitive.content.toDoubleOrNull()?.toFloat() ?: 0.0f
                            } else {
                                peak = firstCh.jsonPrimitive.content.toDoubleOrNull()?.toFloat() ?: 0.0f
                            }
                        }
                        levelsMap[name] = peak.coerceIn(0.0f, 1.0f)
                    }
                    _inputVolumeLevels.value = levelsMap
                }
            }
            "SceneItemEnableStateChanged" -> {
                val sceneName = data["sceneName"]?.jsonPrimitive?.content ?: ""
                val itemId = data["sceneItemId"]?.jsonPrimitive?.content ?: ""
                val enabled = data["sceneItemEnabled"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
                val key = "$sceneName:$itemId"
                val m = _sceneItemStates.value.toMutableMap()
                m[key] = enabled
                _sceneItemStates.value = m
            }
            "StreamStateChanged" -> {
                val active = data["outputActive"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                _streamStats.value = _streamStats.value.copy(isStreaming = active)
            }
            "RecordStateChanged" -> {
                val active = data["outputActive"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                _streamStats.value = _streamStats.value.copy(isRecording = active)
            }
        }
    }

    private fun startStatsPolling() {
        statsPollJob?.cancel()
        statsPollJob = scope.launch {
            sendRequest("GetSceneList")
            sendRequest("GetInputList")
            var tick = 0
            while (_connectionState.value == ObsConnectionState.AUTHENTICATED) {
                sendRequest("GetStreamStatus")
                sendRequest("GetRecordStatus")
                sendRequest("GetStats")
                tick++
                if (tick % 5 == 0) {
                    sendRequest("GetSceneList")
                    sendRequest("GetInputList")
                }
                delay(2000L)
            }
        }
    }

    private fun handleHello(d: JsonObject, config: ObsConfig) {
        val authObj = d["authentication"]?.jsonObject
        val identifyPayload = if (authObj != null) {
            val challenge = authObj["challenge"]?.jsonPrimitive?.content ?: ""
            val salt = authObj["salt"]?.jsonPrimitive?.content ?: ""
            val authResponse = generateAuthResponse(config.password, salt, challenge)

            buildJsonObject {
                put("op", 1)
                put("d", buildJsonObject {
                    put("rpcVersion", 1)
                    put("authentication", authResponse)
                    put("eventSubscriptions", 1069) // General(1) + Scenes(4) + Inputs(8) + Outputs(32) + InputVolumeMeters(1024)
                })
            }
        } else {
            buildJsonObject {
                put("op", 1)
                put("d", buildJsonObject {
                    put("rpcVersion", 1)
                    put("eventSubscriptions", 1069)
                })
            }
        }

        webSocket?.send(identifyPayload.toString())
    }

    /**
     * OBS v5 SHA-256 Authentication:
     * secret = Base64(SHA-256(password + salt))
     * authResponse = Base64(SHA-256(secret + challenge))
     */
    private fun generateAuthResponse(password: String, salt: String, challenge: String): String {
        val md = MessageDigest.getInstance("SHA-256")

        // Step 1: secret = Base64(SHA-256(password + salt))
        val passPlusSalt = (password + salt).toByteArray(StandardCharsets.UTF_8)
        val secretBytes = md.digest(passPlusSalt)
        val secretBase64 = Base64.encodeToString(secretBytes, Base64.NO_WRAP)

        // Step 2: authResponse = Base64(SHA-256(secret + challenge))
        val secretPlusChallenge = (secretBase64 + challenge).toByteArray(StandardCharsets.UTF_8)
        val authBytes = md.digest(secretPlusChallenge)
        return Base64.encodeToString(authBytes, Base64.NO_WRAP)
    }

    private fun scheduleReconnect(config: ObsConfig) {
        if (!config.autoReconnect) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts++
            val backoffMs = (config.reconnectIntervalMs * (1 shl (reconnectAttempts - 1).coerceAtMost(4)))
                .coerceIn(1000L, 30000L)
            _connectionState.value = ObsConnectionState.CONNECTING
            _lastMessage.value = "Reconnecting in ${backoffMs / 1000}s (attempt $reconnectAttempts)..."
            delay(backoffMs)
            doConnect(config)
        }
    }

    /**
     * Sends an OpCode 6 Request to OBS WebSocket v5.
     */
    fun sendRequest(requestType: String, requestData: JsonObject? = null) {
        if (_connectionState.value != ObsConnectionState.AUTHENTICATED) {
            Log.w(tag, "Cannot send $requestType: OBS not authenticated")
            _lastMessage.value = "Cannot send $requestType: Disconnected"
            return
        }

        val requestId = UUID.randomUUID().toString()
        val payload = buildJsonObject {
            put("op", 6)
            put("d", buildJsonObject {
                put("requestType", requestType)
                put("requestId", requestId)
                if (requestData != null) {
                    put("requestData", requestData)
                }
            })
        }
        webSocket?.send(payload.toString())
    }

    fun setCurrentProgramScene(sceneName: String) {
        sendRequest(
            requestType = "SetCurrentProgramScene",
            requestData = buildJsonObject { put("sceneName", sceneName) }
        )
    }

    fun toggleInputMute(inputName: String) {
        sendRequest(
            requestType = "ToggleInputMute",
            requestData = buildJsonObject { put("inputName", inputName) }
        )
    }

    fun setInputMute(inputName: String, muted: Boolean) {
        sendRequest(
            requestType = "SetInputMute",
            requestData = buildJsonObject {
                put("inputName", inputName)
                put("inputMuted", muted)
            }
        )
    }

    fun toggleRecord() {
        sendRequest("ToggleRecord")
    }

    fun toggleStream() {
        sendRequest("ToggleStream")
    }

    fun setSceneItemEnabled(sceneName: String, sceneItemId: Int, enabled: Boolean) {
        sendRequest(
            requestType = "SetSceneItemEnabled",
            requestData = buildJsonObject {
                put("sceneName", sceneName)
                put("sceneItemId", sceneItemId)
                put("sceneItemEnabled", enabled)
            }
        )
    }

    fun saveReplayBuffer() {
        sendRequest("SaveReplayBuffer")
    }
}
