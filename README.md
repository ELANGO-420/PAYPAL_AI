 Architecture & Technology Stack
•
UI Framework: Jetpack Compose with Material 3 design system, custom gradients, cards, and smooth navigation.
•
Backend & Cloud Database:
◦
Firebase Authentication: Email/Password sign-in/registration, Google Sign-In, and Phone SMS OTP verification (Project ID: com-example-hack-90b75).
◦
Cloud Firestore: Real-time synchronization of wallet transactions and escrow status.
•
Payment Gateway Integration:
◦
Official PayPal Sandbox REST APIs (/v1/oauth2/token for OAuth Bearer token authentication and /v2/checkout/orders for order capture and processing).
•
Artificial Intelligence:
◦
Google Gemini API (gemini-1.5-flash, gemini-2.0-flash) integrated into an interactive financial analyst chatbot.
•
Security & Cryptography:
◦
Zero-Trace Obfuscated Memory Vault (EncryptedSecretVault) that securely masks and protects PayPal API credentials and Gemini API keys in memory.
◦
Real-Time Firebase Security Logger & Audit Monitor tracking all authentication, database syncs, and security events.
🔥 Core Flagship Features
1.
💰 Real Wallet & Smart Usage-Verified Escrow ("Pulse")
◦
Displays live wallet balances, monthly credit/debit cashflows, and total AI savings.
◦
Smart Escrow: Holds recurring subscription funds (e.g., gym memberships, cloud labs) in escrow and releases them only when app usage is verified via Android sensors, GPS geofences, or QR check-ins.
◦
Idle Auto-Pause: Automatically detects 5 days of zero usage, pauses idle subscriptions, and saves money directly into your balance.
◦
PayPal Sandbox Top-up: Instantly top up your wallet using live PayPal Sandbox API checkout flows.
2.
📶 Offline SMS / USSD Mesh Payments
◦
Allows users to make secure peer-to-peer and merchant transfers even without internet connectivity by tunneling encrypted payment payloads over SMS.
3.
🛡️ AI Scam Shield & Behavioral Fraud Interception
◦
Real-time AI fraud detection engine that evaluates transaction velocity and merchant risk scores.
◦
Automatically intercepts and blocks high-risk phishing transfers (e.g., fake bank KYC updates) and logs security audit trails.
4.
👥 Smart Friend Expense Splitter
◦
Easily split dinner bills or shared expenses among friends.
◦
Simulates friend reimbursements and automatically syncs credited splits to your wallet and Firestore database.
5.
🤖 Gemini AI Financial Analyst Chatbot
◦
An interactive AI chat assistant built directly into the app.
◦
Queries live wallet metrics (balance, cashflow, escrow savings, scam protection) to provide personalized, 2-sentence financial advice and spending insights.
