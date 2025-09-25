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
import com.FDGEntertain.repositoryWebView.AdIdProvider
import com.FDGEntertain.repositoryWebView.WebViewBackNavigator
import com.FDGEntertain.repositoryWebView.WebViewCameraPermission
import com.FDGEntertain.repositoryWebView.WebViewConsoleHandler
import com.FDGEntertain.repositoryWebView.WebViewCookiesAndDownloads
import com.FDGEntertain.repositoryWebView.WebViewFileChooser
import com.FDGEntertain.repositoryWebView.WebViewLifecycleDelegate
import com.FDGEntertain.repositoryWebView.WebViewUrlHandler
import com.FDGEntertain.util.FcmTokenFetcher
import com.FDGEntertain.util.LogoAnimator
import com.FDGEntertain.util.StartUrlBuilder
import com.FDGEntertain.util.WebViewStarter
import com.FDGEntertain.wv.SimpleAdIdProvider
import com.FDGEntertain.wv.SimpleWebViewBackNavigator
import com.FDGEntertain.wv.SimpleWebViewCameraPermission
import com.FDGEntertain.wv.SimpleWebViewConsoleHandler
import com.FDGEntertain.wv.SimpleWebViewCookiesAndDownloads
import com.FDGEntertain.wv.SimpleWebViewFileChooser
import com.FDGEntertain.wv.SimpleWebViewLifecycleDelegate
import com.FDGEntertain.wv.SimpleWebViewUrlHandler
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
//    private var pendingFileChooser: ValueCallback<Array<Uri>>? = null
    private lateinit var logoAnimator: LogoAnimator
    private lateinit var webViewLifecycle: WebViewLifecycleDelegate
    private lateinit var backNavigator: WebViewBackNavigator
    private lateinit var fileChooser: WebViewFileChooser
    private lateinit var cameraPermission: WebViewCameraPermission
    private lateinit var adIdProvider: AdIdProvider
    private lateinit var urlHandler: WebViewUrlHandler
    private lateinit var consoleHandler: WebViewConsoleHandler
    private lateinit var cookiesAndDownloads: WebViewCookiesAndDownloads
    private companion object {
        private const val BASE_URL = "https://keqe05h98f.execute-api.eu-north-1.amazonaws.com/"
    }

    private lateinit var webStarter: WebViewStarter
    private lateinit var fcmFetcher: FcmTokenFetcher
    private lateinit var urlBuilder: StartUrlBuilder
    // у тебя уже есть провайдер AdID-класса: SimpleAdIdProvider(applicationContext)


    // =====================================================================================
    // 1) Точка входа. Вызывается системой при создании Activity (главный UI-поток).
    // =====================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }

        setupUi()                   // (UI) Инфлейтим layout, настраиваем отступы, показываем лоадер
//        setupBackNavigation()       // (UI) Определяем поведение системной кнопки "назад"
        requestPostNotifications()  // (UI -> асинхронно) Запрашиваем разрешение на уведомления.
        webViewLifecycle = SimpleWebViewLifecycleDelegate(webViews)
        backNavigator = SimpleWebViewBackNavigator(
            activity = this,
            root = binding.root,
            progress = binding.horizontalProgressBar,
            webViews = webViews
        )
        backNavigator.install()
        fileChooser = SimpleWebViewFileChooser(this)

        cameraPermission = SimpleWebViewCameraPermission(
            caller = this,
            context = this
        )

        adIdProvider = SimpleAdIdProvider(applicationContext)
        urlHandler = SimpleWebViewUrlHandler(this)
        consoleHandler = SimpleWebViewConsoleHandler(this)
        cookiesAndDownloads = SimpleWebViewCookiesAndDownloads(this)

        fcmFetcher = FcmTokenFetcher()
        urlBuilder = StartUrlBuilder(BASE_URL, packageName)
        adIdProvider = SimpleAdIdProvider(applicationContext) // твой ранее сделанный класс

        webStarter = WebViewStarter(
            scope = lifecycleScope,
            fcm = fcmFetcher,
            adIdBlock = { adIdProvider.getAdId() }, // лямбда вместо интерфейса
            urlBuilder = urlBuilder,
            rootWebView = binding.rootWebView,
            loaderView = binding.circleProgressBarLayout,
            initWebView = ::initWebView           // твоя существующая функция
        )

        // После ответа ОС вызовет postNotificationsResultLauncher (см. ниже),
        // где мы стартуем Install Referrer.

        logoAnimator = LogoAnimator(binding.logoImage)
        logoAnimator.start()
    }

    // =====================================================================================
    // 2) Чистая настройка UI. Ничего асинхронного. Выполняется в onCreate на UI-потоке.
    // =====================================================================================
    private fun setupUi() {
        setupContentRoot()
        setupInsetsAndInitialState()
    }

    private fun setupContentRoot() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
    }

    private fun setupInsetsAndInitialState() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                sys.left, sys.top, sys.right,
                kotlin.comparisons.maxOf(ime.bottom, sys.bottom)
            )
            insets
        }

        binding.rootWebView.isVisible = false
        binding.circleProgressBarLayout.isVisible = true
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
            override fun onInstallReferrerSetupFinished(code: Int) {
                handleReferrerSetupFinished(code)          // <- вынесли обработку сюда
            }

            override fun onInstallReferrerServiceDisconnected() {
                installReferrerClient.startConnection(this)
            }
        })
    }

    // 2) Обрабатываем результат подключения (ровно как у тебя)
    private fun handleReferrerSetupFinished(code: Int) {
        if (code == InstallReferrerClient.InstallReferrerResponse.OK) {
            try {
                if (installReferer == null) {
                    installReferer = installReferrerClient.installReferrer.installReferrer
                    // как только получили — продолжаем твой флоу
                    openWebViewFlow(installReferer!!)
                }
            } catch (_: RemoteException) {
            }
        }
    }

    // =====================================================================================
    // 5) Главный "оркестратор" подготовки данных для URL и показа WebView.
    // Здесь совмещаем два источника асинхронных данных:
    //  - FCM токен (Task из Firebase, придёт позже)
    //  - Ad Id (получаем на IO-потоке; может занять время)
    // После — конфигурируем WebView и делаем loadUrl().
    //==========================================================
    // =====================================================================================
    private fun openWebViewFlow(refer: String) {
        webStarter.start(refer)
    }

    // =====================================================================================
    // 6) Единая точка конфигурации WebView.
    // Вызывается каждый раз, когда создаём новый WebView (root и новые окна).
    // =====================================================================================
    // =====================================================================================
    @SuppressLint("RequiresFeature")
    private fun initWebView(view: WebView) {
        cookiesAndDownloads.applyTo(view)

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
             * =======================================================================
             */
            override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                return consoleHandler.onConsoleMessage(msg)

            }

            /**
             * 6.3) Страница запросила доступ к камере/микрофону и т.п.
             * Если CAMERA уже дан — сразу grant(); иначе запрашиваем у ОС и продолжим в cameraResultLauncher.
             * ==================================================================================
             */
            override fun onPermissionRequest(request: PermissionRequest) {
               cameraPermission.onPermissionRequest(request)
            }

            /**
             * 6.4) Страница хочет открыть новое окно (target="_blank" или window.open).
             * Мы создаём новый WebView, настраиваем его, добавляем на экран и в стек.
             * WebViewTransport — "труба", через которую сообщаем движку, что новое окно готово.
             * ===============================================================================
             */
            // 1) Разделённая реализация onCreateWindow
            override fun onCreateWindow(
                v: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val newWv = createAndAttachWebView()
                deliverToTransport(newWv, resultMsg)
                return true
            }

            // 2) Создание и добавление нового WebView (UI + стек)
            private fun createAndAttachWebView(): WebView {
                val newWv = WebView(this@MainActivityTest)
                initWebView(newWv)
                binding.root.addView(newWv)
                webViews.add(newWv)
                return newWv
            }

            // 3) Передача созданного WebView обратно в движок через WebViewTransport
            private fun deliverToTransport(newWv: WebView, resultMsg: Message?) {
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = newWv
                resultMsg?.sendToTarget()
            }


            /**
             * 6.5) Страница открыла системный диалог выбора файла (input type=file).
             * Мы запускаем системный файловый пикер. Результат вернём в pendingFileChooser.
             * ===========================================================================
             */
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // Если вдруг висел старый колбэк — аккуратно "обнулим" его
                return fileChooser.onShowFileChooser(fileChooserParams, filePathCallback)
            }
        }

        // --- WebViewClient = навигация внутри WebView (перехват ссылок, ошибки загрузки и т.д.) ---
        view.webViewClient = object : WebViewClient() {

            /**
             * 6.6) Навигация по URL.
             * Если схема http/https — оставляем WebView обрабатывать страницу (return false).
             * Если нет (intent://, market:// и пр.) — пробуем отдать наружу в систему через Intent.
             * =============================================================================
             */
            override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
                return urlHandler.shouldOverride(req)
            }
        }
        setupViewContainer(view)
        setupWebSettings(view)
        // Если это корневой WebView и он ещё не в стеке — положим.
        if (!webViews.contains(view)) webViews.add(view)
    }

    // =====================================================================================
    // 7) Вспомогательные: сборка URL и безопасное получение Ad ID.
    // =====================================================================================

    /**
     * 7.1) Простой билдер query-строки без percent-encoding.
     * ВАЖНО: если на бэке ждут "сырые" значения (например, referer с '&'), не кодируем.
     * Если потребуется кодирование — делай точечно по конкретным ключам.
     * ==============================================
     */
//    private fun buildQuery(params: Map<String, String>): String =
//        params.entries.joinToString("&") { "${it.key}=${it.value}" }
//
//    // Приклеивает строку запроса к базовому URL
//    private fun appendQuery(baseUrl: String, query: String): String =
//        if (baseUrl.contains("?")) "$baseUrl&$query" else "$baseUrl?$query"

    /**
     * 7.2) Получение Advertising ID:
     *  - сперва пытаемся взять из SharedPreferences (кеш)
     *  - иначе просим у Play Services (может бросать исключения)
     *  - если пришёл нулевой ID — генерим случайный и кешируем
     *
     * Вызывается на IO-потоке (см. openWebViewFlow) — т.к. может блокировать.
     */
//    private fun getAdIdSafe(): String {
//        // сперва пытаемся взять из SharedPreferences (кеш)
//        val prefs = getSharedPreferences(packageName, MODE_PRIVATE)
//        val cached = prefs.getString("adId", null)
//        if (cached != null) return cached
//
//        // просим у Play Services (может бросать исключения)
//        val fresh = try {
//            AdvertisingIdClient.getAdvertisingIdInfo(this).id
//        } catch (_: Exception) {
//            null
//        }
//
//        val value = when {
//            fresh.isNullOrBlank() || fresh == "00000000-0000-0000-0000-000000000000" ->
//                "${UUID.randomUUID()}${UUID.randomUUID()}"
//
//            else -> fresh
//        }
//
//        prefs.edit { putString("adId", value) }
//        return value!!
//    }

    // =====================================================================================
    // 8) Результаты внешних системных экранов (камера, выбор файла).
    // Эти лончеры регистрируются один раз и живут вместе с Activity.
    // =====================================================================================

    /**
     * 8.1) Результат системного диалога разрешений на CAMERA.
     * Если пользователь согласился — отдаём grant() в исходный PermissionRequest.
     */
//    private val cameraResultLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//            if (::pendingCameraRequest.isInitialized) {
//                if (granted) pendingCameraRequest.grant(pendingCameraRequest.resources)
//                else pendingCameraRequest.deny()
//            }
//        }

    /**
     * 8.2) Результат системного файлового пикера.
     * Возвращаем WebView массив Uri выбранных файлов (или null).
     */
//    private val fileChooserResultLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
//            pendingFileChooser?.onReceiveValue(uris)
//            pendingFileChooser = null
//        }

    // =====================================================================================
    // 9) Поведение системной кнопки "назад".
    // Управляем стеком WebView: если верхний может "назад" — идём назад;
    // иначе закрываем верхний; если остался один и он не может "назад" — закрываем Activity.
    // =====================================================================================
//    private fun setupBackNavigation() {
//        onBackPressedDispatcher.addCallback(this) {
//            // Во время back-анимаций убираем прогресс, чтобы не мигал
//            binding.horizontalProgressBar.isVisible = false
//
//            val top =
//                webViews.lastOrNull() ?: return@addCallback finish() // если вообще нет, закрываемся
//            if (webViews.size == 1) {
//                if (top.canGoBack()) top.goBack() else finish()
//            } else {
//                if (top.canGoBack()) {
//                    top.goBack()
//                } else {
//                    // Окно пустое (история пуста) — закрываем и удаляем из стека
//                    binding.root.removeView(top)
//                    top.destroy()
//                    webViews.removeAt(webViews.lastIndex)
//                }
//            }
//        }
//    }
// 1) Контейнерные настройки самого WebView
    private fun setupViewContainer(view: WebView) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    // 2) Настройки WebSettings (поведение браузера внутри WebView)
    private fun setupWebSettings(view: WebView) {
        with(view.settings) {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            userAgentString = userAgentString.replace("; wv", "")
            mixedContentMode = 0 // равно WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = false
            builtInZoomControls = true
            displayZoomControls = false
            domStorageEnabled = true
            setSupportMultipleWindows(true)
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




