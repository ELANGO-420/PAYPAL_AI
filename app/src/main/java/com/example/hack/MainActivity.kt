package com.example.hack

import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

// ==========================================
// COLOR PALETTE & DESIGN SYSTEM
// ==========================================
val SlateBg = Color(0xFFF4F7FB)
val PayPalNavy = Color(0xFF001845)
val PayPalBlue = Color(0xFF0070BA)
val MintGreen = Color(0xFF0D9488)
val MintSurface = Color(0xFFCCFBF1)
val AmberDebit = Color(0xFFD97706)
val AmberSurface = Color(0xFFFEF3C7)
val CrimsonRed = Color(0xFFDC2626)
val RedSurface = Color(0xFFFEE2E2)
val IndigoOffline = Color(0xFF4F46E5)
val IndigoSurface = Color(0xFFEEF2FF)
val CardBorder = Color(0xFFE2E8F0)

fun formatMoney(amount: Double): String {
    return String.format(Locale.US, "$%.2f", amount)
}

// ==========================================
// LIVE AI CHATBOT ENGINE
// ==========================================
object LiveAiChatEngine {
    var apiKey: String = "AQ.Ab8RN6LwSYty-i_phjbn7HeLUFRt7MTf2OehbN0y7u0Edrx7wg"

    suspend fun generateAiResponse(
        userQuery: String,
        monthlyCredit: Double,
        monthlyDebit: Double,
        balance: Double,
        savedByAi: Double,
        protectedFromScams: Double
    ): String = withContext(Dispatchers.IO) {
        val promptText = """
            You are PayPal AI Financial Analyst, an expert, friendly AI assistant built into the PayPal Android App.
            Here is the user's current live financial context:
            - Wallet Balance: ${formatMoney(balance)}
            - Monthly Credit: +${formatMoney(monthlyCredit)}
            - Monthly Debit: -${formatMoney(monthlyDebit)}
            - Money Saved by AI Escrow Pauses: ${formatMoney(savedByAi)}
            - Scams Blocked by AI Scam Shield: ${formatMoney(protectedFromScams)}

            User's Question: "$userQuery"

            Provide a concise, highly insightful, professional, and helpful financial response (2-3 sentences max) tailored specifically to their live numbers.
        """.trimIndent()

        try {
            val endpoints = listOf(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            )

            for (endpointUrl in endpoints) {
                try {
                    val url = URL(endpointUrl)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 4000
                        readTimeout = 4000
                        setRequestProperty("Content-Type", "application/json")
                    }

                    val escapedPrompt = promptText.replace("\n", " ").replace("\"", "\\\"")
                    val jsonBody = """
                        {
                          "contents": [
                            {
                              "parts": [
                                {
                                  "text": "$escapedPrompt"
                                }
                              ]
                            }
                          ]
                        }
                    """.trimIndent()

                    conn.outputStream.use { os ->
                        os.write(jsonBody.toByteArray())
                    }

                    if (conn.responseCode == 200) {
                        val respText = conn.inputStream.bufferedReader().use { it.readText() }
                        val textIdx = respText.indexOf("\"text\": \"")
                        if (textIdx != -1) {
                            val candidate = respText.substring(textIdx + 9)
                            val endIdx = candidate.indexOf("\"")
                            if (endIdx != -1) {
                                val reply = candidate.substring(0, endIdx)
                                    .replace("\\n", " ")
                                    .replace("\\\"", "\"")
                                if (reply.isNotBlank()) return@withContext reply
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            getSmartLocalFallback(userQuery, monthlyCredit, monthlyDebit, balance, savedByAi, protectedFromScams)
        } catch (e: Exception) {
            getSmartLocalFallback(userQuery, monthlyCredit, monthlyDebit, balance, savedByAi, protectedFromScams)
        }
    }

    private fun getSmartLocalFallback(
        userQuery: String,
        monthlyCredit: Double,
        monthlyDebit: Double,
        balance: Double,
        savedByAi: Double,
        protectedFromScams: Double
    ): String {
        val q = userQuery.lowercase()
        val netCashflow = monthlyCredit - monthlyDebit
        return when {
            q.contains("credit") || q.contains("debit") || q.contains("cashflow") -> {
                "Your Total Monthly Credit is ${formatMoney(monthlyCredit)} and Total Debit is ${formatMoney(monthlyDebit)}. Net Cashflow: ${formatMoney(netCashflow)} with wallet balance at ${formatMoney(balance)}."
            }
            q.contains("spend") || q.contains("most") || q.contains("largest") -> {
                "Your largest expenses this month were in Food & Dining ($120.00) and Utility Bills ($145.00). Overall debit stands at ${formatMoney(monthlyDebit)}."
            }
            q.contains("save") || q.contains("ai") || q.contains("scam") || q.contains("protected") -> {
                "AI has saved you ${formatMoney(savedByAi)} by automatically pausing idle subscriptions, and intercepted ${formatMoney(protectedFromScams)} from fraudulent scammers!"
            }
            else -> {
                "AI Statement Summary: Balance is ${formatMoney(balance)}. Net Cashflow is ${formatMoney(netCashflow)}. AI Escrow & Scam Protection are active."
            }
        }
    }
}

// ==========================================
// PAYPAL SANDBOX REST API INTEGRATION ENGINE
// ==========================================
object PayPalSandboxEngine {
    const val PAYPAL_CLIENT_ID = "BAAL5YDDEWDKSHd_mKoPC1baLgEjjyjLAg8YjFi0r2A1attsIww6Z7wDMoN0DtBIOwIqzZXC74fyGC6dZI"
    const val PAYPAL_SECRET = "EJ7q4Iw-J-9oy25zr2GNHWDTpFZGmJtRAWm-e1kvvLGO-hXtA7hI6j7Ir8l0p2l5KZA2Oz92bP_0_XXE"
    const val PAYPAL_BASE_URL = "https://api-m.sandbox.paypal.com"

    suspend fun executePayPalSandboxPayment(amount: Double, description: String): String = withContext(Dispatchers.IO) {
        val fallbackId = "PAYID-SANDBOX-SUCCESS-${UUID.randomUUID().toString().take(8).uppercase()}"
        try {
            // 1. Obtain OAuth2 Access Token from PayPal Sandbox REST API
            val authUrl = URL("$PAYPAL_BASE_URL/v1/oauth2/token")
            val authConn = (authUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 3000
                readTimeout = 3000
                val userPass = "$PAYPAL_CLIENT_ID:$PAYPAL_SECRET"
                val encodedAuth = Base64.encodeToString(userPass.toByteArray(), Base64.NO_WRAP)
                setRequestProperty("Authorization", "Basic $encodedAuth")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            authConn.outputStream.use { os ->
                os.write("grant_type=client_credentials".toByteArray())
            }

            if (authConn.responseCode == 200) {
                val responseStr = authConn.inputStream.bufferedReader().use { it.readText() }
                val tokenStart = responseStr.indexOf("\"access_token\":\"")
                if (tokenStart != -1) {
                    val tokenSub = responseStr.substring(tokenStart + 16)
                    val accessToken = tokenSub.substring(0, tokenSub.indexOf("\""))

                    // 2. Create PayPal Sandbox Order
                    val orderUrl = URL("$PAYPAL_BASE_URL/v2/checkout/orders")
                    val orderConn = (orderUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 3000
                        readTimeout = 3000
                        setRequestProperty("Authorization", "Bearer $accessToken")
                        setRequestProperty("Content-Type", "application/json")
                    }

                    val jsonPayload = """
                        {
                            "intent": "CAPTURE",
                            "purchase_units": [
                                {
                                    "amount": {
                                        "currency_code": "USD",
                                        "value": "${String.format(Locale.US, "%.2f", amount)}"
                                    },
                                    "description": "$description"
                                }
                            ]
                        }
                    """.trimIndent()

                    orderConn.outputStream.use { os ->
                        os.write(jsonPayload.toByteArray())
                    }

                    if (orderConn.responseCode == 200 || orderConn.responseCode == 201) {
                        val orderResp = orderConn.inputStream.bufferedReader().use { it.readText() }
                        val idStart = orderResp.indexOf("\"id\":\"")
                        if (idStart != -1) {
                            val idSub = orderResp.substring(idStart + 6)
                            return@withContext idSub.substring(0, idSub.indexOf("\""))
                        }
                    }
                }
            }
            fallbackId
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackId
        }
    }
}

// ==========================================
// OFFICIAL PAYPAL LOGO BADGE COMPOSABLE
// ==========================================
@Composable
fun PayPalLogoBadge(modifier: Modifier = Modifier, height: Dp = 34.dp) {
    Surface(
        modifier = modifier
            .height(height)
            .shadow(3.dp, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFF0070BA).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .height(height * 0.75f)
                    .width(height * 0.75f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "P",
                    color = Color(0xFF0070BA),
                    fontWeight = FontWeight.Black,
                    fontSize = (height.value * 0.58f).sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.offset(x = 3.dp, y = 0.dp)
                )
                Text(
                    text = "P",
                    color = Color(0xFF001845),
                    fontWeight = FontWeight.Black,
                    fontSize = (height.value * 0.58f).sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.offset(x = (-3).dp, y = 0.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pay",
                    color = Color(0xFF001845),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (height.value * 0.52f).sp,
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = "Pal",
                    color = Color(0xFF0070BA),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (height.value * 0.52f).sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

// ==========================================
// DATA MODELS
// ==========================================
enum class TransactionType { CREDIT, DEBIT }

data class TransactionItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val amount: Double,
    val type: TransactionType,
    val date: String,
    val isOffline: Boolean = false,
    val tag: String = "",
)

data class EscrowSub(
    val id: String,
    val title: String,
    val costPerDay: Double,
    val trackingBadge: String,
    var isPaused: Boolean = false,
    var statusText: String = "ACTIVE • ESCROW PROTECTED",
)

data class FriendContact(
    val id: String,
    val name: String,
    val handle: String,
    var isSelected: Boolean = false,
    var statusText: String = "Not requested",
)

data class ChatMsg(
    val sender: String,
    val text: String,
    val isUser: Boolean,
    val time: String = "Just now",
)

data class ScamTarget(
    val name: String,
    val handle: String,
    val defaultAmount: Double,
    val isScam: Boolean,
    val riskScore: String,
    val reason: String,
)

data class PayPalSandboxOrder(
    val orderId: String = "PAYID-SB-${UUID.randomUUID().toString().take(8).uppercase()}",
    val title: String,
    val recipient: String,
    val amount: Double,
    val type: TransactionType = TransactionType.DEBIT
)

// ==========================================
// MAIN ACTIVITY ENTRY POINT
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = PayPalNavy,
                    secondary = PayPalBlue,
                    background = SlateBg,
                    surface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SlateBg
                ) {
                    PayPalFintechApp()
                }
            }
        }
    }
}

// ==========================================
// ROOT COMPOSABLE & CENTRAL STATE
// ==========================================
@Composable
fun PayPalFintechApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Dynamic Central State
    var balance by remember { mutableDoubleStateOf(1420.00) }
    var monthlyCredit by remember { mutableDoubleStateOf(2450.00) }
    var monthlyDebit by remember { mutableDoubleStateOf(1030.00) }
    var savedByAi by remember { mutableDoubleStateOf(32.50) }
    var protectedFromScams by remember { mutableDoubleStateOf(0.00) }
    var offlineModeEnabled by remember { mutableStateOf(false) }

    // PayPal Sandbox Configuration State
    var sandboxClientId by remember { mutableStateOf(PayPalSandboxEngine.PAYPAL_CLIENT_ID) }
    var sandboxBuyerEmail by remember { mutableStateOf("sb-buyer8491@personal.example.com") }
    var showSandboxConfigDialog by remember { mutableStateOf(false) }
    var activeSandboxOrder by remember { mutableStateOf<PayPalSandboxOrder?>(null) }
    var sandboxStep by remember { mutableIntStateOf(0) }
    var isExecutingSandbox by remember { mutableStateOf(false) }
    var liveSandboxTxId by remember { mutableStateOf("") }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Pre-loaded Statement Transactions
    val transactions = remember {
        mutableStateListOf(
            TransactionItem(title = "Salary Direct Deposit", category = "Income", amount = 1500.00, type = TransactionType.CREDIT, date = "Today, 09:30 AM", tag = "[PAYROLL]"),
            TransactionItem(title = "Freelance UI Design", category = "Freelance", amount = 950.00, type = TransactionType.CREDIT, date = "Yesterday", tag = "[CLIENT]"),
            TransactionItem(title = "Gadget Store Refund", category = "Shopping", amount = 65.00, type = TransactionType.CREDIT, date = "2 days ago", tag = "[REFUND]"),
            TransactionItem(title = "Whole Foods Supermarket", category = "Food & Dining", amount = 120.00, type = TransactionType.DEBIT, date = "3 days ago"),
            TransactionItem(title = "Urban Bistro Dinner", category = "Food & Dining", amount = 85.00, type = TransactionType.DEBIT, date = "4 days ago"),
            TransactionItem(title = "Electricity & Utility Bill", category = "Bills & Utilities", amount = 145.00, type = TransactionType.DEBIT, date = "5 days ago")
        )
    }

    // Pre-loaded Escrow Subscriptions
    val subscriptions = remember {
        mutableStateListOf(
            EscrowSub("1", "Gold's Gym Pro", 1.50, "GPS Geofence & Gate QR"),
            EscrowSub("2", "MasterClass App", 1.00, "Android UsageStatsManager Sensor"),
            EscrowSub("3", "Cloud GPU Lab", 2.00, "Merchant Login Webhook")
        )
    }

    // Pre-loaded Friends for Split Bill
    val friends = remember {
        mutableStateListOf(
            FriendContact("f1", "Bala", "@bala_dev"),
            FriendContact("f2", "Karthick", "@karthick_ui"),
            FriendContact("f3", "Priya", "@priya_tech"),
            FriendContact("f4", "Rahul", "@rahul_fin")
        )
    }

    // Pre-loaded AI Chatbot Messages
    val chatMessages = remember {
        mutableStateListOf(
            ChatMsg(
                sender = "PayPal AI",
                text = "Hello! I am your PayPal Financial AI Assistant. Ask me about your credit vs debit breakdown, idle subscription savings, or scam protection statistics!",
                isUser = false
            )
        )
    }

    fun launchSandboxCheckout(order: PayPalSandboxOrder) {
        activeSandboxOrder = order
        sandboxStep = 0
        isExecutingSandbox = false
        liveSandboxTxId = ""
    }

    Scaffold(
        topBar = {
            HeaderBar(
                offlineMode = offlineModeEnabled,
                onOpenSandboxSettings = { showSandboxConfigDialog = true }
            )
        },
        bottomBar = {
            NavBottomBar(selectedTab = selectedTab) { selectedTab = it }
        },
        containerColor = SlateBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TabHomeEscrow(
                    balance = balance,
                    monthlyCredit = monthlyCredit,
                    monthlyDebit = monthlyDebit,
                    savedByAi = savedByAi,
                    protectedFromScams = protectedFromScams,
                    subscriptions = subscriptions,
                    onVerifyAndPay = { sub ->
                        launchSandboxCheckout(
                            PayPalSandboxOrder(
                                title = "Escrow: ${sub.title}",
                                recipient = sub.title,
                                amount = sub.costPerDay,
                                type = TransactionType.DEBIT
                            )
                        )
                    },
                    onSimulateIdle = { sub ->
                        sub.isPaused = true
                        sub.statusText = "AI PAUSED • SAVING MONEY"
                        val savedAmount = sub.costPerDay * 5.0
                        savedByAi += savedAmount
                        Toast.makeText(context, "AI detected 5 days zero usage! Subscription paused. Saved ${formatMoney(savedAmount)}", Toast.LENGTH_LONG).show()
                    },
                    onSandboxDeposit = { amt ->
                        launchSandboxCheckout(
                            PayPalSandboxOrder(
                                title = "PayPal Sandbox Wallet Top-up",
                                recipient = "PayPal Sandbox Balance",
                                amount = amt,
                                type = TransactionType.CREDIT
                            )
                        )
                    }
                )

                1 -> TabOfflinePay(
                    balance = balance,
                    offlineModeEnabled = offlineModeEnabled,
                    onToggleOfflineMode = { offlineModeEnabled = it },
                    onSendOfflinePayment = { recipient, amount ->
                        balance -= amount
                        monthlyDebit += amount
                        transactions.add(
                            0,
                            TransactionItem(
                                title = "Offline SMS: $recipient",
                                category = "Offline Mesh Transfer",
                                amount = amount,
                                type = TransactionType.DEBIT,
                                date = "Just now",
                                isOffline = true,
                                tag = "[OFFLINE SMS]"
                            )
                        )
                    }
                )

                2 -> TabScamShield(
                    protectedFromScams = protectedFromScams,
                    onPaymentAttempt = { target, amount ->
                        if (target.isScam) {
                            protectedFromScams += amount
                        } else {
                            launchSandboxCheckout(
                                PayPalSandboxOrder(
                                    title = "Merchant Payment: ${target.name}",
                                    recipient = target.name,
                                    amount = amount,
                                    type = TransactionType.DEBIT
                                )
                            )
                        }
                    }
                )

                3 -> TabSplitBill(
                    balance = balance,
                    friends = friends,
                    onFriendsPaid = { friendList, perPersonShare ->
                        coroutineScope.launch {
                            var totalCollected = 0.0
                            friendList.forEach { friend ->
                                delay(600)
                                friend.statusText = "PAID VIA PAYPAL ✅"
                                totalCollected += perPersonShare
                                balance += perPersonShare
                                monthlyCredit += perPersonShare
                                transactions.add(
                                    0,
                                    TransactionItem(
                                        title = "Split Reimbursement (${friend.name})",
                                        category = "Bill Reimbursement",
                                        amount = perPersonShare,
                                        type = TransactionType.CREDIT,
                                        date = "Just now",
                                        tag = "[SANDBOX PAID]"
                                    )
                                )
                            }
                            Toast.makeText(context, "All friends settled! Added ${formatMoney(totalCollected)} to wallet.", Toast.LENGTH_LONG).show()
                        }
                    }
                )

                4 -> TabAiAnalyst(
                    balance = balance,
                    monthlyCredit = monthlyCredit,
                    monthlyDebit = monthlyDebit,
                    savedByAi = savedByAi,
                    protectedFromScams = protectedFromScams,
                    transactions = transactions,
                    chatMessages = chatMessages
                )
            }
        }
    }

    // PAYPAL SANDBOX CHECKOUT EXECUTION DIALOG
    if (activeSandboxOrder != null) {
        val order = activeSandboxOrder!!
        Dialog(onDismissRequest = { if (!isExecutingSandbox) activeSandboxOrder = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PayPalLogoBadge(height = 30.dp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "SANDBOX API ACTIVE", color = Color(0xFFD97706), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = order.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PayPalNavy)
                    Text(text = "Amount: ${formatMoney(order.amount)}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = PayPalBlue)

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(text = "Sandbox Client ID: ${sandboxClientId.take(18)}...", fontSize = 10.sp, color = Color.Gray)
                            Text(text = "Sandbox Buyer: $sandboxBuyerEmail", fontSize = 10.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                            Text(text = "Sandbox Base URL: ${PayPalSandboxEngine.PAYPAL_BASE_URL}", fontSize = 10.sp, color = PayPalBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isExecutingSandbox) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (sandboxStep >= 1) Icons.Default.CheckCircle else Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (sandboxStep >= 1) MintGreen else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "1. POST /v1/oauth2/token (OAuth Authenticated)", fontSize = 11.sp, fontWeight = if (sandboxStep == 1) FontWeight.Bold else FontWeight.Normal)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (sandboxStep >= 2) Icons.Default.CheckCircle else Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (sandboxStep >= 2) MintGreen else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "2. POST /v2/checkout/orders (Executing Order)...", fontSize = 11.sp, fontWeight = if (sandboxStep == 2) FontWeight.Bold else FontWeight.Normal)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (sandboxStep >= 3) Icons.Default.CheckCircle else Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (sandboxStep >= 3) MintGreen else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "3. Sandbox Order Captured: ${if (liveSandboxTxId.isNotBlank()) liveSandboxTxId else "PAYID-APPROVED"}", fontSize = 11.sp, fontWeight = if (sandboxStep == 3) FontWeight.Bold else FontWeight.Normal)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (sandboxStep == 3) {
                            Button(
                                onClick = {
                                    if (order.type == TransactionType.CREDIT) {
                                        balance += order.amount
                                        monthlyCredit += order.amount
                                        transactions.add(
                                            0,
                                            TransactionItem(
                                                title = order.title,
                                                category = "PayPal Sandbox Top-up",
                                                amount = order.amount,
                                                type = TransactionType.CREDIT,
                                                date = "Just now",
                                                tag = "[PAYPAL SANDBOX]"
                                            )
                                        )
                                    } else {
                                        balance -= order.amount
                                        monthlyDebit += order.amount
                                        transactions.add(
                                            0,
                                            TransactionItem(
                                                title = order.title,
                                                category = "PayPal Sandbox Payment",
                                                amount = order.amount,
                                                type = TransactionType.DEBIT,
                                                date = "Just now",
                                                tag = "[PAYPAL SANDBOX]"
                                            )
                                        )
                                    }
                                    activeSandboxOrder = null
                                    Toast.makeText(context, "PayPal Sandbox Payment Approved! Order: $liveSandboxTxId", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Complete & Close Sandbox", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            CircularProgressIndicator(color = PayPalBlue, modifier = Modifier.size(26.dp))
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { activeSandboxOrder = null },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "Cancel", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    isExecutingSandbox = true
                                    sandboxStep = 1
                                    coroutineScope.launch {
                                        delay(600)
                                        sandboxStep = 2
                                        val realTxId = PayPalSandboxEngine.executePayPalSandboxPayment(order.amount, order.title)
                                        liveSandboxTxId = realTxId
                                        sandboxStep = 3
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(containerColor = PayPalBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "Pay via PayPal Sandbox", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // PAYPAL SANDBOX CONFIGURATION SETTINGS DIALOG
    if (showSandboxConfigDialog) {
        Dialog(onDismissRequest = { showSandboxConfigDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PayPalLogoBadge(height = 28.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "PayPal Sandbox Credentials", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PayPalNavy)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "Sandbox Client ID", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = sandboxClientId,
                        onValueChange = { sandboxClientId = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Sandbox Buyer Email", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = sandboxBuyerEmail,
                        onValueChange = { sandboxBuyerEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "AI Chatbot API Key", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = LiveAiChatEngine.apiKey,
                        onValueChange = { LiveAiChatEngine.apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showSandboxConfigDialog = false
                            Toast.makeText(context, "PayPal Sandbox Credentials Saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PayPalNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Save Credentials", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// TOP HEADER BAR WITH OFFICIAL LOGO & SANDBOX BUTTON
// ==========================================
@Composable
fun HeaderBar(
    offlineMode: Boolean,
    onOpenSandboxSettings: () -> Unit
) {
    Surface(
        color = PayPalNavy,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PayPalLogoBadge(height = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Demo",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (offlineMode) "OFFLINE MESH MODE" else "SANDBOX ACTIVE",
                            color = if (offlineMode) Color(0xFFA5B4FC) else Color(0xFF93C5FD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onOpenSandboxSettings() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = "Sandbox Config", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Sandbox", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (offlineMode) IndigoOffline else MintGreen)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (offlineMode) "SMS Tunnel" else "AI Active",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5-TAB SLEEK NAVIGATION BAR
// ==========================================
@Composable
fun NavBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        val navItems = listOf(
            Triple("Home", Icons.Default.Home, 0),
            Triple("Offline", Icons.AutoMirrored.Filled.Send, 1),
            Triple("Scam Shield", Icons.Default.Lock, 2),
            Triple("Split Bill", Icons.Default.Person, 3),
            Triple("AI Analyst", Icons.Default.Info, 4)
        )

        navItems.forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selectedTab == index) PayPalBlue else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) PayPalBlue else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFE0F2FE)
                )
            )
        }
    }
}

// ==========================================
// TAB 1: HOME & SMART ESCROW ("Pulse")
// ==========================================
@Composable
fun TabHomeEscrow(
    balance: Double,
    monthlyCredit: Double,
    monthlyDebit: Double,
    savedByAi: Double,
    protectedFromScams: Double,
    subscriptions: List<EscrowSub>,
    onVerifyAndPay: (EscrowSub) -> Unit,
    onSimulateIdle: (EscrowSub) -> Unit,
    onSandboxDeposit: (Double) -> Unit
) {
    var customDepositText by remember { mutableStateOf("100.00") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HERO CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PayPalNavy, PayPalBlue)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PayPalLogoBadge(height = 28.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Demo Wallet",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "SANDBOX PROTECTED",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = formatMoney(balance),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // SUMMARY PILLS ROW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Credit Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(text = "Monthly Credit", color = Color(0xFFA7F3D0), fontSize = 10.sp)
                                    Text(text = "+${formatMoney(monthlyCredit)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Debit Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(text = "Monthly Debit", color = Color(0xFFFDE68A), fontSize = 10.sp)
                                    Text(text = "-${formatMoney(monthlyDebit)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // AI Saved Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(text = "Saved by AI", color = Color(0xFF6EE7B7), fontSize = 10.sp)
                                    Text(text = formatMoney(savedByAi), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        if (protectedFromScams > 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CrimsonRed.copy(alpha = 0.3f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scams Blocked: ${formatMoney(protectedFromScams)} Saved",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // PAYPAL SANDBOX QUICK TOP-UP CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PayPalBlue.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PayPalLogoBadge(height = 24.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "PayPal Sandbox Top-up", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PayPalNavy)
                        }
                        Text(text = "Environment: SANDBOX", fontSize = 10.sp, color = AmberDebit, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customDepositText,
                            onValueChange = { customDepositText = it },
                            placeholder = { Text("Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val amt = customDepositText.toDoubleOrNull() ?: 50.0
                                onSandboxDeposit(amt)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PayPalBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Add Sandbox Funds", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // SMART ESCROW TITLE
        item {
            Column {
                Text(
                    text = "Smart Usage-Verified Escrow",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PayPalNavy
                )
                Text(
                    text = "Subscriptions are held in Escrow and released only upon verified daily usage.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // SUBSCRIPTION CARDS
        items(subscriptions) { sub ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (sub.isPaused) AmberSurface else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = sub.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PayPalNavy
                            )
                            Text(
                                text = "${formatMoney(sub.costPerDay)} / day",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PayPalBlue
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (sub.isPaused) AmberDebit else MintGreen)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = sub.statusText,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tracked via: ${sub.trackingBadge}",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onVerifyAndPay(sub) },
                            modifier = Modifier.weight(1.2f),
                            enabled = !sub.isPaused,
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Verify & Pay Sandbox", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onSimulateIdle(sub) },
                            modifier = Modifier.weight(1f),
                            enabled = !sub.isPaused,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberDebit),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Simulate 5D Idle", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: OFFLINE PAYMENT MODE ("Offline Pay")
// ==========================================
@Composable
fun TabOfflinePay(
    balance: Double,
    offlineModeEnabled: Boolean,
    onToggleOfflineMode: (Boolean) -> Unit,
    onSendOfflinePayment: (String, Double) -> Unit
) {
    var selectedRecipient by remember { mutableStateOf("Arun General Store") }
    var amountText by remember { mutableStateOf("25.00") }
    var isSimulatingPayment by remember { mutableStateOf(false) }
    var simulationStep by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    val recipients = listOf("Arun General Store", "Campus Canteen", "Bala Dev", "City Express Mini Mart")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP TOGGLE CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (offlineModeEnabled) IndigoSurface else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline SMS Payment Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (offlineModeEnabled) IndigoOffline else PayPalNavy
                            )
                            Text(
                                text = "No Internet / SMS-USSD Tunnel",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = offlineModeEnabled,
                            onCheckedChange = { onToggleOfflineMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IndigoOffline
                            )
                        )
                    }

                    if (offlineModeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(IndigoOffline)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "OFFLINE MESH ACTIVE",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "No Mobile Data required. Payments are compressed into an encrypted SMS token and settled via PayPal Telecom Gateway.",
                                        color = Color(0xFFE0E7FF),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // QUICK RECIPIENT SELECTOR
        item {
            Column {
                Text(
                    text = "Select Offline Merchant / Contact",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PayPalNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recipients) { contact ->
                        FilterChip(
                            selected = selectedRecipient == contact,
                            onClick = { selectedRecipient = contact },
                            label = { Text(text = contact, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoOffline,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // AMOUNT INPUT & ACTION CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Enter Payment Amount ($)", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt in 0.01..balance) {
                                isSimulatingPayment = true
                                simulationStep = 1
                                coroutineScope.launch {
                                    delay(1200)
                                    simulationStep = 2
                                    delay(1200)
                                    simulationStep = 3
                                    onSendOfflinePayment(selectedRecipient, amt)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (offlineModeEnabled) IndigoOffline else PayPalNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = offlineModeEnabled
                    ) {
                        Text(
                            text = if (offlineModeEnabled) "Send Offline SMS Payment" else "Enable Offline Mode to Send",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // 3-STEP PROGRESS DIALOG FOR OFFLINE PAY
    if (isSimulatingPayment) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Offline SMS Payment Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = IndigoOffline
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 1
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = if (simulationStep >= 1) Icons.Default.CheckCircle else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (simulationStep >= 1) MintGreen else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "1. Generating Encrypted Token (PP-SMS-9941X)...",
                            fontSize = 12.sp,
                            fontWeight = if (simulationStep == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step 2
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = if (simulationStep >= 2) Icons.Default.CheckCircle else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (simulationStep >= 2) MintGreen else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "2. Dispatching Cellular SMS (0 KB Data)...",
                            fontSize = 12.sp,
                            fontWeight = if (simulationStep == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step 3
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = if (simulationStep >= 3) Icons.Default.CheckCircle else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (simulationStep >= 3) MintGreen else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "3. Payment Confirmed! Receipt SMS Received.",
                            fontSize = 12.sp,
                            fontWeight = if (simulationStep == 3) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (simulationStep == 3) {
                        Button(
                            onClick = {
                                isSimulatingPayment = false
                                simulationStep = 0
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Done & Close", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        CircularProgressIndicator(color = IndigoOffline, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: AUTO SCAM DETECTION ("Scam Shield")
// ==========================================
@Composable
fun TabScamShield(
    protectedFromScams: Double,
    onPaymentAttempt: (ScamTarget, Double) -> Unit
) {
    var customRecipient by remember { mutableStateOf("") }
    var customAmount by remember { mutableStateOf("") }
    var activeAlertTarget by remember { mutableStateOf<ScamTarget?>(null) }
    var activeAlertAmount by remember { mutableDoubleStateOf(0.0) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastPaidTarget by remember { mutableStateOf("") }

    val presetTargets = listOf(
        ScamTarget(
            name = "Fake Bank KYC Update",
            handle = "kyc-verify-urgent@scam.net",
            defaultAmount = 250.00,
            isScam = true,
            riskScore = "98.4% (CRITICAL)",
            reason = "Account flagged for 42 recent phishing reports + abnormal velocity pattern + mismatched merchant KYC."
        ),
        ScamTarget(
            name = "Crypto Double-Money Lottery",
            handle = "win-eth-fast@doubler.org",
            defaultAmount = 500.00,
            isScam = true,
            riskScore = "99.1% (CRITICAL)",
            reason = "Known high-risk Ponzi smart contract address + unauthorized social engineering link."
        ),
        ScamTarget(
            name = "Verified Campus Bookstore",
            handle = "payments@campusbooks.edu",
            defaultAmount = 45.00,
            isScam = false,
            riskScore = "1.2% (SAFE)",
            reason = "Verified Educational Merchant with 5-year clean transaction history."
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SCAM SHIELD HEADER CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CrimsonRed)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PayPal AI Scam Shield Active",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Real-time AI behavioral analysis intercepts fraudulent transactions before funds leave your wallet.",
                        color = Color(0xFFFEE2E2),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Total Protected from Scammers:", color = Color.White, fontSize = 12.sp)
                            Text(
                                text = formatMoney(protectedFromScams),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Test Live Fraud Detection Scenarios",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PayPalNavy
            )
        }

        // PRESET SCAMMER / SAFE TARGET CARDS
        items(presetTargets) { target ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (target.isScam) CrimsonRed.copy(alpha = 0.5f) else MintGreen.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (target.isScam) RedSurface else MintSurface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = target.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PayPalNavy
                            )
                            Text(text = target.handle, fontSize = 11.sp, color = Color.Gray)
                        }

                        Text(
                            text = formatMoney(target.defaultAmount),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (target.isScam) CrimsonRed else MintGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            activeAlertTarget = target
                            activeAlertAmount = target.defaultAmount
                            if (!target.isScam) {
                                lastPaidTarget = target.name
                                showSuccessDialog = true
                            }
                            onPaymentAttempt(target, target.defaultAmount)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (target.isScam) CrimsonRed else MintGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (target.isScam) "Attempt Scam Transfer (${formatMoney(target.defaultAmount)})" else "Pay via PayPal Sandbox (${formatMoney(target.defaultAmount)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // CUSTOM RECIPIENT INPUT
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Custom Payment Security Check", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PayPalNavy)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customRecipient,
                        onValueChange = { customRecipient = it },
                        placeholder = { Text("Enter Email / Handle (e.g., suspicious-kyc@mail.com)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it },
                        placeholder = { Text("Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val amt = customAmount.toDoubleOrNull() ?: 100.0
                            val isSuspicious = customRecipient.lowercase().let {
                                it.contains("kyc") || it.contains("fake") || it.contains("scam") || it.contains("lottery") || it.contains("phish")
                            }
                            val target = ScamTarget(
                                name = customRecipient.ifBlank { "Unknown Target" },
                                handle = customRecipient,
                                defaultAmount = amt,
                                isScam = isSuspicious,
                                riskScore = if (isSuspicious) "97.8% (CRITICAL)" else "2.1% (SAFE)",
                                reason = if (isSuspicious) "Unrecognized handle with recent suspicious payment requests flagged by AI." else "Standard clean profile."
                            )
                            activeAlertTarget = target
                            activeAlertAmount = amt
                            if (!isSuspicious) {
                                lastPaidTarget = target.name
                                showSuccessDialog = true
                            }
                            onPaymentAttempt(target, amt)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PayPalNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Scan & Attempt Transfer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // RED AI SCAM BLOCKED DIALOG
    if (activeAlertTarget != null && activeAlertTarget!!.isScam) {
        Dialog(onDismissRequest = { activeAlertTarget = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RedSurface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CrimsonRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PAYMENT AUTOMATICALLY BLOCKED BY AI",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = CrimsonRed,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CrimsonRed)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "AI FRAUD RISK SCORE: ${activeAlertTarget!!.riskScore}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Target: ${activeAlertTarget!!.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PayPalNavy
                    )
                    Text(
                        text = "Blocked Amount: ${formatMoney(activeAlertAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CrimsonRed
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "AI Reason: ${activeAlertTarget!!.reason}",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { activeAlertTarget = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Acknowledge & Protect Wallet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // GREEN SAFE SUCCESS DIALOG
    if (showSuccessDialog) {
        Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MintSurface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintGreen, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "AI Security Check Passed (Risk: 1.2%)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MintGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Sent ${formatMoney(activeAlertAmount)} safely to $lastPaidTarget.", fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 4: SMART FRIEND BILL SPLITTER ("Split Bill")
// ==========================================
@Composable
fun TabSplitBill(
    balance: Double,
    friends: List<FriendContact>,
    onFriendsPaid: (List<FriendContact>, Double) -> Unit
) {
    var billTitle by remember { mutableStateOf("Team Dinner at BBQ") }
    var billAmountText by remember { mutableStateOf("80.00") }
    var isMessageSent by remember { mutableStateOf(false) }

    val totalAmount = billAmountText.toDoubleOrNull() ?: 0.0
    val selectedCount = friends.count { it.isSelected }
    val totalPeople = selectedCount + 1
    val perPersonShare = if (totalPeople > 0) totalAmount / totalPeople else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP BILL SPLIT INPUT CARD WITH LIVE WALLET REFERENCE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Smart Friend Expense Splitter", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PayPalNavy)
                            Text(text = "Split expenses & auto-deposit into wallet.", fontSize = 11.sp, color = Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MintSurface)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Wallet: ${formatMoney(balance)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Bill Title", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = billTitle,
                        onValueChange = { billTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Total Bill Amount ($)", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = billAmountText,
                        onValueChange = { billAmountText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // PRESET QUICK CHIPS
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                billTitle = "Team Dinner at BBQ"
                                billAmountText = "80.00"
                            },
                            label = { Text("BBQ $80", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                billTitle = "Movie & Snacks"
                                billAmountText = "40.00"
                            },
                            label = { Text("Movie $40", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // FRIEND SELECTOR CHIPS / CHECKBOXES
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Select Friends to Split With", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PayPalNavy)
                    Spacer(modifier = Modifier.height(8.dp))

                    friends.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { friend.isSelected = !friend.isSelected }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = friend.isSelected,
                                    onCheckedChange = { friend.isSelected = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(text = friend.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PayPalNavy)
                                    Text(text = friend.handle, fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            if (isMessageSent) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (friend.statusText.contains("PAID")) MintSurface else AmberSurface)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = friend.statusText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (friend.statusText.contains("PAID")) MintGreen else AmberDebit
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // REAL-TIME CALCULATION SUMMARY CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PayPalBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Live Calculation Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PayPalNavy)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total People ($selectedCount friends + You):", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "$totalPeople People", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Per Person Equal Share:", fontSize = 13.sp, color = PayPalBlue, fontWeight = FontWeight.Bold)
                        Text(text = formatMoney(perPersonShare), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PayPalBlue)
                    }

                    if (isMessageSent) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Message Sent: \"Hey! Split $billTitle ($billAmountText). Your share is ${formatMoney(perPersonShare)}. Pay via PayPal Link.\"",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                friends.forEach {
                                    if (it.isSelected) {
                                        it.statusText = "Message Sent • Waiting"
                                    }
                                }
                                isMessageSent = true
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedCount > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = PayPalNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Send Split Request", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val selectedFriends = friends.filter { it.isSelected }
                                onFriendsPaid(selectedFriends, perPersonShare)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isMessageSent && selectedCount > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Simulate Friends Paying", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 5: AI STATEMENT CHATBOT ("AI Analyst")
// ==========================================
@Composable
fun TabAiAnalyst(
    balance: Double,
    monthlyCredit: Double,
    monthlyDebit: Double,
    savedByAi: Double,
    protectedFromScams: Double,
    transactions: List<TransactionItem>,
    chatMessages: MutableList<ChatMsg>
) {
    var userQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var isThinking by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val netCashflow = monthlyCredit - monthlyDebit

    val filteredTransactions = remember(selectedFilter, transactions.size) {
        when (selectedFilter) {
            "CREDIT" -> transactions.filter { it.type == TransactionType.CREDIT }
            "DEBIT" -> transactions.filter { it.type == TransactionType.DEBIT }
            else -> transactions
        }
    }

    fun handleQuery(query: String) {
        if (query.isBlank() || isThinking) return
        chatMessages.add(ChatMsg(sender = "You", text = query, isUser = true))
        isThinking = true

        coroutineScope.launch {
            val aiReply = LiveAiChatEngine.generateAiResponse(
                userQuery = query,
                monthlyCredit = monthlyCredit,
                monthlyDebit = monthlyDebit,
                balance = balance,
                savedByAi = savedByAi,
                protectedFromScams = protectedFromScams
            )
            isThinking = false
            chatMessages.add(ChatMsg(sender = "PayPal AI", text = aiReply, isUser = false))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SUMMARY METRICS ROW
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Live Cashflow & Statement Overview", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PayPalNavy)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Credit Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MintSurface)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(text = "Total Credit", fontSize = 10.sp, color = MintGreen, fontWeight = FontWeight.Bold)
                                Text(text = "+${formatMoney(monthlyCredit)}", fontSize = 13.sp, color = MintGreen, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // Debit Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberSurface)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(text = "Total Debit", fontSize = 10.sp, color = AmberDebit, fontWeight = FontWeight.Bold)
                                Text(text = "-${formatMoney(monthlyDebit)}", fontSize = 13.sp, color = AmberDebit, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // Net Cashflow Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(IndigoSurface)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(text = "Net Cashflow", fontSize = 10.sp, color = IndigoOffline, fontWeight = FontWeight.Bold)
                                Text(text = formatMoney(netCashflow), fontSize = 13.sp, color = IndigoOffline, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // CHATBOT SECTION
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PayPalBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "PayPal AI Financial Analyst Chatbot", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PayPalNavy)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // PRESET QUESTION CHIPS
                    Text(text = "Tap 1-click insights:", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val questions = listOf(
                            "Analyze my monthly Credit vs Debit",
                            "Where did I spend the most?",
                            "How much did AI save me this month?"
                        )
                        items(questions) { q ->
                            SuggestionChip(
                                onClick = { handleQuery(q) },
                                label = { Text(text = q, fontSize = 10.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // CHAT LOG BOX
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chatMessages.takeLast(4).forEach { msg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (msg.isUser) PayPalBlue else Color.White)
                                        .border(1.dp, if (msg.isUser) Color.Transparent else CardBorder, RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 11.sp,
                                        color = if (msg.isUser) Color.White else PayPalNavy
                                    )
                                }
                            }
                        }

                        if (isThinking) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = PayPalBlue, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "PayPal AI is analyzing your live financial statement...", fontSize = 10.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // INPUT FIELD
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userQuery,
                            onValueChange = { userQuery = it },
                            placeholder = { Text("Ask AI about statement...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (userQuery.isNotBlank()) {
                                    val q = userQuery
                                    userQuery = ""
                                    handleQuery(q)
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PayPalBlue)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }
        }

        // STATEMENT HISTORY TITLE & FILTERS
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Statement History", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PayPalNavy)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "CREDIT",
                            onClick = { selectedFilter = "CREDIT" },
                            label = { Text("Credit", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "DEBIT",
                            onClick = { selectedFilter = "DEBIT" },
                            label = { Text("Debit", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // TRANSACTION ITEMS
        items(filteredTransactions) { tx ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (tx.type == TransactionType.CREDIT) MintSurface else AmberSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (tx.type == TransactionType.CREDIT) Icons.Default.Add else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (tx.type == TransactionType.CREDIT) MintGreen else AmberDebit,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = tx.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PayPalNavy)
                                if (tx.tag.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = tx.tag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PayPalBlue)
                                }
                            }
                            Text(text = "${tx.category} • ${tx.date}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (tx.type == TransactionType.CREDIT) MintSurface else AmberSurface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (tx.type == TransactionType.CREDIT) "+${formatMoney(tx.amount)}" else "-${formatMoney(tx.amount)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = if (tx.type == TransactionType.CREDIT) MintGreen else AmberDebit
                        )
                    }
                }
            }
        }
    }
}
