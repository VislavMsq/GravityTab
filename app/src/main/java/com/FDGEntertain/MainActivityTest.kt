package com.FDGEntertain

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.os.RemoteException
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.FDGEntertain.databinding.ActivityMainBinding
import com.FDGEntertain.repository.WebViewLifecycleDelegate
import com.FDGEntertain.util.LogoAnimator
import com.FDGEntertain.wv.SimpleWebViewLifecycleDelegate
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * MainActivity — "дирижёр" старта приложения и загрузки WebView.
 *
 * 1) onCreate() -> setupUi() -> setupBackNavigation() -> requestPostNotifications()
 * 2) postNotificationsResultLauncher -> startInstallReferrerFlow()
 * 3) CustomReferrerStateListener -> openWebViewFlow(refer)
 * 4) openWebViewFlow: FCM token (асинхронно) + getAdIdSafe() (IO) -> initWebView() -> loadUrl()
 * 5) Далее живём внутри WebView-клиентов (прогресс, новые окна, камера, выбор файла, навигация назад).
 */
class MainActivityTest : AppCompatActivity() {
    // ---------- Поля состояния и ссылки на UI ----------

    lateinit var binding: ActivityMainBinding

    /**
     * Стек открытых WebView.
     * В корне у нас один rootWebView из layout.
     * Когда страница открывает target="_blank" — создаём новый WebView, кладём сюда
     * и управляем back-навигацией и закрытием.
     */
    private val webViews = mutableListOf<WebView>()

    // Install Referrer (источник установки) — приходит из Play Store через сервис
    private var installReferer: String? = null
    private lateinit var installReferrerClient: InstallReferrerClient

    // Временные переменные под запрос камеры/выбор файла (их колбэки приходят позже)
    private lateinit var pendingCameraRequest: PermissionRequest
    private var pendingFileChooser: ValueCallback<Array<Uri>>? = null
    private lateinit var logoAnimator: LogoAnimator
    private lateinit var webViewLifecycle: WebViewLifecycleDelegate


    // =====================================================================================
    // 1) Точка входа. Вызывается системой при создании Activity (главный UI-поток).
    // =====================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupUi()                   // (UI) Инфлейтим layout, настраиваем отступы, показываем лоадер
        setupBackNavigation()       // (UI) Определяем поведение системной кнопки "назад"
        requestPostNotifications()  // (UI -> асинхронно) Запрашиваем разрешение на уведомления.
        webViewLifecycle = SimpleWebViewLifecycleDelegate(webViews)
        // После ответа ОС вызовет postNotificationsResultLauncher (см. ниже),
        // где мы стартуем Install Referrer.

        logoAnimator = LogoAnimator(binding.logoImage)
        logoAnimator.start()
    }

    // =====================================================================================
    // 2) Чистая настройка UI. Ничего асинхронного. Выполняется в onCreate на UI-потоке.
    // =====================================================================================
    private fun setupUi() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge() // рисуем под системными барами

        // Настраиваем корректные отступы под клавиатуру и системные бары
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()) // клавиатура и другой ввод
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars()) // навигация и бары
            v.setPadding(
                sys.left, sys.top, sys.right,
                kotlin.comparisons.maxOf(ime.bottom, sys.bottom)
            )
            insets
        }
        setContentView(binding.root)

        // Изначально показываем экран загрузки, сам WebView прячем
        binding.rootWebView.isVisible = false
        binding.circleProgressBarLayout.isVisible = true // индикатор загрузки
    }

    // =====================================================================================
    // 3) Первый "триггер" асинхронного флоу — запрос нотификаций.
    // ОС покажет системный диалог, результат придёт в postNotificationsResultLauncher.
    // =====================================================================================
    private fun requestPostNotifications() {
        postNotificationsResultLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
    }

    /**
     * 3.1) Колбэк разрешений на уведомления.
     * Вызывается ОС после того, как пользователь ответит (разрешил/запретил/пропустил).
     * Независимо от ответа — мы запускаем процесс получения Install Referrer.
     */
    private val postNotificationsResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Мы дальше не читаем конкретный результат — наша логика не критична к разрешению.
            startInstallReferrerFlow()
        }

    // =====================================================================================
    // 4) Стартуем установку соединения с сервисом Install Referrer (Play Store).
    // После установления связи будет вызван onInstallReferrerSetupFinished(...)
    // =====================================================================================
    private fun startInstallReferrerFlow() {
        installReferrerClient = InstallReferrerClient.newBuilder(this).build()
        installReferrerClient.startConnection(object : InstallReferrerStateListener {

            /**
             * 4.1) Этот колбэк приходит от сервиса Play Store, когда он готов отдать referrer.
             * Поток: Binder/фоновый, но мы тут делаем минимум и уходим дальше в наш UI-флоу.
             */

            override fun onInstallReferrerSetupFinished(code: Int) {
                if (code == InstallReferrerClient.InstallReferrerResponse.OK) {
                    try {
                        if (installReferer == null) {
                            // Достаём строку реферера (источник установки).
                            installReferer = installReferrerClient.installReferrer.installReferrer

                            // 5) Как только получили реферер — переходим к сбору остальных параметров
                            // и подготовке WebView. Следующий шаг — openWebViewFlow(refer).
                            openWebViewFlow(installReferer!!)
                        }
                    } catch (_: RemoteException) {
                        // Если сервис упал — просто игнорируем, можно добавить retry/лог.
                    }
                }
                // Иные коды ответа можно обработать логами/метриками (не критично для базового флоу).
            }

            /**
             * 4.2) Сервис отвалился — можно переподключиться. Это редкий кейс.
             */
            override fun onInstallReferrerServiceDisconnected() {
                installReferrerClient.startConnection(this)
            }
        })
    }

    // =====================================================================================
    // 5) Главный "оркестратор" подготовки данных для URL и показа WebView.
    // Здесь совмещаем два источника асинхронных данных:
    //  - FCM токен (Task из Firebase, придёт позже)
    //  - Ad Id (получаем на IO-потоке; может занять время)
    // После — конфигурируем WebView и делаем loadUrl().
    // =====================================================================================
    private fun openWebViewFlow(refer: String) {
        // 5.1) Запрашиваем FCM токен (Firebase сам решит кэш/обновление). Это асинхронный Task.
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tusk ->
            val frbToken = if (tusk.isSuccessful) tusk.result ?: "" else ""
            // 5.2) Параллельно на IO-потоке получаем AdId (может сходить к Play Services).
            lifecycleScope.launch(Dispatchers.IO) {
                val adId = getAdIdSafe() // блокирующая операция, поэтому IO

                // 5.3) Возвращаемся в UI-поток, где можно трогать View.
                withContext(Dispatchers.Main) {
                    // Конфигурируем корневой WebView (клиенты, куки, настройки).
                    initWebView(binding.rootWebView)

                    // 5.4) Собираем URL с параметрами, необходимыми бэкенду.
                    val url = buildUrl(
                        baseUrl = "https://keqe05h98f.execute-api.eu-north-1.amazonaws.com/",
                        params = mapOf(
                            "flipAdNest" to adId,
                            "matchReferCode" to refer,
                            "wingFrbToken" to frbToken,
                            "comboEggPack" to packageName
                        )
                    )
                    // 5.5) Грузим страницу. С этого момента WebView начинает свой жизненный цикл.
                    binding.rootWebView.loadUrl(url)

                    // 5.6) Показываем WebView и убираем лоадер.
                    binding.rootWebView.isVisible = true
                    binding.circleProgressBarLayout.isVisible = false // индикатор загрузки
                }
            }
        }
    }

    // =====================================================================================
    // 6) Единая точка конфигурации WebView.
    // Вызывается каждый раз, когда создаём новый WebView (root и новые окна).
    // =====================================================================================
    @SuppressLint("RequiresFeature")
    private fun initWebView(view: WebView) {
        // --- Куки: для нормальной работы множества сайтов и авторизаций ---
        CookieManager.getInstance()
            .setAcceptThirdPartyCookies(view, true) // чтобы сохранялись куки во влоэенный сервисах
        CookieManager.getInstance().setAcceptCookie(true)

        // --- Загрузки файлов (download attribute, прямые ссылки и т.д.) -> во внешние приложения ---
        view.setDownloadListener { url, _, _, _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // --- WebChromeClient = расширенные фичи браузера: прогресс, разрешения, новые окна, диалоги, консоль ---
        view.webChromeClient = object :
            WebChromeClient() { // обрабатывает события, которые происходят после начала загрузки веб-страницы

            /**
             * 6.1) Прогресс загрузки страницы (0..100). Вызывается движком WebView на UI-потоке.
             * Мы обновляем полоску прогресса и скрываем её на 100%.
             */
            override fun onProgressChanged(v: WebView?, p: Int) {
                binding.horizontalProgressBar.progress = p
                binding.horizontalProgressBar.isVisible = p < 100
            }

            /**
             * 6.2) Сообщения из JS-консоли страницы.
             * Если фронт пишет console.log("openActivity"), мы интерпретируем это как команду
             * открыть игровую активность и закрыть текущую.
             */
            override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                if (msg?.message() == "openFlipBoard") {
                    startActivity(
                        Intent(
                            this@MainActivityTest,
                            GameActivity::class.java
                        )
                    )  //объект, который описывает операцию, которую нужно выполнить
                    finish()
                }
                return true // true = "мы обработали сообщение"
            }

            /**
             * 6.3) Страница запросила доступ к камере/микрофону и т.п.
             * Если CAMERA уже дан — сразу grant(); иначе запрашиваем у ОС и продолжим в cameraResultLauncher.
             */
            override fun onPermissionRequest(request: PermissionRequest) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivityTest,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    request.grant(request.resources)
                } else {
                    pendingCameraRequest = request
                    // ОС покажет системный диалог, результат придёт в cameraResultLauncher ниже
                    cameraResultLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            /**
             * 6.4) Страница хочет открыть новое окно (target="_blank" или window.open).
             * Мы создаём новый WebView, настраиваем его, добавляем на экран и в стек.
             * WebViewTransport — "труба", через которую сообщаем движку, что новое окно готово.
             */
            override fun onCreateWindow(
                v: WebView, // wv откуда пришел запрос
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val newWv = WebView(this@MainActivityTest)
                initWebView(newWv)                 // повторяем полную конфигурацию
                binding.root.addView(newWv)        // добавляем физически в иерархию View
                webViews.add(newWv)                // кладём на вершину стека

                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = newWv
                resultMsg?.sendToTarget()
                return true
            }

            /**
             * 6.5) Страница открыла системный диалог выбора файла (input type=file).
             * Мы запускаем системный файловый пикер. Результат вернём в pendingFileChooser.
             */
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // Если вдруг висел старый колбэк — аккуратно "обнулим" его
                pendingFileChooser?.onReceiveValue(null)
                pendingFileChooser = filePathCallback
                return try {
                    // ОС откроет UI выбора файла. Результат придёт в fileChooserResultLauncher.
                    fileChooserResultLauncher.launch(fileChooserParams?.createIntent()!!)
                    true
                } catch (_: ActivityNotFoundException) {
                    // Если нет подходящей Activity — корректно вернём null и сбросим ссылку
                    pendingFileChooser?.onReceiveValue(null)
                    pendingFileChooser = null
                    false
                }
            }
        }

        // --- WebViewClient = навигация внутри WebView (перехват ссылок, ошибки загрузки и т.д.) ---
        view.webViewClient = object : WebViewClient() {

            /**
             * 6.6) Навигация по URL.
             * Если схема http/https — оставляем WebView обрабатывать страницу (return false).
             * Если нет (intent://, market:// и пр.) — пробуем отдать наружу в систему через Intent.
             */
            override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
                val s = req.url.toString()
                if (s.startsWith("http")) return false // пусть WebView сам загрузит
                return try {
                    startActivity(Intent.parseUri(s, Intent.URI_INTENT_SCHEME))
                    true // сказали WebView "мы обработали, не трогай"
                } catch (_: Exception) {
                    true // тоже не отдаём WebView: наверняка не должен грузить такие схемы
                }
            }
        }
        // --- Производительность/UX настройки самого виджета ---
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        with(view.settings) {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            userAgentString = userAgentString.replace("; wv", "")
            mixedContentMode = 0
            mediaPlaybackRequiresUserGesture = false
            builtInZoomControls = true
            displayZoomControls = false
            domStorageEnabled = true
            setSupportMultipleWindows(true)
        }
        // Если это корневой WebView и он ещё не в стеке — положим.
        if (webViews.contains(view)) webViews.add(view)
    }

    // =====================================================================================
    // 7) Вспомогательные: сборка URL и безопасное получение Ad ID.
    // =====================================================================================

    /**
     * 7.1) Простой билдер query-строки без percent-encoding.
     * ВАЖНО: если на бэке ждут "сырые" значения (например, referer с '&'), не кодируем.
     * Если потребуется кодирование — делай точечно по конкретным ключам.
     */
    private fun buildUrl(baseUrl: String, params: Map<String, String>): String {
        val qs = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        return if (baseUrl.contains("?")) "$baseUrl&$qs" else "$baseUrl?$qs"
    }

    /**
     * 7.2) Получение Advertising ID:
     *  - сперва пытаемся взять из SharedPreferences (кеш)
     *  - иначе просим у Play Services (может бросать исключения)
     *  - если пришёл нулевой ID — генерим случайный и кешируем
     *
     * Вызывается на IO-потоке (см. openWebViewFlow) — т.к. может блокировать.
     */
    private fun getAdIdSafe(): String {
        // сперва пытаемся взять из SharedPreferences (кеш)
        val prefs = getSharedPreferences(packageName, MODE_PRIVATE)
        val cached = prefs.getString("adId", null)
        if (cached != null) return cached

        // просим у Play Services (может бросать исключения)
        val fresh = try {
            AdvertisingIdClient.getAdvertisingIdInfo(this).id
        } catch (_: Exception) {
            null
        }

        val value = when {
            fresh.isNullOrBlank() || fresh == "00000000-0000-0000-0000-000000000000" ->
                "${UUID.randomUUID()}${UUID.randomUUID()}"

            else -> fresh
        }

        prefs.edit { putString("adId", value) }
        return value!!
    }

    // =====================================================================================
    // 8) Результаты внешних системных экранов (камера, выбор файла).
    // Эти лончеры регистрируются один раз и живут вместе с Activity.
    // =====================================================================================

    /**
     * 8.1) Результат системного диалога разрешений на CAMERA.
     * Если пользователь согласился — отдаём grant() в исходный PermissionRequest.
     */
    private val cameraResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (::pendingCameraRequest.isInitialized) {
                if (granted) pendingCameraRequest.grant(pendingCameraRequest.resources)
                else pendingCameraRequest.deny()
            }
        }

    /**
     * 8.2) Результат системного файлового пикера.
     * Возвращаем WebView массив Uri выбранных файлов (или null).
     */
    private val fileChooserResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            pendingFileChooser?.onReceiveValue(uris)
            pendingFileChooser = null
        }

    // =====================================================================================
    // 9) Поведение системной кнопки "назад".
    // Управляем стеком WebView: если верхний может "назад" — идём назад;
    // иначе закрываем верхний; если остался один и он не может "назад" — закрываем Activity.
    // =====================================================================================
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            // Во время back-анимаций убираем прогресс, чтобы не мигал
            binding.horizontalProgressBar.isVisible = false

            val top =
                webViews.lastOrNull() ?: return@addCallback finish() // если вообще нет, закрываемся
            if (webViews.size == 1) {
                if (top.canGoBack()) top.goBack() else finish()
            } else {
                if (top.canGoBack()) {
                    top.goBack()
                } else {
                    // Окно пустое (история пуста) — закрываем и удаляем из стека
                    binding.root.removeView(top)
                    top.destroy()
                    webViews.removeAt(webViews.lastIndex)
                }
            }
        }
    }

    // =====================================================================================
    // 10) Жизненный цикл Activity: важно для корректной работы WebView и куков.
    //======================================================================================
    // =====================================================================================
    override fun onResume() {
        super.onResume()
        webViewLifecycle.onResume()
    }

    override fun onPause() {
        super.onPause()
        webViewLifecycle.onPause()
    }
}




